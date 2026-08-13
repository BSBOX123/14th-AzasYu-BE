package com.azasyu.domain.interview.dto;

import java.time.LocalDateTime;

public record InterviewSubmissionResponse(
    Long submissionId,
    Long meetingId,
    String cardGenerationStatus,
    String failureMessage,
    LocalDateTime submittedAt,
    IdeaCardResponse ideaCard
) {
    public record IdeaCardResponse(
        Long id,
        String coreOpinion,
        String rationale,
        String concern,
        String alternative,
        LocalDateTime createdAt
    ) {
    }
}
