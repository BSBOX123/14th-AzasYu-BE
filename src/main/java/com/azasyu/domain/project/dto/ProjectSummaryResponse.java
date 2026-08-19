package com.azasyu.domain.project.dto;

import com.azasyu.domain.project.entity.ProjectColor;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = """
    내 프로젝트 목록의 항목. 참여 코드를 제외하면 상세 조회와 같은 정보를 담으므로
    목록 화면에서 상세를 다시 호출할 필요가 없다.
    """)
public record ProjectSummaryResponse(
    @Schema(description = "프로젝트 식별자", example = "1")
    Long id,

    @Schema(description = "프로젝트 이름", example = "해커톤 MVP")
    String name,

    @Schema(description = "프로젝트 설명", example = "가짜 합의를 줄이는 협업 서비스를 만든다.")
    String description,

    @Schema(description = "프로젝트 카드 색상", example = "BLUE")
    ProjectColor color,

    @Schema(description = "이 프로젝트에서 내 역할", example = "OWNER",
        allowableValues = {"OWNER", "MEMBER"})
    String myRole,

    @Schema(description = "내가 참여한 시각", example = "2026-08-14T10:00:00")
    LocalDateTime joinedAt,

    @Schema(description = "프로젝트 생성 시각", example = "2026-08-14T09:00:00")
    LocalDateTime createdAt,

    @Schema(description = "구성원 목록. 참여한 순서대로 정렬된다.")
    List<MemberResponse> members
) {
}
