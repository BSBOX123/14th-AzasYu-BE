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

    public Claims parseClaims(String token) {
        return Jwts.parser().verifyWith(secretKey).build()
            .parseSignedClaims(token)
            .getPayload();
    }

    public long getAccessTokenExpirationSeconds() {
        return accessTokenExpirationMillis / 1000;
    }
}
