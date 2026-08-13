package com.azasyu.domain.interview.ai;

import com.azasyu.domain.interview.InterviewAnswer;
import com.azasyu.domain.meeting.Meeting;
import com.azasyu.global.ai.GeminiApiClient;
import com.azasyu.global.ai.GeminiApiException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class OpenAiIdeaCardClient implements IdeaCardAiClient {

    private final GeminiApiClient geminiApiClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public OpenAiIdeaCardClient(GeminiApiClient geminiApiClient) {
        this.geminiApiClient = geminiApiClient;
    }

    @Override
    public boolean isConfigured() {
        return geminiApiClient.isConfigured();
    }

    @Override
    public IdeaCardDraft generate(Meeting meeting, List<InterviewAnswer> answers) {
        try {
            String output = geminiApiClient.generateStructured(
                "인터뷰 답변을 왜곡하거나 새 내용을 만들지 말고, 한국어 개인 아이디어 카드로 간결하게 정리하세요. 해당 내용이 없으면 '특별한 의견 없음'으로 작성하세요.",
                input(meeting, answers), schema());
            return objectMapper.readValue(output, IdeaCardDraft.class);
        } catch (GeminiApiException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new IllegalStateException("Gemini 아이디어 카드 응답을 해석하지 못했습니다.", exception);
        }
    }

    private String input(Meeting meeting, List<InterviewAnswer> answers) {
        String answerText = answers.stream()
            .map(answer -> "질문: " + answer.getQuestion().getContent() + "\n답변: " + answer.getContent())
            .collect(Collectors.joining("\n\n"));
        return "회의 제목: " + meeting.getTitle() + "\n회의 목적: " + meeting.getPurpose() + "\n\n" + answerText;
    }

    private Map<String, Object> schema() {
        Map<String, Object> properties = Map.of(
            "coreOpinion", Map.of("type", "string"),
            "rationale", Map.of("type", "string"),
            "concern", Map.of("type", "string"),
            "alternative", Map.of("type", "string")
        );
        return Map.of(
            "type", "object", "properties", properties,
            "required", List.of("coreOpinion", "rationale", "concern", "alternative"),
            "additionalProperties", false
        );
    }
}
