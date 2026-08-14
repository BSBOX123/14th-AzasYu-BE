package com.azasyu.domain.meeting.repository;

import com.azasyu.domain.meeting.entity.MeetingRecord;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MeetingRecordRepository extends JpaRepository<MeetingRecord, Long> {

    boolean existsByMeetingId(Long meetingId);

    @EntityGraph(attributePaths = "meeting")
    Optional<MeetingRecord> findByMeetingId(Long meetingId);
}
