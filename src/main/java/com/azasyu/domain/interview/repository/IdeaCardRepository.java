package com.azasyu.domain.interview.repository;

import com.azasyu.domain.interview.entity.IdeaCard;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IdeaCardRepository extends JpaRepository<IdeaCard, Long> {

    Optional<IdeaCard> findBySubmissionId(Long submissionId);

    @EntityGraph(attributePaths = "submission")
    List<IdeaCard> findAllBySubmissionMeetingIdOrderByCreatedAtAsc(Long meetingId);
}
