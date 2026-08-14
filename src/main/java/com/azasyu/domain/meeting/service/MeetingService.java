package com.azasyu.domain.meeting.service;

import com.azasyu.domain.interview.service.InterviewQuestionService;
import com.azasyu.domain.meeting.dto.CreateMeetingRequest;
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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * 회의 생성과 조회.
 *
 * <p>안건과 참여자는 회의 생성 시 함께 등록하며 별도 관리 API가 없음.
 * 회의 상세는 참여자가 아니어도 프로젝트 구성원이면 조회할 수 있음. *
 * <p>AI 호출은 트랜잭션 밖에서 수행함. 트랜잭션 안에서 호출하면 응답이 늦어질 때
 * DB 커넥션이 그만큼 오래 점유됨. 상태 저장은 {@code TransactionTemplate}으로
 * 짧은 트랜잭션을 열어 처리하므로 서비스 메서드에 {@code @Transactional}을 걸지 않음.
 */
@Service
@RequiredArgsConstructor
public class MeetingService {

    private final MeetingRepository meetingRepository;
    private final MeetingAgendaRepository meetingAgendaRepository;
    private final MeetingParticipantRepository meetingParticipantRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final InterviewQuestionService interviewQuestionService;
    private final TransactionTemplate transactionTemplate;

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

        LinkedHashSet<Long> participantIds = new LinkedHashSet<>(request.participantUserIds());
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
            request.meetingDate(), request.startTime(), request.expectedDurationMinutes()
        ));

        for (int index = 0; index < request.agendas().size(); index++) {
            meetingAgendaRepository.save(new MeetingAgenda(meeting, index + 1, request.agendas().get(index).trim()));
        }
        participantIds.forEach(participantId -> meetingParticipantRepository.save(
            new MeetingParticipant(meeting, projectMembers.get(participantId).getUser())
        ));

        return meeting.getId();
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
            meeting.getId(), meeting.getProject().getId(), meeting.getTitle(), meeting.getPurpose(), agendas,
            meeting.getMeetingDate(), meeting.getStartTime(), meeting.getExpectedDurationMinutes(),
            new MeetingDetailResponse.CreatorResponse(meeting.getCreatedBy().getId(), meeting.getCreatedBy().getName()),
            participants, meeting.getCreatedAt()
        );
    }

    private ProjectMember getProjectMember(Long projectId, Long userId) {
        return projectMemberRepository.findByProjectIdAndUserId(projectId, userId)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "PROJECT_NOT_FOUND", "프로젝트를 찾을 수 없습니다."));
    }
}
