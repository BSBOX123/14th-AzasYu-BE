package com.azasyu.domain.interview.ai;

import com.azasyu.domain.meeting.ai.MeetingContext;
import java.util.List;

public interface IdeaSummaryAiClient {

    boolean isConfigured();

    IdeaSummaryDraft generate(MeetingContext meeting, List<IdeaCardContext> cards);
}
