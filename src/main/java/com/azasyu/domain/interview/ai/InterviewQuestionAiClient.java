package com.azasyu.domain.interview.ai;

import com.azasyu.domain.meeting.Meeting;
import java.util.List;

public interface InterviewQuestionAiClient {

    boolean isConfigured();

    List<String> generate(Meeting meeting, List<String> agendas);
}
