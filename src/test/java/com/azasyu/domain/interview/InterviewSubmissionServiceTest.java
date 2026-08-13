package com.azasyu.domain.interview;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.azasyu.domain.auth.AuthService;
import com.azasyu.domain.auth.dto.SignUpRequest;
import com.azasyu.domain.interview.dto.SubmitInterviewRequest;
import com.azasyu.domain.meeting.MeetingService;
import com.azasyu.domain.meeting.dto.CreateMeetingRequest;
import com.azasyu.domain.project.ProjectService;
import com.azasyu.domain.project.dto.CreateProjectRequest;
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
