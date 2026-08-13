package com.azasyu.domain.interview.dto;

import java.util.List;

public record InterviewQuestionsResponse(
    Long meetingId,
    String generationStatus,
    String failureMessage,
    List<QuestionResponse> questions
) {
    public record QuestionResponse(Long id, int order, String content) {
    }
}
