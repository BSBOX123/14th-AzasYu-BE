package com.azasyu.domain.interview.ai;

import com.azasyu.domain.meeting.ai.MeetingContext;
import java.util.List;

public interface IdeaCardAiClient {

    boolean isConfigured();

    IdeaCardDraft generate(MeetingContext meeting, List<InterviewAnswerContext> answers);
}
