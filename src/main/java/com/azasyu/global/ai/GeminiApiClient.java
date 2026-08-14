package com.azasyu.global.ai;

import com.azasyu.global.config.AppProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

/**
 * Gemini generateContent API 호출 담당.
 *
 * <p>도메인별 AI 클라이언트가 프롬프트와 JSON 스키마를 넘기면 여기서 HTTP 호출과
 * 응답 파싱만 수행함. 타임아웃은 {@link GeminiClientConfig}가 만든 RestClient에 설정돼 있음.
 */
@Component
public class GeminiApiClient {

    private static final String GENERATE_CONTENT_URL =
        "https://generativelanguage.googleapis.com/v1beta/models/{model}:generateContent";

    private final AppProperties properties;
    private final RestClient restClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public GeminiApiClient(AppProperties properties, RestClient geminiRestClient) {
        this.properties = properties;
        this.restClient = geminiRestClient;
    }

    public boolean isConfigured() {
        return StringUtils.hasText(properties.gemini().apiKey());
    }

    /**
     * 스키마에 맞는 JSON 문자열을 생성해 반환함.
     *
     * @param schema Gemini responseJsonSchema로 전달할 JSON 스키마
     * @throws GeminiApiException Gemini가 오류 응답을 주거나 타임아웃(504)된 경우
     * @throws IllegalStateException 응답을 해석할 수 없거나 생성된 텍스트가 없는 경우
     */
    public String generateStructured(String systemPrompt, String input, Map<String, Object> schema) {
        Map<String, Object> body = Map.of(
            "system_instruction", Map.of("parts", List.of(Map.of("text", systemPrompt))),
            "contents", List.of(Map.of(
                "role", "user",
                "parts", List.of(Map.of("text", input))
            )),
            "generationConfig", Map.of(
                "responseMimeType", "application/json",
                "responseJsonSchema", schema
            )
        );

        JsonNode response;
        try {
            String responseBody = restClient.post()
                .uri(GENERATE_CONTENT_URL, properties.gemini().model())
                .header("x-goog-api-key", properties.gemini().apiKey())
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(String.class);
            response = objectMapper.readTree(responseBody);
        } catch (RestClientResponseException exception) {
            throw new GeminiApiException(
                exception.getStatusCode().value(), extractGoogleMessage(exception.getResponseBodyAsString()), exception
            );
        } catch (ResourceAccessException exception) {
            throw new GeminiApiException(
                HttpStatus.GATEWAY_TIMEOUT.value(), "Gemini 응답 시간이 초과되었습니다.", exception
            );
        } catch (Exception exception) {
            throw new IllegalStateException("Gemini 응답 JSON을 해석하지 못했습니다.", exception);
        }

        JsonNode text = response == null
            ? null
            : response.path("candidates").path(0).path("content").path("parts").path(0).path("text");
        if (text == null || text.isMissingNode() || !StringUtils.hasText(text.asText())) {
            throw new IllegalStateException("Gemini 응답에 생성된 텍스트가 없습니다.");
        }
        return text.asText();
    }

    private String extractGoogleMessage(String responseBody) {
        try {
            JsonNode error = objectMapper.readTree(responseBody).path("error");
            String message = error.path("message").asText();
            return StringUtils.hasText(message) ? message : "Gemini API 요청에 실패했습니다.";
        } catch (Exception ignored) {
            return "Gemini API 요청에 실패했습니다.";
        }
    }
}
