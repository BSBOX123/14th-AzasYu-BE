package com.azasyu.domain.meeting.ai;

import java.util.List;

public record MeetingAnalysisDraft(
    String meetingPurpose,
    String keyDiscussions,
    String decisions,
    String followUpChecks,
    List<AmbiguityDraft> ambiguities
) {
    public record AmbiguityDraft(String expression, String reason) {
    }
}
