package com.azasyu.domain.project.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

@Schema(description = "참여 코드로 프로젝트에 참가하는 요청")
public record JoinProjectRequest(
    @Schema(description = "8자리 영숫자 참여 코드. 소문자로 보내도 대문자로 변환해 처리한다.",
        example = "DN4JMQZH", pattern = "[A-Za-z0-9]{8}")
    @NotBlank @Pattern(regexp = "[A-Za-z0-9]{8}") String joinCode
) {
}
