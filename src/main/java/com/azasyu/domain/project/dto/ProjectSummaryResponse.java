package com.azasyu.domain.project.dto;

import java.time.LocalDateTime;

public record ProjectSummaryResponse(
    Long id,
    String name,
    String description,
    String myRole,
    LocalDateTime joinedAt
) {
}
