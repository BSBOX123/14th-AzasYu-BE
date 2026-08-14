package com.azasyu.domain.project.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(description = "내 프로젝트 목록의 항목")
public record ProjectSummaryResponse(
    @Schema(description = "프로젝트 식별자", example = "1")
    Long id,

    @Schema(description = "프로젝트 이름", example = "해커톤 MVP")
    String name,

    @Schema(description = "프로젝트 설명", example = "가짜 합의를 줄이는 협업 서비스를 만든다.")
    String description,

    @Schema(description = "이 프로젝트에서 내 역할", example = "OWNER",
        allowableValues = {"OWNER", "MEMBER"})
    String myRole,

    @Schema(description = "내가 참여한 시각", example = "2026-08-14T10:00:00")
    LocalDateTime joinedAt
) {
}
