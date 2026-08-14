package com.azasyu.domain.project.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "프로젝트 생성 요청")
public record CreateProjectRequest(
    @Schema(description = "프로젝트 이름 (최대 100자)", example = "해커톤 MVP")
    @NotBlank @Size(max = 100) String name,

    @Schema(description = "프로젝트 설명 (최대 1000자)", example = "가짜 합의를 줄이는 협업 서비스를 만든다.")
    @NotBlank @Size(max = 1000) String description
) {
}
