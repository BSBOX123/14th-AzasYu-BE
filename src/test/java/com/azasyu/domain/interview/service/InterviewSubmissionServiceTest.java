package com.azasyu.domain.interview.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.azasyu.domain.auth.AuthService;
import com.azasyu.domain.auth.dto.SignUpRequest;
import com.azasyu.domain.interview.dto.SubmitInterviewRequest;
import com.azasyu.domain.interview.entity.InterviewQuestion;
import com.azasyu.domain.interview.entity.InterviewQuestionSet;
import com.azasyu.domain.interview.repository.InterviewQuestionRepository;
import com.azasyu.domain.interview.repository.InterviewQuestionSetRepository;
import com.azasyu.domain.meeting.dto.CreateMeetingRequest;
import com.azasyu.domain.meeting.service.MeetingService;
import com.azasyu.domain.project.dto.CreateProjectRequest;
import com.azasyu.domain.project.dto.JoinProjectRequest;
import com.azasyu.domain.project.service.ProjectService;
import com.azasyu.global.error.ApiException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class InterviewSubmissionServiceTest {

    @Autowired private InterviewSubmissionService submissionService;
    @Autowired private InterviewQuestionSetRepository questionSetRepository;
    @Autowired private InterviewQuestionRepository questionRepository;
    @Autowired private MeetingService meetingService;
    @Autowired private ProjectService projectService;
    @Autowired private AuthService authService;

    @Test
    void participantSubmitsAllAnswers() {
        TestContext context = prepareInterview("submission-user@example.com");
        var request = new SubmitInterviewRequest(List.of(
            new SubmitInterviewRequest.AnswerRequest(context.question1().getId(), "핵심 기능을 먼저 완성해야 합니다."),
            new SubmitInterviewRequest.AnswerRequest(context.question2().getId(), "AI 응답 지연이 우려됩니다.")
        ));

        var response = submissionService.submit(context.userId(), context.meetingId(), request);

        assertThat(response.cardGenerationStatus()).isEqualTo("NOT_CONFIGURED");
        assertThat(response.ideaCard()).isNull();
        assertThat(submissionService.getMine(context.userId(), context.meetingId()).submissionId())
            .isEqualTo(response.submissionId());
    }

    @Test
    void rejectsIncompleteAnswers() {
        TestContext context = prepareInterview("incomplete-user@example.com");
        var request = new SubmitInterviewRequest(List.of(
            new SubmitInterviewRequest.AnswerRequest(context.question1().getId(), "첫 번째 답변")
        ));

        assertThatThrownBy(() -> submissionService.submit(context.userId(), context.meetingId(), request))
            .isInstanceOf(ApiException.class)
            .hasMessage("모든 인터뷰 질문에 한 번씩 답변해야 합니다.");
    }

    @Test
    void meetingDetailShowsInterviewProgress() {
        Long ownerId = authService.signUp(new SignUpRequest("status-owner@example.com", "생성자", "password123")).userId();
        Long memberId = authService.signUp(new SignUpRequest("status-member@example.com", "참여자", "password123")).userId();
        var project = projectService.create(ownerId, new CreateProjectRequest("현황 확인", "진행률"));
        projectService.join(memberId, new JoinProjectRequest(project.joinCode()));
        var meeting = meetingService.create(ownerId, project.id(), new CreateMeetingRequest(
            "현황 회의", "진행률 확인", List.of("안건"), LocalDate.now().plusDays(1),
            LocalTime.of(15, 0), 60, List.of(ownerId, memberId)
        ));
        InterviewQuestionSet set = questionSetRepository.findByMeetingId(meeting.id()).orElseThrow();
        set.generated();
        InterviewQuestion question = questionRepository.save(new InterviewQuestion(set, 1, "질문"));

        var before = meetingService.getDetail(ownerId, meeting.id()).interviewStatus();
        assertThat(before.totalParticipants()).isEqualTo(2);
        assertThat(before.submittedCount()).isZero();
        assertThat(before.mySubmitted()).isFalse();

        submissionService.submit(ownerId, meeting.id(), new SubmitInterviewRequest(
            List.of(new SubmitInterviewRequest.AnswerRequest(question.getId(), "답변입니다."))
        ));

        var afterOwner = meetingService.getDetail(ownerId, meeting.id()).interviewStatus();
        assertThat(afterOwner.submittedCount()).isEqualTo(1);
        assertThat(afterOwner.mySubmitted()).as("제출한 본인은 true").isTrue();

        var afterMember = meetingService.getDetail(memberId, meeting.id()).interviewStatus();
        assertThat(afterMember.submittedCount()).as("제출 인원 수는 모두에게 같다").isEqualTo(1);
        assertThat(afterMember.mySubmitted()).as("제출하지 않은 사람은 false").isFalse();
    }

    private TestContext prepareInterview(String email) {
        Long userId = authService.signUp(new SignUpRequest(email, "참여자", "password123")).userId();
        var project = projectService.create(userId, new CreateProjectRequest("인터뷰 프로젝트", "답변 제출 테스트"));
        var meeting = meetingService.create(userId, project.id(), new CreateMeetingRequest(
            "기능 결정", "MVP 범위 결정", List.of("기능 범위"), LocalDate.now().plusDays(1),
            LocalTime.of(15, 0), 60, List.of(userId)
        ));
        InterviewQuestionSet set = questionSetRepository.findByMeetingId(meeting.id()).orElseThrow();
        set.generated();
        InterviewQuestion question1 = questionRepository.save(new InterviewQuestion(set, 1, "가장 중요한 기능은 무엇인가요?"));
        InterviewQuestion question2 = questionRepository.save(new InterviewQuestion(set, 2, "우려되는 점은 무엇인가요?"));
        return new TestContext(userId, meeting.id(), question1, question2);
    }

    private record TestContext(
        Long userId,
        Long meetingId,
        InterviewQuestion question1,
        InterviewQuestion question2
    ) {
    }
}
