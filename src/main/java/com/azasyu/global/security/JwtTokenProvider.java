package com.azasyu.global.security;

import com.azasyu.global.config.AppProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Component;

/**
 * JWT 액세스 토큰 발급과 검증.
 *
 * <p>subject에 사용자 식별자, 클레임에 이메일을 담음. 리프레시 토큰은 사용하지 않으므로
 * 만료되면 다시 로그인해야 함.
 */
@Component
public class JwtTokenProvider {

    private final SecretKey secretKey;
    private final long accessTokenExpirationMillis;

    public JwtTokenProvider(AppProperties appProperties) {
        this.secretKey = Keys.hmacShaKeyFor(
            appProperties.jwt().secret().getBytes(StandardCharsets.UTF_8)
        );
        this.accessTokenExpirationMillis = appProperties.jwt().accessTokenExpiration().toMillis();
    }

    /** 만료 시간은 {@code app.jwt.access-token-expiration} 설정을 따름. */
    public String createAccessToken(Long userId, String email) {
        Instant now = Instant.now();
        return Jwts.builder()
            .subject(String.valueOf(userId))
            .claim("email", email)
            .issuedAt(Date.from(now))
            .expiration(Date.from(now.plusMillis(accessTokenExpirationMillis)))
            .signWith(secretKey)
            .compact();
    }

    /**
     * 서명을 검증하고 클레임을 반환함.
     *
     * @throws io.jsonwebtoken.JwtException 서명이 맞지 않거나 만료된 토큰인 경우
     */
    public Claims parseClaims(String token) {
        return Jwts.parser().verifyWith(secretKey).build()
            .parseSignedClaims(token)
            .getPayload();
    }

    public long getAccessTokenExpirationSeconds() {
        return accessTokenExpirationMillis / 1000;
    }
}
