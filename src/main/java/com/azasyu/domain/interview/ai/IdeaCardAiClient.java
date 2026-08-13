package com.azasyu.domain.interview.ai;

import com.azasyu.domain.interview.InterviewAnswer;
import com.azasyu.domain.meeting.Meeting;
import java.util.List;

public interface IdeaCardAiClient {

    boolean isConfigured();

    IdeaCardDraft generate(Meeting meeting, List<InterviewAnswer> answers);
}
