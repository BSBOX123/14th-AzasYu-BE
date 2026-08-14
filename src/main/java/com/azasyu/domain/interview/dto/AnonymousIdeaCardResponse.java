package com.azasyu.domain.interview.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(description = """
    익명 아이디어 카드. 작성자를 식별할 수 있는 값은 담지 않는다.
    """)
public record AnonymousIdeaCardResponse(
    @Schema(description = "카드 식별자", example = "1")
    Long id,

    @Schema(description = "핵심 의견", example = "배포 자동화는 최소 범위로 시작하자")
    String coreOpinion,

    @Schema(description = "그렇게 생각하는 이유", example = "해커톤 기간이 짧아 리스크를 줄여야 한다")
    String rationale,

    @Schema(description = "우려되는 점", example = "수동 배포가 반복되면 실수가 늘어난다")
    String concern,

    @Schema(description = "제안하는 대안", example = "CI만 먼저 붙이고 배포는 이후에 검토")
    String alternative,

    @Schema(description = "카드 생성 시각", example = "2026-08-14T14:30:05")
    LocalDateTime createdAt
) {
}
