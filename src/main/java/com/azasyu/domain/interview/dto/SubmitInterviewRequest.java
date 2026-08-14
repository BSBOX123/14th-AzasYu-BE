package com.azasyu.domain.interview.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

@Schema(description = "인터뷰 답변 제출 요청")
public record SubmitInterviewRequest(
    @Schema(description = """
        답변 목록. **공통 질문 전체에 정확히 하나씩** 담아야 한다.
        하나라도 빠지면 INCOMPLETE_ANSWERS, 같은 질문이 두 번 오면 DUPLICATE_ANSWER가 반환된다.
        """)
    @NotEmpty List<@Valid AnswerRequest> answers
) {
    @Schema(description = "질문 하나에 대한 답변")
    public record AnswerRequest(
        @Schema(description = "공통 질문 조회에서 받은 질문 식별자", example = "1")
        @NotNull Long questionId,

        @Schema(description = "답변 내용 (최대 5000자). 앞뒤 공백은 제거된다.",
            example = "배포 자동화는 최소 범위로 시작하는 편이 좋다고 생각합니다.")
        @NotBlank @Size(max = 5000) String content
    ) {
    }
}
