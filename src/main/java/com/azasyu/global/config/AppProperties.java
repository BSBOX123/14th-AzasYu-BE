package com.azasyu.global.config;

import java.time.Duration;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * {@code app} 접두사 설정 묶음.
 *
 * <p>테스트용 {@code application.yml}이 메인 설정을 병합하지 않고 통째로 대체하므로,
 * 값이 없으면 record 컴포넌트가 {@code null}이 되어 컨텍스트 로딩이 깨짐.
 * 새 항목을 추가할 때는 {@code @DefaultValue}를 붙이거나 테스트 설정에도 함께 추가해야 함.
 */
@ConfigurationProperties(prefix = "app")
public record AppProperties(Jwt jwt, Gemini gemini, Cors cors) {

    public record Jwt(String secret, Duration accessTokenExpiration) {
    }

    public record Gemini(
        String apiKey,
        String model,
        @DefaultValue("5s") Duration connectTimeout,
        @DefaultValue("60s") Duration readTimeout
    ) {
    }

    public record Cors(List<String> allowedOrigins) {
    }
}
