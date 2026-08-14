package com.azasyu.domain.project.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(description = "프로젝트 구성원")
public record MemberResponse(
    @Schema(description = "사용자 식별자", example = "1")
    Long userId,

    @Schema(description = "표시 이름", example = "홍길동")
    String name,

    @Schema(description = "역할", example = "MEMBER", allowableValues = {"OWNER", "MEMBER"})
    String role,

    @Schema(description = "참여 시각", example = "2026-08-14T10:05:00")
    LocalDateTime joinedAt
) {
}
