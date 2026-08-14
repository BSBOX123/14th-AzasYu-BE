package com.azasyu.global.ai;

/**
 * Gemini 호출 실패. 상태 코드는 Gemini가 준 HTTP 상태이며, 타임아웃은 504로 표기함.
 */
public class GeminiApiException extends RuntimeException {

    private final int statusCode;

    public GeminiApiException(int statusCode, String message, Throwable cause) {
        super(message, cause);
        this.statusCode = statusCode;
    }

    public int getStatusCode() {
        return statusCode;
    }

    /** 상태 코드별로 사용자에게 보여줄 한국어 메시지를 만듦. */
    public String userMessage() {
        return switch (statusCode) {
            case 400 -> "Gemini 요청 형식이 올바르지 않습니다: " + getMessage();
            case 401, 403 -> "Gemini API 키 또는 모델 접근 권한을 확인해 주세요: " + getMessage();
            case 404 -> "설정한 Gemini 모델을 찾을 수 없습니다: " + getMessage();
            case 429 -> "Gemini 무료 사용 한도를 초과했습니다. 잠시 후 다시 시도해 주세요.";
            case 504 -> "Gemini 응답이 지연되어 요청을 종료했습니다. 잠시 후 다시 시도해 주세요.";
            default -> "Gemini API 호출에 실패했습니다 (HTTP " + statusCode + "): " + getMessage();
        };
    }
}
