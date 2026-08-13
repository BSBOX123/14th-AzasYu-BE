package com.azasyu.domain.interview.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record SubmitInterviewRequest(
    @NotEmpty List<@Valid AnswerRequest> answers
) {
    public record AnswerRequest(
        @NotNull Long questionId,
        @NotBlank @Size(max = 5000) String content
    ) {
    }
}
