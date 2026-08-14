package com.azasyu.domain.meeting.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = """
    AI 회의 분석 결과. status가 GENERATED가 아니면 본문 필드는 모두 null이다.
    """)
public record MeetingAnalysisResponse(
    @Schema(description = "분석 식별자", example = "1")
    Long id,

    @Schema(description = "회의 식별자", example = "1")
    Long meetingId,

    @Schema(description = """
        생성 상태. PENDING이면 생성 중, FAILED면 failureMessage 확인 후 재생성,
        NOT_CONFIGURED면 서버에 Gemini 키가 없는 상태다.
        """, example = "GENERATED",
        allowableValues = {"PENDING", "GENERATED", "FAILED", "NOT_CONFIGURED"})
    String status,

    @Schema(description = "실패 사유. status가 GENERATED면 null.",
        example = "회의 분석에 실패했습니다. 잠시 후 다시 시도해 주세요.")
    String failureMessage,

    @Schema(description = "AI가 원문에서 파악한 실제 회의 목적. 회의 생성 시 입력한 purpose와 다를 수 있다.")
    String meetingPurpose,

    @Schema(description = "주요 논의 내용")
    String keyDiscussions,

    @Schema(description = "결정된 사항")
    String decisions,

    @Schema(description = "추가 확인이 필요한 항목")
    String followUpChecks,

    @Schema(description = "모호하게 표현된 문장 목록. 없으면 빈 배열.")
    List<AmbiguityResponse> ambiguities,

    @Schema(description = "마지막 갱신 시각", example = "2026-08-14T15:01:00")
    LocalDateTime updatedAt
) {
    @Schema(description = "모호한 표현 한 건")
    public record AmbiguityResponse(
        @Schema(description = "탐지 결과 식별자", example = "1")
        Long id,

        @Schema(description = "표시 순번. 1부터 시작한다.", example = "1")
        int order,

        @Schema(description = "원문에서 모호하다고 판단한 표현", example = "모니터링은 적당히 하기로")
        String expression,

        @Schema(description = "모호하다고 판단한 이유",
            example = "'적당히'의 기준이 정의되지 않아 사람마다 판단이 다를 수 있음")
        String reason
    ) {
    }
}
