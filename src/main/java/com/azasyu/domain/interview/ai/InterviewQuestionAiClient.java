package com.azasyu.domain.interview.ai;

import com.azasyu.domain.meeting.ai.MeetingContext;
import java.util.List;

public interface InterviewQuestionAiClient {

    boolean isConfigured();

    List<String> generate(MeetingContext meeting, List<String> agendas);
}
