package com.azasyu.domain.project.dto;

import java.time.LocalDateTime;
import java.util.List;

public record ProjectDetailResponse(
    Long id,
    String name,
    String description,
    String joinCode,
    String myRole,
    LocalDateTime createdAt,
    List<MemberResponse> members
) {
    public record MemberResponse(Long userId, String name, String role, LocalDateTime joinedAt) {
    }
}
