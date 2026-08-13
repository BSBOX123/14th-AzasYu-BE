package com.azasyu.domain.interview;

import com.azasyu.domain.interview.ai.IdeaCardContext;
import com.azasyu.domain.interview.ai.IdeaSummaryAiClient;
import com.azasyu.domain.interview.ai.IdeaSummaryDraft;
import com.azasyu.domain.interview.dto.AnonymousIdeaCardResponse;
import com.azasyu.domain.interview.dto.IdeaSummaryResponse;
import com.azasyu.domain.meeting.Meeting;
import com.azasyu.domain.meeting.MeetingParticipantRepository;
import com.azasyu.domain.meeting.MeetingRepository;
import com.azasyu.domain.meeting.ai.MeetingContext;
import com.azasyu.domain.user.User;
import com.azasyu.domain.user.UserRepository;
import com.azasyu.global.error.ApiException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

@Service
@RequiredArgsConstructor
public class IdeaBoardService {

    private static final Logger log = LoggerFactory.getLogger(IdeaBoardService.class);

    private final IdeaCardRepository ideaCardRepository;
    private final IdeaSummaryRepository ideaSummaryRepository;
    private final MeetingRepository meetingRepository;
    private final MeetingParticipantRepository participantRepository;
    private final UserRepository userRepository;
    private final IdeaSummaryAiClient summaryAiClient;
    private final TransactionTemplate transactionTemplate;

    @Transactional(readOnly = true)
    public List<AnonymousIdeaCardResponse> getCards(Long userId, Long meetingId) {
        requireParticipant(meetingId, userId);
        return ideaCardRepository.findAllBySubmissionMeetingIdOrderByCreatedAtAsc(meetingId).stream()
            .map(card -> new AnonymousIdeaCardResponse(
                card.getId(), card.getCoreOpinion(), card.getRationale(), card.getConcern(),
                card.getAlternative(), card.getCreatedAt()
            ))
            .toList();
    }

    @Transactional(readOnly = true)
    public IdeaSummaryResponse getLatestSummary(Long userId, Long meetingId) {
        requireParticipant(meetingId, userId);
        return ideaSummaryRepository.findFirstByMeetingIdOrderByVersionDesc(meetingId)
            .map(this::toResponse)
            .orElse(null);
    }

    /**
     * 익명 카드 전체를 다시 요약한다.
     *
     * <p>메서드에 {@code @Transactional}을 걸지 않는다. AI 호출이 트랜잭션 안에서
     * 일어나면 응답이 늦어질 때 DB 커넥션이 그만큼 오래 점유되기 때문이다.
     * 검증과 저장만 각각 짧은 트랜잭션으로 처리한다.
     */
    public IdeaSummaryResponse refreshSummary(Long userId, Long meetingId) {
        PendingSummary pending = transactionTemplate.execute(status -> {
            requireParticipant(meetingId, userId);
            List<IdeaCard> cards = ideaCardRepository.findAllBySubmissionMeetingIdOrderByCreatedAtAsc(meetingId);
            if (cards.isEmpty()) {
                throw new ApiException(HttpStatus.CONFLICT, "NO_IDEA_CARDS", "요약할 아이디어 카드가 없습니다.");
            }
            if (!summaryAiClient.isConfigured()) {
                throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "GEMINI_NOT_CONFIGURED", "Gemini API 키가 설정되지 않았습니다.");
            }
            Meeting meeting = getMeeting(meetingId);
            return new PendingSummary(
                MeetingContext.from(meeting), cards.stream().map(IdeaCardContext::from).toList()
            );
        });

        IdeaSummaryDraft draft;
        try {
            // 트랜잭션 밖에서 호출한다. 응답이 지연돼도 DB 커넥션을 잡지 않는다.
            draft = summaryAiClient.generate(pending.meeting(), pending.cards());
        } catch (RuntimeException exception) {
            log.warn("Idea summary generation failed: meetingId={}", meetingId, exception);
            throw new ApiException(HttpStatus.BAD_GATEWAY, "IDEA_SUMMARY_FAILED", "전체 의견 요약 생성에 실패했습니다.");
        }

        return transactionTemplate.execute(status -> {
            Meeting meeting = getMeeting(meetingId);
            User user = userRepository.findById(userId).orElseThrow();
            // 버전은 저장 시점에 다시 계산한다. AI 호출 사이에 다른 요청이 저장했을 수 있다.
            int version = ideaSummaryRepository.findFirstByMeetingIdOrderByVersionDesc(meetingId)
                .map(summary -> summary.getVersion() + 1).orElse(1);
            return toResponse(ideaSummaryRepository.save(new IdeaSummary(
                meeting, user, version, pending.cards().size(), draft.commonOpinions(), draft.differingOpinions(),
                draft.keyConcerns(), draft.discussionPoints()
            )));
        });
    }

    private Meeting getMeeting(Long meetingId) {
        return meetingRepository.findWithDetailsById(meetingId)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "MEETING_NOT_FOUND", "회의를 찾을 수 없습니다."));
    }

    private IdeaSummaryResponse toResponse(IdeaSummary summary) {
        return new IdeaSummaryResponse(
            summary.getId(), summary.getMeeting().getId(), summary.getVersion(), summary.getSourceCardCount(),
            summary.getCommonOpinions(), summary.getDifferingOpinions(), summary.getKeyConcerns(),
            summary.getDiscussionPoints(), summary.getCreatedAt()
        );
    }

    private void requireParticipant(Long meetingId, Long userId) {
        if (!participantRepository.existsByMeetingIdAndUserId(meetingId, userId)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "IDEA_BOARD_NOT_FOUND", "아이디어 보드를 찾을 수 없습니다.");
        }
    }

    /** 트랜잭션 밖으로 넘기는 AI 호출 입력. 엔티티가 아니라 값만 담는다. */
    private record PendingSummary(MeetingContext meeting, List<IdeaCardContext> cards) {
    }
}
