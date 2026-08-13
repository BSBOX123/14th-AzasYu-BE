package com.azasyu.domain.auth.dto;

public record AuthResponse(
    Long userId,
    String email,
    String name,
    String accessToken,
    String tokenType,
    long expiresInSeconds
) {
}
