package com.azasyu.domain.interview.ai;

public record IdeaSummaryDraft(
    String commonOpinions,
    String differingOpinions,
    String keyConcerns,
    String discussionPoints
) {
}
