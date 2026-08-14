package com.azasyu.domain.project.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "프로젝트 상세. 참여 코드와 구성원 목록을 포함한다.")
public record ProjectDetailResponse(
    @Schema(description = "프로젝트 식별자", example = "1")
    Long id,

    @Schema(description = "프로젝트 이름", example = "해커톤 MVP")
    String name,

    @Schema(description = "프로젝트 설명", example = "가짜 합의를 줄이는 협업 서비스를 만든다.")
    String description,

    @Schema(description = "팀원에게 공유하는 8자리 참여 코드", example = "DN4JMQZH")
    String joinCode,

    @Schema(description = "이 프로젝트에서 내 역할", example = "OWNER",
        allowableValues = {"OWNER", "MEMBER"})
    String myRole,

    @Schema(description = "프로젝트 생성 시각", example = "2026-08-14T10:00:00")
    LocalDateTime createdAt,

    @Schema(description = "구성원 목록. 참여한 순서대로 정렬된다.")
    List<MemberResponse> members
) {
}
