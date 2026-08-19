package com.azasyu.domain.interview.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(description = """
    익명 카드 전체를 AI가 종합한 전체 의견 요약.
    새로고침할 때마다 version이 올라가며 이전 버전을 덮어쓰지 않는다.
    """)
public record IdeaSummaryResponse(
    @Schema(description = "요약 식별자", example = "1")
    Long id,

    @Schema(description = "회의 식별자", example = "1")
    Long meetingId,

    @Schema(description = "요약 버전. 새로고침할 때마다 1씩 증가한다.", example = "1")
    int version,

    @Schema(description = "이 요약을 만들 때 사용한 카드 수", example = "5")
    int sourceCardCount,

    @Schema(description = "공통적으로 나타난 의견")
    String commonOpinions,

    @Schema(description = "서로 갈리는 의견")
    String differingOpinions,

    @Schema(description = "주요 우려 사항")
    String keyConcerns,

    @Schema(description = "추가 논의가 필요한 지점")
    String discussionPoints,

    @Schema(description = "요약 생성 이후 카드가 수정·삭제되어 새로고침이 필요한지 여부", example = "true")
    boolean isOutdated,

    @Schema(description = "요약 생성 시각", example = "2026-08-14T16:00:00")
    LocalDateTime refreshedAt
) {
}
