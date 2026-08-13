package com.azasyu.domain.interview;

import java.util.Optional;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IdeaCardRepository extends JpaRepository<IdeaCard, Long> {

    Optional<IdeaCard> findBySubmissionId(Long submissionId);

    @EntityGraph(attributePaths = "submission")
    List<IdeaCard> findAllBySubmissionMeetingIdOrderByCreatedAtAsc(Long meetingId);
}
