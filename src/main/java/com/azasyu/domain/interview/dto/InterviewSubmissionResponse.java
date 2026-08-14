package com.azasyu.domain.interview.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(description = """
    내 인터뷰 제출 내역과 생성된 개인 아이디어 카드.
    카드 생성이 실패해도 제출 자체는 보존된다.
    """)
public record InterviewSubmissionResponse(
    @Schema(description = "제출 식별자", example = "1")
    Long submissionId,

    @Schema(description = "회의 식별자", example = "1")
    Long meetingId,

    @Schema(description = """
        아이디어 카드 생성 상태. PENDING이면 생성 중, FAILED면 재생성 API로 복구,
        NOT_CONFIGURED면 서버에 Gemini 키가 없는 상태다.
        """, example = "GENERATED",
        allowableValues = {"PENDING", "GENERATED", "FAILED", "NOT_CONFIGURED"})
    String cardGenerationStatus,

    @Schema(description = "실패 사유. cardGenerationStatus가 GENERATED면 null.")
    String failureMessage,

    @Schema(description = "제출 시각", example = "2026-08-14T14:30:00")
    LocalDateTime submittedAt,

    @Schema(description = "생성된 아이디어 카드. cardGenerationStatus가 GENERATED가 아니면 null.")
    IdeaCardResponse ideaCard
) {
    @Schema(description = "내 답변에서 생성된 개인 아이디어 카드")
    public record IdeaCardResponse(
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
}
