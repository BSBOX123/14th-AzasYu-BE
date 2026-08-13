package com.azasyu.domain.meeting;

import com.azasyu.domain.interview.InterviewQuestionService;
import com.azasyu.domain.meeting.dto.CreateMeetingRequest;
import com.azasyu.domain.meeting.dto.MeetingDetailResponse;
import com.azasyu.domain.meeting.dto.MeetingSummaryResponse;
import com.azasyu.domain.project.Project;
import com.azasyu.domain.project.ProjectMember;
import com.azasyu.domain.project.ProjectMemberRepository;
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
     * 회의를 생성하고 공통 질문 생성을 시작한다.
     *
     * <p>메서드에 {@code @Transactional}을 걸지 않는다. 회의 저장을 먼저 커밋한 뒤
     * 질문 생성을 시작해야 AI 호출이 트랜잭션 밖에서 이루어진다.
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
