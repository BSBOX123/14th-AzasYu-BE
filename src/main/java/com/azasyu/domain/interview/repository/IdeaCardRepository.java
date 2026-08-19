package com.azasyu.domain.interview.repository;

import com.azasyu.domain.interview.entity.IdeaCard;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IdeaCardRepository extends JpaRepository<IdeaCard, Long> {

    Optional<IdeaCard> findBySubmissionId(Long submissionId);

    @EntityGraph(attributePaths = {"submission", "submission.user"})
    List<IdeaCard> findAllBySubmissionMeetingIdAndVisibleTrueOrderByCreatedAtAsc(Long meetingId);

    @EntityGraph(attributePaths = {"submission", "submission.user"})
    Optional<IdeaCard> findByIdAndSubmissionMeetingId(Long cardId, Long meetingId);

    long countBySubmissionMeetingIdAndVisibleTrue(Long meetingId);

    boolean existsBySubmissionMeetingIdAndVisibleTrueAndUpdatedAtAfter(
        Long meetingId, java.time.LocalDateTime refreshedAt
    );
}
