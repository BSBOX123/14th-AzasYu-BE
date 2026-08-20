package com.azasyu.domain.interview.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "내 아이디어 카드 수정 요청")
public record UpdateIdeaCardRequest(
    @NotBlank
    @Size(max = 1000)
    @Schema(description = "핵심 의견", example = "핵심 기능의 완성도를 우선하자")
    String coreOpinion,

    @NotBlank
    @Size(max = 2000)
    @Schema(description = "그렇게 생각하는 이유", example = "5분 발표에서는 안정적인 시연이 중요하다")
    String rationale,

    @NotBlank
    @Size(max = 2000)
    @Schema(description = "우려되는 점", example = "기능을 늘리면 테스트 시간이 부족할 수 있다")
    String concern,

    @NotBlank
    @Size(max = 2000)
    @Schema(description = "제안하는 대안", example = "핵심 흐름을 먼저 완성하고 부가 기능은 이후에 추가한다")
    String alternative
) {
}
