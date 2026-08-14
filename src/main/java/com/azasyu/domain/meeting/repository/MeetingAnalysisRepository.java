package com.azasyu.domain.meeting.repository;

import com.azasyu.domain.meeting.entity.MeetingAnalysis;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MeetingAnalysisRepository extends JpaRepository<MeetingAnalysis, Long> {

    @EntityGraph(attributePaths = "meeting")
    Optional<MeetingAnalysis> findByMeetingId(Long meetingId);
}
