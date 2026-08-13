package com.azasyu.domain.interview.ai;

import com.azasyu.domain.interview.IdeaCard;
import com.azasyu.domain.meeting.Meeting;
import java.util.List;

public interface IdeaSummaryAiClient {

    boolean isConfigured();

    IdeaSummaryDraft generate(Meeting meeting, List<IdeaCard> cards);
}
