package com.azasyu.domain.meeting;

import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MeetingParticipantRepository extends JpaRepository<MeetingParticipant, Long> {

    boolean existsByMeetingIdAndUserId(Long meetingId, Long userId);

    @EntityGraph(attributePaths = "user")
    List<MeetingParticipant> findAllByMeetingIdOrderByIdAsc(Long meetingId);
}
