package com.azasyu.global.security;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Authorization 헤더의 Bearer 토큰을 검증해 SecurityContext에 인증 정보를 넣음.
 *
 * <p>토큰이 없거나 유효하지 않아도 예외를 던지지 않고 요청을 통과시킴. 인증이 필요한 경로의
 * 차단은 {@link com.azasyu.global.config.SecurityConfig}의 권한 설정이 담당함.
 *
 * <p>인증 주체(principal)로 사용자 식별자({@code Long})를 넣으므로 컨트롤러에서
 * {@code @AuthenticationPrincipal Long userId}로 받음.
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";
    private final JwtTokenProvider jwtTokenProvider;

    public JwtAuthenticationFilter(JwtTokenProvider jwtTokenProvider) {
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain
    ) throws ServletException, IOException {
        String authorization = request.getHeader("Authorization");

        if (authorization != null && authorization.startsWith(BEARER_PREFIX)) {
            try {
                String token = authorization.substring(BEARER_PREFIX.length());
                Long userId = Long.valueOf(jwtTokenProvider.parseClaims(token).getSubject());
                var authentication = new UsernamePasswordAuthenticationToken(
                    userId, null, List.of(new SimpleGrantedAuthority("ROLE_USER"))
                );
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } catch (JwtException | IllegalArgumentException ignored) {
                SecurityContextHolder.clearContext();
            }
        }

        filterChain.doFilter(request, response);
    }
}
