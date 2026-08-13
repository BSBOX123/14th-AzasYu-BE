package com.azasyu.domain.interview;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.azasyu.domain.auth.AuthService;
import com.azasyu.domain.auth.dto.SignUpRequest;
import com.azasyu.domain.meeting.MeetingRepository;
import com.azasyu.domain.meeting.MeetingService;
import com.azasyu.domain.meeting.dto.CreateMeetingRequest;
import com.azasyu.domain.project.ProjectService;
import com.azasyu.domain.project.dto.CreateProjectRequest;
import com.azasyu.domain.user.UserRepository;
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
class IdeaBoardServiceTest {

    @Autowired private IdeaBoardService ideaBoardService;
    @Autowired private IdeaCardRepository ideaCardRepository;
    @Autowired private InterviewSubmissionRepository submissionRepository;
    @Autowired private MeetingRepository meetingRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private MeetingService meetingService;
    @Autowired private ProjectService projectService;
    @Autowired private AuthService authService;

    @Test
    void participantSeesAnonymousCardsWithoutSubmittingInterview() {
        TestContext context = prepare("board-user@example.com");
        var submission = submissionRepository.save(new InterviewSubmission(
            meetingRepository.findById(context.meetingId()).orElseThrow(),
            userRepository.findById(context.userId()).orElseThrow()
        ));
        ideaCardRepository.save(new IdeaCard(
            submission, "핵심 기능 우선", "데모 완성도", "AI 지연", "재시도 제공"
        ));

        var cards = ideaBoardService.getCards(context.userId(), context.meetingId());

        assertThat(cards).hasSize(1);
        assertThat(cards.getFirst().coreOpinion()).isEqualTo("핵심 기능 우선");
    }

    @Test
    void rejectsSummaryRefreshWhenNoCardsExist() {
        TestContext context = prepare("empty-board-user@example.com");

        assertThatThrownBy(() -> ideaBoardService.refreshSummary(context.userId(), context.meetingId()))
            .isInstanceOf(ApiException.class)
            .hasMessage("요약할 아이디어 카드가 없습니다.");
    }

    private TestContext prepare(String email) {
        Long userId = authService.signUp(new SignUpRequest(email, "참여자", "password123")).userId();
        var project = projectService.create(userId, new CreateProjectRequest("보드 프로젝트", "익명 보드 테스트"));
        var meeting = meetingService.create(userId, project.id(), new CreateMeetingRequest(
            "의견 공유", "의견을 익명으로 검토", List.of("구현 범위"), LocalDate.now().plusDays(1),
            LocalTime.of(16, 0), 60, List.of(userId)
        ));
        return new TestContext(userId, meeting.id());
    }

    private record TestContext(Long userId, Long meetingId) {
    }
}
