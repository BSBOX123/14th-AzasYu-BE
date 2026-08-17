package com.azasyu.domain.meeting.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "회의 원문 등록 요청 (텍스트 직접 입력)")
public record CreateMeetingRecordRequest(
    @Schema(description = "회의 내용 전문 (최대 50만 자). 앞뒤 공백은 제거된다.",
        example = "오늘 회의에서는 배포 자동화 범위를 논의했습니다. 일단 CI만 먼저 붙이기로 했습니다.")
    @NotBlank(message = "회의 내용을 입력해 주세요")
    @Size(max = 500000, message = "회의 내용은 50만 자 이하로 입력해 주세요")
    String content
) {
}
