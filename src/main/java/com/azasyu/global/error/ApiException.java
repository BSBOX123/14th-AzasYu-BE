package com.azasyu.global.error;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * 클라이언트에 그대로 전달할 오류.
 *
 * <p>{@link GlobalExceptionHandler}가 받아 상태 코드와 {@code code}, {@code message}를
 * 응답 본문으로 내보냄. message는 사용자에게 노출되므로 내부 정보를 담지 않음.
 */
@Getter
public class ApiException extends RuntimeException {

    private final HttpStatus status;
    private final String code;

    public ApiException(HttpStatus status, String code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }
}
