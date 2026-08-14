package com.azasyu.domain.meeting.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

@Schema(description = "회의 참여 코드로 합류하는 요청")
public record JoinMeetingRequest(
    @Schema(description = "8자리 영숫자 회의 참여 코드. 소문자로 보내도 대문자로 변환해 처리한다.",
        example = "K7M2QRTX", pattern = "[A-Za-z0-9]{8}")
    @NotBlank @Pattern(regexp = "[A-Za-z0-9]{8}") String joinCode
) {
}
