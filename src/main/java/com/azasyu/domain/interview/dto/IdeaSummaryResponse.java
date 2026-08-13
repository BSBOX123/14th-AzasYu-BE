package com.azasyu.domain.interview.dto;

import java.time.LocalDateTime;

public record IdeaSummaryResponse(
    Long id,
    Long meetingId,
    int version,
    int sourceCardCount,
    String commonOpinions,
    String differingOpinions,
    String keyConcerns,
    String discussionPoints,
    LocalDateTime refreshedAt
) {
}
