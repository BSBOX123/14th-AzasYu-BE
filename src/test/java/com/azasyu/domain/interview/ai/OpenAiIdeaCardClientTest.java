package com.azasyu.domain.interview.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.azasyu.domain.meeting.ai.MeetingContext;
import com.azasyu.global.ai.GeminiApiClient;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class OpenAiIdeaCardClientTest {

    @Test
    void includesEveryAgendaInOrderAndRequestsAgendaSeparatedOutput() {
        GeminiApiClient geminiApiClient = mock(GeminiApiClient.class);
        when(geminiApiClient.generateStructured(
            org.mockito.ArgumentMatchers.anyString(),
            org.mockito.ArgumentMatchers.anyString(),
            anyMap()
        )).thenReturn("""
            {
              "coreOpinion": "[안건 1: 기능 범위] 핵심 기능만 구현",
              "rationale": "[안건 1: 기능 범위] 일정이 짧기 때문",
              "concern": "[안건 1: 기능 범위] 특별한 의견 없음",
              "alternative": "[안건 1: 기능 범위] 후속 버전에서 확장"
            }
            """);
        OpenAiIdeaCardClient client = new OpenAiIdeaCardClient(geminiApiClient);

        IdeaCardDraft result = client.generate(
            new MeetingContext("MVP 회의", "범위를 확정한다"),
            java.util.List.of("기능 범위", "공개 방식", "분석 결과"),
            java.util.List.of(new InterviewAnswerContext("어떤 기능이 필요한가요?", "핵심 기능만 필요합니다."))
        );

        ArgumentCaptor<String> prompt = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> input = ArgumentCaptor.forClass(String.class);
        verify(geminiApiClient).generateStructured(prompt.capture(), input.capture(), anyMap());

        assertThat(prompt.getValue())
            .contains("모든 안건")
            .contains("[안건 N: 안건명]")
            .contains("특별한 의견 없음");
        assertThat(input.getValue())
            .containsSubsequence("1. 기능 범위", "2. 공개 방식", "3. 분석 결과")
            .contains("질문: 어떤 기능이 필요한가요?", "답변: 핵심 기능만 필요합니다.");
        assertThat(result.coreOpinion()).contains("기능 범위");
    }
}
