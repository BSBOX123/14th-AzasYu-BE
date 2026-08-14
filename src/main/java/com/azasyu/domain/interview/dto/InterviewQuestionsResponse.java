package com.azasyu.domain.interview.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = """
    회의별 AI 공통 질문. generationStatus가 GENERATED일 때만 questions가 채워지고
    답변을 제출할 수 있다.
    """)
public record InterviewQuestionsResponse(
    @Schema(description = "회의 식별자", example = "1")
    Long meetingId,

    @Schema(description = """
        생성 상태. PENDING이면 생성 중, FAILED면 failureMessage 확인 후 재생성,
        NOT_CONFIGURED면 서버에 Gemini 키가 없는 상태다.
        """, example = "GENERATED",
        allowableValues = {"PENDING", "GENERATED", "FAILED", "NOT_CONFIGURED"})
    String generationStatus,

    @Schema(description = "실패 사유. generationStatus가 GENERATED면 null.")
    String failureMessage,

    @Schema(description = "질문 목록. generationStatus가 GENERATED가 아니면 빈 배열.")
    List<QuestionResponse> questions
) {
    @Schema(description = "공통 질문 한 건")
    public record QuestionResponse(
        @Schema(description = "질문 식별자. 답변 제출 시 questionId로 그대로 사용한다.", example = "1")
        Long id,

        @Schema(description = "표시 순번. 1부터 시작한다.", example = "1")
        int order,

        @Schema(description = "질문 내용", example = "현재 계획된 배포 자동화 범위가 충분하다고 생각하시나요?")
        String content
    ) {
    }
}
