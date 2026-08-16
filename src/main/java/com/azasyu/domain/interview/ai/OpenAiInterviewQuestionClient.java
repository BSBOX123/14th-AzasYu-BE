package com.azasyu.domain.interview.ai;

import com.azasyu.domain.meeting.ai.MeetingContext;
import com.azasyu.global.ai.GeminiApiClient;
import com.azasyu.global.ai.GeminiApiException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class OpenAiInterviewQuestionClient implements InterviewQuestionAiClient {

    private final GeminiApiClient geminiApiClient;
    private final ObjectMapper objectMapper;

    public OpenAiInterviewQuestionClient(GeminiApiClient geminiApiClient) {
        this.geminiApiClient = geminiApiClient;
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public boolean isConfigured() {
        return geminiApiClient.isConfigured();
    }

    @Override
    public List<String> generate(MeetingContext meeting, List<String> agendas) {
        try {
            String outputText = geminiApiClient.generateStructured(
                systemPrompt(), meetingPrompt(meeting, agendas), jsonSchema());
            QuestionsPayload payload = objectMapper.readValue(outputText, QuestionsPayload.class);
            if (payload.questions() == null || payload.questions().isEmpty()) {
                throw new IllegalStateException("Gemini가 질문을 반환하지 않았습니다.");
            }
            return payload.questions().stream().map(QuestionPayload::content).toList();
        } catch (GeminiApiException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new IllegalStateException("Gemini 질문 응답을 해석하지 못했습니다.", exception);
        }
    }

    private Map<String, Object> jsonSchema() {
        Map<String, Object> question = Map.of(
            "type", "object",
            "properties", Map.of("content", Map.of("type", "string")),
            "required", List.of("content"),
            "additionalProperties", false
        );
        return Map.of(
            "type", "object",
            "properties", Map.of("questions", Map.of("type", "array", "items", question)),
            "required", List.of("questions"),
            "additionalProperties", false
        );
    }

    private String systemPrompt() {
        return """
            당신은 회의 전 참여자들의 실제 생각을 안전하게 끌어내는 인터뷰 설계자입니다.
            회의 정보에 맞춰 모든 참여자에게 동일하게 제공할 한국어 질문을 생성하세요.
            질문 수는 회의 복잡도에 따라 직접 결정하고, 핵심 의견·이유·우려·반대 의견·대안을 드러내세요.
            꼬리질문은 만들지 말고, 질문끼리 중복되지 않게 하세요.
            """;
    }

    //회의 제목, 목적, 안건만 llm에 전달. -> 퀄리티 높은 질문
    private String meetingPrompt(MeetingContext meeting, List<String> agendas) {
        String agendaText = String.join("\n", agendas.stream().map(agenda -> "- " + agenda).toList());
        return "회의 제목: " + meeting.title() + "\n회의 목적: " + meeting.purpose() + "\n안건:\n" + agendaText;
    }

    private record QuestionsPayload(List<QuestionPayload> questions) {
    }

    private record QuestionPayload(String content) {
    }
}
