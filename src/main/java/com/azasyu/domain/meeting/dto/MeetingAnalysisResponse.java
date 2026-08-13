package com.azasyu.domain.meeting.dto;

import java.time.LocalDateTime;
import java.util.List;

public record MeetingAnalysisResponse(
    Long id,
    Long meetingId,
    String status,
    String failureMessage,
    String meetingPurpose,
    String keyDiscussions,
    String decisions,
    String followUpChecks,
    List<AmbiguityResponse> ambiguities,
    LocalDateTime updatedAt
) {
    public record AmbiguityResponse(Long id, int order, String expression, String reason) {
    }
}
