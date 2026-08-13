package com.azasyu.domain.interview.ai;

import com.azasyu.domain.meeting.ai.MeetingContext;
import com.azasyu.global.ai.GeminiApiClient;
import com.azasyu.global.ai.GeminiApiException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class OpenAiIdeaSummaryClient implements IdeaSummaryAiClient {

    private final GeminiApiClient geminiApiClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public OpenAiIdeaSummaryClient(GeminiApiClient geminiApiClient) {
        this.geminiApiClient = geminiApiClient;
    }

    @Override
    public boolean isConfigured() {
        return geminiApiClient.isConfigured();
    }

    @Override
    public IdeaSummaryDraft generate(MeetingContext meeting, List<IdeaCardContext> cards) {
        try {
            String output = geminiApiClient.generateStructured(
                "익명 아이디어 카드들만 근거로 전체 의견을 한국어로 종합하세요. 없는 합의나 의견을 만들지 말고, 소수 의견도 누락하지 마세요.",
                input(meeting, cards), schema());
            return objectMapper.readValue(output, IdeaSummaryDraft.class);
        } catch (GeminiApiException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new IllegalStateException("Gemini 전체 의견 요약 응답을 해석하지 못했습니다.", exception);
        }
    }

    private String input(MeetingContext meeting, List<IdeaCardContext> cards) {
        String cardText = cards.stream().map(card -> """
            핵심 의견: %s
            이유: %s
            우려: %s
            대안: %s
            """.formatted(card.coreOpinion(), card.rationale(), card.concern(), card.alternative()))
            .collect(Collectors.joining("\n---\n"));
        return "회의 제목: " + meeting.title() + "\n회의 목적: " + meeting.purpose() + "\n\n익명 카드:\n" + cardText;
    }

    private Map<String, Object> schema() {
        Map<String, Object> fields = Map.of(
            "commonOpinions", Map.of("type", "string"),
            "differingOpinions", Map.of("type", "string"),
            "keyConcerns", Map.of("type", "string"),
            "discussionPoints", Map.of("type", "string")
        );
        return Map.of(
            "type", "object", "properties", fields,
            "required", List.of("commonOpinions", "differingOpinions", "keyConcerns", "discussionPoints"),
            "additionalProperties", false
        );
    }
}
