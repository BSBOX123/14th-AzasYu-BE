package com.azasyu.domain.interview.repository;

import com.azasyu.domain.interview.entity.InterviewSubmission;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InterviewSubmissionRepository extends JpaRepository<InterviewSubmission, Long> {

    boolean existsByMeetingIdAndUserId(Long meetingId, Long userId);

    long countByMeetingId(Long meetingId);

    @EntityGraph(attributePaths = {"meeting", "user"})
    Optional<InterviewSubmission> findByMeetingIdAndUserId(Long meetingId, Long userId);
}
