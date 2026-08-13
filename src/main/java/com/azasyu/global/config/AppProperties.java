package com.azasyu.global.config;

import java.time.Duration;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

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
