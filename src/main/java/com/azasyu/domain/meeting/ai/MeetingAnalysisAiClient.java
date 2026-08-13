package com.azasyu.domain.meeting.ai;

import com.azasyu.domain.meeting.Meeting;

public interface MeetingAnalysisAiClient {

    boolean isConfigured();

    MeetingAnalysisDraft analyze(Meeting meeting, String recordContent);
}
