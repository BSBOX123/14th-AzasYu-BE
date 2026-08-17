package com.azasyu.domain.project.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "프로젝트 생성 요청")
public record CreateProjectRequest(
    @Schema(description = "프로젝트 이름 (최대 20자)", example = "해커톤 MVP")
    @NotBlank(message = "프로젝트 이름을 입력해 주세요")
    @Size(max = 20, message = "프로젝트 이름은 20자 이하로 입력해 주세요")
    String name,

    @Schema(description = "프로젝트 설명 (최대 1000자)", example = "가짜 합의를 줄이는 협업 서비스를 만든다.")
    @NotBlank(message = "프로젝트 설명을 입력해 주세요")
    @Size(max = 1000, message = "프로젝트 설명은 1000자 이하로 입력해 주세요")
    String description
) {
}
