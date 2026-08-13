package com.azasyu.domain.meeting.ai;

public interface MeetingAnalysisAiClient {

    boolean isConfigured();

    MeetingAnalysisDraft analyze(MeetingContext meeting, String recordContent);
}
