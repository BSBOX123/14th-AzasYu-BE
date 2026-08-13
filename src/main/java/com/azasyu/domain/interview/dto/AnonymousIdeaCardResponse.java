package com.azasyu.domain.interview.dto;

import java.time.LocalDateTime;

public record AnonymousIdeaCardResponse(
    Long id,
    String coreOpinion,
    String rationale,
    String concern,
    String alternative,
    LocalDateTime createdAt
) {
}
