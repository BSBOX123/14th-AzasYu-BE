package com.azasyu.domain.meeting;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MeetingRepository extends JpaRepository<Meeting, Long> {

    @EntityGraph(attributePaths = "createdBy")
    List<Meeting> findAllByProjectIdOrderByMeetingDateDescStartTimeDesc(Long projectId);

    @EntityGraph(attributePaths = {"project", "createdBy"})
    Optional<Meeting> findWithDetailsById(Long meetingId);
}
