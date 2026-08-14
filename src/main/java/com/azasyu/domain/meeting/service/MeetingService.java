package com.azasyu.domain.meeting.service;

import com.azasyu.domain.interview.service.InterviewQuestionService;
import com.azasyu.domain.meeting.dto.CreateMeetingRequest;
import com.azasyu.domain.meeting.dto.JoinMeetingRequest;
import com.azasyu.domain.meeting.dto.MeetingDetailResponse;
import com.azasyu.domain.meeting.dto.MeetingSummaryResponse;
import com.azasyu.domain.meeting.entity.Meeting;
import com.azasyu.domain.meeting.entity.MeetingAgenda;
import com.azasyu.domain.meeting.entity.MeetingParticipant;
import com.azasyu.domain.meeting.repository.MeetingAgendaRepository;
import com.azasyu.domain.meeting.repository.MeetingParticipantRepository;
import com.azasyu.domain.meeting.repository.MeetingRepository;
import com.azasyu.domain.project.entity.Project;
import com.azasyu.domain.project.entity.ProjectMember;
import com.azasyu.domain.project.repository.ProjectMemberRepository;
import com.azasyu.domain.user.User;
import com.azasyu.global.error.ApiException;
import com.azasyu.global.support.JoinCodeGenerator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * 회의 생성·조회와 참여 코드 합류.
 *
 * <p>안건은 회의 생성 시에만 등록하며 별도 관리 API가 없음. 참여자는 생성 시 지정하거나
 * 나중에 참여 코드로 합류함. 생성자는 지정 여부와 무관하게 항상 참여자로 등록됨.
 *
 * <p>회의 상세는 참여자가 아니어도 프로젝트 구성원이면 조회할 수 있음.
 * 반면 인터뷰·원문·분석 기능은 회의 참여자만 쓸 수 있음.
 *
 * <p>{@link #create}는 AI 호출이 이어지므로 {@code @Transactional}을 걸지 않고
 * {@code TransactionTemplate}으로 짧은 트랜잭션을 나눠 엶. 트랜잭션 안에서 AI를 호출하면
 * 응답이 늦어질 때 DB 커넥션이 그만큼 오래 점유되기 때문임.
 */
@Service
@RequiredArgsConstructor
public class MeetingService {

    private final MeetingRepository meetingRepository;
    private final MeetingAgendaRepository meetingAgendaRepository;
    private final MeetingParticipantRepository meetingParticipantRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final InterviewQuestionService interviewQuestionService;
    private final JoinCodeGenerator joinCodeGenerator;
    private final TransactionTemplate transactionTemplate;

    private static final int JOIN_CODE_GENERATION_ATTEMPTS = 10;

    /**
     * 회의를 생성하고 공통 질문 생성을 시작함.
     *
     * <p>메서드에 {@code @Transactional}을 걸지 않음. 회의 저장을 먼저 커밋해야
     * 이어지는 AI 호출이 트랜잭션 밖에서 이루어짐.
     */
    public MeetingDetailResponse create(Long userId, Long projectId, CreateMeetingRequest request) {
        Long meetingId = transactionTemplate.execute(status -> createMeeting(userId, projectId, request));
        interviewQuestionService.initializeAndGenerate(meetingId);
        return transactionTemplate.execute(status -> buildDetail(userId, meetingId));
    }

    private Long createMeeting(Long userId, Long projectId, CreateMeetingRequest request) {
        ProjectMember creatorMember = getProjectMember(projectId, userId);
        Map<Long, ProjectMember> projectMembers = projectMemberRepository
            .findAllByProjectIdOrderByJoinedAtAsc(projectId).stream()
            .collect(Collectors.toMap(member -> member.getUser().getId(), Function.identity()));

        // 생성자는 지정 여부와 무관하게 항상 참여자로 등록한다. 참여자가 아니면 자기가 만든 회의의
        // 인터뷰·원문·분석 기능을 쓸 수 없다. Set이라 명시적으로 넣어 보내도 중복되지 않는다.
        LinkedHashSet<Long> participantIds = new LinkedHashSet<>();
        participantIds.add(userId);
        if (request.participantUserIds() != null) {
            participantIds.addAll(request.participantUserIds());
        }

        List<Long> invalidParticipantIds = participantIds.stream()
            .filter(participantId -> !projectMembers.containsKey(participantId))
            .toList();
        if (!invalidParticipantIds.isEmpty()) {
            throw new ApiException(
                HttpStatus.BAD_REQUEST,
                "INVALID_MEETING_PARTICIPANT",
                "프로젝트 구성원이 아닌 사용자는 회의 참여자로 선택할 수 없습니다."
            );
        }

        Project project = creatorMember.getProject();
        User creator = creatorMember.getUser();
        Meeting meeting = meetingRepository.save(new Meeting(
            project, creator, request.title().trim(), request.purpose().trim(),
            request.meetingDate(), request.startTime(), request.expectedDurationMinutes(),
            createUniqueJoinCode()
        ));

        for (int index = 0; index < request.agendas().size(); index++) {
            meetingAgendaRepository.save(new MeetingAgenda(meeting, index + 1, request.agendas().get(index).trim()));
        }
        participantIds.forEach(participantId -> meetingParticipantRepository.save(
            new MeetingParticipant(meeting, projectMembers.get(participantId).getUser())
        ));

        return meeting.getId();
    }

    /**
     * 회의 참여 코드로 합류함.
     *
     * <p>회의는 프로젝트 안에 있으므로 **프로젝트 구성원만** 합류할 수 있음.
     * 구성원이 아니면 프로젝트 참여 코드로 먼저 팀에 들어와야 함.
     */
    @Transactional
    public MeetingDetailResponse joinByCode(Long userId, JoinMeetingRequest request) {
        String joinCode = request.joinCode().trim().toUpperCase(Locale.ROOT);
        Meeting meeting = meetingRepository.findByJoinCode(joinCode)
            .orElseThrow(() -> new ApiException(
                HttpStatus.NOT_FOUND, "MEETING_JOIN_CODE_NOT_FOUND", "유효하지 않은 회의 참여 코드입니다."));

        Long projectId = meeting.getProject().getId();
        ProjectMember member = projectMemberRepository.findByProjectIdAndUserId(projectId, userId)
            .orElseThrow(() -> new ApiException(
                HttpStatus.FORBIDDEN, "PROJECT_MEMBER_REQUIRED",
                "프로젝트에 먼저 참여해야 회의에 합류할 수 있습니다."));

        if (meetingParticipantRepository.existsByMeetingIdAndUserId(meeting.getId(), userId)) {
            throw new ApiException(
                HttpStatus.CONFLICT, "ALREADY_MEETING_PARTICIPANT", "이미 참여 중인 회의입니다.");
        }

        meetingParticipantRepository.save(new MeetingParticipant(meeting, member.getUser()));
        return buildDetail(userId, meeting.getId());
    }

    @Transactional(readOnly = true)
    public List<MeetingSummaryResponse> getProjectMeetings(Long userId, Long projectId) {
        getProjectMember(projectId, userId);
        return meetingRepository.findAllByProjectIdOrderByMeetingDateDescStartTimeDesc(projectId).stream()
            .map(meeting -> new MeetingSummaryResponse(
                meeting.getId(), meeting.getTitle(), meeting.getPurpose(), meeting.getMeetingDate(),
                meeting.getStartTime(), meeting.getExpectedDurationMinutes(),
                meetingParticipantRepository.findAllByMeetingIdOrderByIdAsc(meeting.getId()).size()
            ))
            .toList();
    }

    @Transactional(readOnly = true)
    public MeetingDetailResponse getDetail(Long userId, Long meetingId) {
        return buildDetail(userId, meetingId);
    }

    private MeetingDetailResponse buildDetail(Long userId, Long meetingId) {
        Meeting meeting = meetingRepository.findWithDetailsById(meetingId)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "MEETING_NOT_FOUND", "회의를 찾을 수 없습니다."));
        getProjectMember(meeting.getProject().getId(), userId);

        List<MeetingDetailResponse.AgendaResponse> agendas = meetingAgendaRepository
            .findAllByMeetingIdOrderByAgendaOrderAsc(meetingId).stream()
            .map(agenda -> new MeetingDetailResponse.AgendaResponse(
                agenda.getId(), agenda.getAgendaOrder(), agenda.getContent()
            ))
            .toList();
        List<MeetingDetailResponse.ParticipantResponse> participants = meetingParticipantRepository
            .findAllByMeetingIdOrderByIdAsc(meetingId).stream()
            .map(participant -> new MeetingDetailResponse.ParticipantResponse(
                participant.getUser().getId(), participant.getUser().getName()
            ))
            .toList();

        return new MeetingDetailResponse(
            meeting.getId(), meeting.getProject().getId(), meeting.getTitle(), meeting.getPurpose(),
            meeting.getJoinCode(), agendas,
            meeting.getMeetingDate(), meeting.getStartTime(), meeting.getExpectedDurationMinutes(),
            new MeetingDetailResponse.CreatorResponse(meeting.getCreatedBy().getId(), meeting.getCreatedBy().getName()),
            participants, meeting.getCreatedAt()
        );
    }

    /**
     * 중복되지 않는 회의 참여 코드를 만듦.
     *
     * <p>충돌 시 최대 {@value #JOIN_CODE_GENERATION_ATTEMPTS}회까지 재시도하고,
     * 그래도 실패하면 예외를 던짐.
     */
    private String createUniqueJoinCode() {
        for (int attempt = 0; attempt < JOIN_CODE_GENERATION_ATTEMPTS; attempt++) {
            String code = joinCodeGenerator.generate();
            if (!meetingRepository.existsByJoinCode(code)) {
                return code;
            }
        }
        throw new ApiException(
            HttpStatus.INTERNAL_SERVER_ERROR, "JOIN_CODE_GENERATION_FAILED", "참여 코드 생성에 실패했습니다.");
    }

    private ProjectMember getProjectMember(Long projectId, Long userId) {
        return projectMemberRepository.findByProjectIdAndUserId(projectId, userId)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "PROJECT_NOT_FOUND", "프로젝트를 찾을 수 없습니다."));
    }
}
