package com.azasyu.domain.meeting.dto;

import java.time.LocalDate;
import java.time.LocalTime;

public record MeetingSummaryResponse(
    Long id,
    String title,
    String purpose,
    LocalDate meetingDate,
    LocalTime startTime,
    Integer expectedDurationMinutes,
    int participantCount
) {
}
