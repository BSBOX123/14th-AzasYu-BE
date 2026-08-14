package com.azasyu.domain.interview.service;

import com.azasyu.domain.interview.ai.IdeaCardContext;
import com.azasyu.domain.interview.ai.IdeaSummaryAiClient;
import com.azasyu.domain.interview.ai.IdeaSummaryDraft;
import com.azasyu.domain.interview.dto.AnonymousIdeaCardResponse;
import com.azasyu.domain.interview.dto.IdeaSummaryResponse;
import com.azasyu.domain.interview.entity.IdeaCard;
import com.azasyu.domain.interview.entity.IdeaSummary;
import com.azasyu.domain.interview.repository.IdeaCardRepository;
import com.azasyu.domain.interview.repository.IdeaSummaryRepository;
import com.azasyu.domain.meeting.ai.MeetingContext;
import com.azasyu.domain.meeting.entity.Meeting;
import com.azasyu.domain.meeting.repository.MeetingParticipantRepository;
import com.azasyu.domain.meeting.repository.MeetingRepository;
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

/**
 * 익명 아이디어 카드 조회와 전체 의견 요약.
 *
 * <p>카드 응답에는 작성자를 식별할 수 있는 값을 담지 않음.
 * 요약은 자동 생성되지 않고 새로고침 요청이 있을 때만 새 버전으로 저장함. *
 * <p>AI 호출은 트랜잭션 밖에서 수행함. 트랜잭션 안에서 호출하면 응답이 늦어질 때
 * DB 커넥션이 그만큼 오래 점유됨. 상태 저장은 {@code TransactionTemplate}으로
 * 짧은 트랜잭션을 열어 처리하므로 서비스 메서드에 {@code @Transactional}을 걸지 않음.
 */
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
     * 익명 카드 전체를 다시 요약해 새 버전으로 저장함.
     *
     * <p>다른 AI 기능과 달리 실패를 상태로 남기지 않고 예외로 반환함.
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
            // 트랜잭션 밖에서 호출함. 응답이 지연돼도 DB 커넥션을 잡지 않음.
            draft = summaryAiClient.generate(pending.meeting(), pending.cards());
        } catch (RuntimeException exception) {
            log.warn("Idea summary generation failed: meetingId={}", meetingId, exception);
            throw new ApiException(HttpStatus.BAD_GATEWAY, "IDEA_SUMMARY_FAILED", "전체 의견 요약 생성에 실패했습니다.");
        }

        return transactionTemplate.execute(status -> {
            Meeting meeting = getMeeting(meetingId);
            User user = userRepository.findById(userId).orElseThrow();
            // 버전은 저장 시점에 다시 계산함. AI 호출 사이에 다른 요청이 저장했을 수 있음.
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

    /** 트랜잭션 밖으로 넘기는 AI 호출 입력. 엔티티가 아니라 값만 담음. */
    private record PendingSummary(MeetingContext meeting, List<IdeaCardContext> cards) {
    }
}
