package com.azasyu.domain.meeting.dto;

import java.time.LocalDateTime;

public record MeetingRecordResponse(
    Long id,
    Long meetingId,
    String sourceType,
    String originalFileName,
    String content,
    LocalDateTime createdAt
) {
}
