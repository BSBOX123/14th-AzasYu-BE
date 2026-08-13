package com.azasyu.domain.interview;

import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InterviewQuestionSetRepository extends JpaRepository<InterviewQuestionSet, Long> {

    @EntityGraph(attributePaths = "meeting")
    Optional<InterviewQuestionSet> findByMeetingId(Long meetingId);
}
