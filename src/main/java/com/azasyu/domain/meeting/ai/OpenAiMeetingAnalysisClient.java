package com.azasyu.domain.meeting.ai;

import com.azasyu.global.ai.GeminiApiClient;
import com.azasyu.global.ai.GeminiApiException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class OpenAiMeetingAnalysisClient implements MeetingAnalysisAiClient {

    private final GeminiApiClient geminiApiClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public OpenAiMeetingAnalysisClient(GeminiApiClient geminiApiClient) {
        this.geminiApiClient = geminiApiClient;
    }

    @Override
    public boolean isConfigured() {
        return geminiApiClient.isConfigured();
    }

    @Override
    public MeetingAnalysisDraft analyze(MeetingContext meeting, String recordContent) {
        try {
            String output = geminiApiClient.generateStructured(
                systemPrompt(),
                "회의 제목: " + meeting.title() + "\n기존 목적: " + meeting.purpose() + "\n\n회의 원문:\n" + recordContent,
                schema());
            return objectMapper.readValue(output, MeetingAnalysisDraft.class);
        } catch (GeminiApiException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new IllegalStateException("Gemini 회의 분석 응답을 해석하지 못했습니다.", exception);
        }
    }

    private String systemPrompt() {
        return """
            회의 원문만 근거로 한국어 회의 결과를 정리하세요. 목적, 주요 논의, 명시적으로 결정된 내용,
            추가 확인이 필요한 내용을 구분하세요. 결정되지 않은 내용을 결정으로 만들지 마세요.
            사람마다 다르게 해석할 수 있도록 기준·범위·기한·완료 조건이 불명확한 원문 표현을 찾아
            표현을 그대로 제시하고 모호한 이유만 설명하세요. 후속 재합의 절차나 담당 업무는 만들지 마세요.
            """;
    }

    private Map<String, Object> schema() {
        Map<String, Object> ambiguity = Map.of(
            "type", "object",
            "properties", Map.of(
                "expression", Map.of("type", "string"),
                "reason", Map.of("type", "string")
            ),
            "required", List.of("expression", "reason"),
            "additionalProperties", false
        );
        Map<String, Object> fields = Map.of(
            "meetingPurpose", Map.of("type", "string"),
            "keyDiscussions", Map.of("type", "string"),
            "decisions", Map.of("type", "string"),
            "followUpChecks", Map.of("type", "string"),
            "ambiguities", Map.of("type", "array", "items", ambiguity)
        );
        return Map.of(
            "type", "object", "properties", fields,
            "required", List.of("meetingPurpose", "keyDiscussions", "decisions", "followUpChecks", "ambiguities"),
            "additionalProperties", false
        );
    }
}
