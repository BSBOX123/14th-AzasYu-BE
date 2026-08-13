package com.azasyu.domain.interview;

import com.azasyu.domain.interview.ai.IdeaSummaryAiClient;
import com.azasyu.domain.interview.ai.IdeaSummaryDraft;
import com.azasyu.domain.interview.dto.AnonymousIdeaCardResponse;
import com.azasyu.domain.interview.dto.IdeaSummaryResponse;
import com.azasyu.domain.meeting.Meeting;
import com.azasyu.domain.meeting.MeetingParticipantRepository;
import com.azasyu.domain.meeting.MeetingRepository;
import com.azasyu.domain.user.User;
import com.azasyu.domain.user.UserRepository;
import com.azasyu.global.error.ApiException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class IdeaBoardService {

    private final IdeaCardRepository ideaCardRepository;
    private final IdeaSummaryRepository ideaSummaryRepository;
    private final MeetingRepository meetingRepository;
    private final MeetingParticipantRepository participantRepository;
    private final UserRepository userRepository;
    private final IdeaSummaryAiClient summaryAiClient;

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

    @Transactional
    public IdeaSummaryResponse refreshSummary(Long userId, Long meetingId) {
        requireParticipant(meetingId, userId);
        List<IdeaCard> cards = ideaCardRepository.findAllBySubmissionMeetingIdOrderByCreatedAtAsc(meetingId);
        if (cards.isEmpty()) {
            throw new ApiException(HttpStatus.CONFLICT, "NO_IDEA_CARDS", "요약할 아이디어 카드가 없습니다.");
        }
        if (!summaryAiClient.isConfigured()) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "GEMINI_NOT_CONFIGURED", "Gemini API 키가 설정되지 않았습니다.");
        }
        Meeting meeting = meetingRepository.findWithDetailsById(meetingId)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "MEETING_NOT_FOUND", "회의를 찾을 수 없습니다."));
        User user = userRepository.findById(userId).orElseThrow();
        int version = ideaSummaryRepository.findFirstByMeetingIdOrderByVersionDesc(meetingId)
            .map(summary -> summary.getVersion() + 1).orElse(1);
        IdeaSummaryDraft draft;
        try {
            draft = summaryAiClient.generate(meeting, cards);
        } catch (RuntimeException exception) {
            throw new ApiException(HttpStatus.BAD_GATEWAY, "IDEA_SUMMARY_FAILED", "전체 의견 요약 생성에 실패했습니다.");
        }
        return toResponse(ideaSummaryRepository.save(new IdeaSummary(
            meeting, user, version, cards.size(), draft.commonOpinions(), draft.differingOpinions(),
            draft.keyConcerns(), draft.discussionPoints()
        )));
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
}
