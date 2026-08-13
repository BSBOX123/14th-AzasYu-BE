package com.azasyu.global.config;

import java.time.Duration;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public record AppProperties(Jwt jwt, Gemini gemini, Cors cors) {

    public record Jwt(String secret, Duration accessTokenExpiration) {
    }

    public record Gemini(String apiKey, String model) {
    }

    public record Cors(List<String> allowedOrigins) {
    }
}
