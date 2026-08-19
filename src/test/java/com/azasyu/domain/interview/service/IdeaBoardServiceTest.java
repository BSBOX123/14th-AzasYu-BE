package com.azasyu.domain.interview.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.azasyu.domain.auth.AuthService;
import com.azasyu.domain.auth.dto.SignUpRequest;
import com.azasyu.domain.interview.entity.IdeaCard;
import com.azasyu.domain.interview.entity.IdeaSummary;
import com.azasyu.domain.interview.entity.InterviewSubmission;
import com.azasyu.domain.interview.dto.UpdateIdeaCardRequest;
import com.azasyu.domain.interview.repository.IdeaCardRepository;
import com.azasyu.domain.interview.repository.IdeaSummaryRepository;
import com.azasyu.domain.interview.repository.InterviewSubmissionRepository;
import com.azasyu.domain.meeting.dto.CreateMeetingRequest;
import com.azasyu.domain.meeting.repository.MeetingRepository;
import com.azasyu.domain.meeting.service.MeetingService;
import com.azasyu.domain.project.dto.CreateProjectRequest;
import com.azasyu.domain.project.dto.JoinProjectRequest;
import com.azasyu.domain.project.service.ProjectService;
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
    @Autowired private IdeaSummaryRepository ideaSummaryRepository;
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
        assertThat(cards.getFirst().isMine()).isTrue();
    }

    @Test
    void identifiesOnlyCurrentUsersCardWithoutExposingAuthor() {
        MultiUserContext context = prepareTwoUsers("board-owner@example.com", "board-member@example.com");
        saveCard(context.meetingId(), context.ownerId(), "내 의견");
        saveCard(context.meetingId(), context.memberId(), "다른 의견");

        var cards = ideaBoardService.getCards(context.ownerId(), context.meetingId());

        assertThat(cards).hasSize(2);
        assertThat(cards).filteredOn(card -> card.isMine()).singleElement()
            .extracting(card -> card.coreOpinion()).isEqualTo("내 의견");
    }

    @Test
    void ownerCanUpdateCardAndSummaryBecomesOutdated() {
        TestContext context = prepare("update-card-user@example.com");
        IdeaCard card = saveCard(context.meetingId(), context.userId(), "기존 의견");
        ideaSummaryRepository.saveAndFlush(new IdeaSummary(
            meetingRepository.findById(context.meetingId()).orElseThrow(),
            userRepository.findById(context.userId()).orElseThrow(),
            1, 1, "공통", "차이", "우려", "논의"
        ));

        var updated = ideaBoardService.updateCard(
            context.userId(), context.meetingId(), card.getId(),
            new UpdateIdeaCardRequest("수정 의견", "수정 이유", "수정 우려", "수정 대안")
        );
        ideaCardRepository.flush();

        assertThat(updated.coreOpinion()).isEqualTo("수정 의견");
        assertThat(updated.isMine()).isTrue();
        assertThat(ideaBoardService.getLatestSummary(context.userId(), context.meetingId()).isOutdated())
            .isTrue();
    }

    @Test
    void userCannotUpdateAnotherUsersCard() {
        MultiUserContext context = prepareTwoUsers("edit-owner@example.com", "edit-member@example.com");
        IdeaCard card = saveCard(context.meetingId(), context.ownerId(), "소유자 의견");

        assertThatThrownBy(() -> ideaBoardService.updateCard(
            context.memberId(), context.meetingId(), card.getId(),
            new UpdateIdeaCardRequest("변경", "이유", "우려", "대안")
        ))
            .isInstanceOf(ApiException.class)
            .hasMessage("본인의 아이디어 카드만 변경할 수 있습니다.");
    }

    @Test
    void deletingCardHidesItButKeepsInterviewSubmission() {
        TestContext context = prepare("delete-card-user@example.com");
        IdeaCard card = saveCard(context.meetingId(), context.userId(), "삭제할 의견");
        Long submissionId = card.getSubmission().getId();

        ideaBoardService.deleteCard(context.userId(), context.meetingId(), card.getId());
        ideaCardRepository.flush();

        assertThat(ideaBoardService.getCards(context.userId(), context.meetingId())).isEmpty();
        assertThat(submissionRepository.findById(submissionId)).isPresent();
        assertThat(ideaCardRepository.findById(card.getId())).get().extracting(IdeaCard::isVisible)
            .isEqualTo(false);
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

    private MultiUserContext prepareTwoUsers(String ownerEmail, String memberEmail) {
        Long ownerId = authService.signUp(new SignUpRequest(ownerEmail, "소유자", "password123")).userId();
        Long memberId = authService.signUp(new SignUpRequest(memberEmail, "참여자", "password123")).userId();
        var project = projectService.create(ownerId, new CreateProjectRequest("보드 프로젝트", "카드 관리 테스트"));
        projectService.join(memberId, new JoinProjectRequest(project.joinCode()));
        var meeting = meetingService.create(ownerId, project.id(), new CreateMeetingRequest(
            "의견 공유", "카드 관리 기능 검토", List.of("구현 범위"), LocalDate.now().plusDays(1),
            LocalTime.of(16, 0), 60, List.of(ownerId, memberId)
        ));
        return new MultiUserContext(ownerId, memberId, meeting.id());
    }

    private IdeaCard saveCard(Long meetingId, Long userId, String coreOpinion) {
        var submission = submissionRepository.save(new InterviewSubmission(
            meetingRepository.findById(meetingId).orElseThrow(),
            userRepository.findById(userId).orElseThrow()
        ));
        return ideaCardRepository.saveAndFlush(new IdeaCard(
            submission, coreOpinion, "이유", "우려", "대안"
        ));
    }

    private record TestContext(Long userId, Long meetingId) {
    }

    private record MultiUserContext(Long ownerId, Long memberId, Long meetingId) {
    }
}
