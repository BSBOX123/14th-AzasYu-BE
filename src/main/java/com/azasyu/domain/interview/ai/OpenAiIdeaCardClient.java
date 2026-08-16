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
    public IdeaCardDraft generate(
        MeetingContext meeting,
        List<String> agendas,
        List<InterviewAnswerContext> answers
    ) {
        try {
            String output = geminiApiClient.generateStructured(
                prompt(), input(meeting, agendas, answers), schema());
            return objectMapper.readValue(output, IdeaCardDraft.class);
        } catch (GeminiApiException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new IllegalStateException("Gemini 아이디어 카드 응답을 해석하지 못했습니다.", exception);
        }
    }

    private String prompt() {
        return """
            당신은 회의 참여자의 사전 인터뷰 답변을 안건별로 정리하는 AI입니다.

            작성 규칙:
            1. 회의의 모든 안건을 입력 순서대로 빠짐없이 다루세요.
            2. 각 필드 안에서 `[안건 N: 안건명]` 형식의 제목으로 안건을 구분하세요.
            3. coreOpinion에는 각 안건에 대한 핵심 의견을 작성하세요.
            4. rationale에는 해당 의견의 이유와 근거를 작성하세요.
            5. concern에는 예상되는 문제점과 우려를 작성하세요.
            6. alternative에는 참여자가 제안한 대안이나 해결 방향을 작성하세요.
            7. 답변에 없는 내용을 추측하거나 새로 만들지 마세요.
            8. 특정 안건에 관한 내용이 없다면 그 안건을 생략하지 말고 `특별한 의견 없음`이라고 작성하세요.
            9. 한 안건만 대표로 선택하거나 여러 안건을 하나로 합치지 마세요.
            10. 각 안건은 핵심을 1~2문장으로 간결하게 정리하되 참여자의 의도를 왜곡하지 마세요.
            """;
    }

    private String input(
        MeetingContext meeting,
        List<String> agendas,
        List<InterviewAnswerContext> answers
    ) {
        String numberedAgendas = agendas.isEmpty()
            ? "등록된 안건 없음"
            : java.util.stream.IntStream.range(0, agendas.size())
                .mapToObj(index -> (index + 1) + ". " + agendas.get(index))
                .collect(Collectors.joining("\n"));
        String answerText = answers.stream()
            .map(answer -> "질문: " + answer.question() + "\n답변: " + answer.answer())
            .collect(Collectors.joining("\n\n"));
        return "회의 제목: " + meeting.title()
            + "\n회의 목적: " + meeting.purpose()
            + "\n\n회의 안건:\n" + numberedAgendas
            + "\n\n인터뷰 답변:\n" + answerText;
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
