package com.yapp.d14.common.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yapp.d14.common.response.ErrorResponse;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String USER_ID_MDC_KEY = "userId";
    private static final String AUTH_FAILURE_METRIC = "auth.failure";

    private final TokenParser tokenParser;
    private final ObjectMapper objectMapper;
    private final MeterRegistry meterRegistry;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String token = extractToken(request);

        if (token == null) {
            filterChain.doFilter(request, response);
            return;
        }

        UUID userId;
        try {
            userId = tokenParser.parse(token);
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(userId, null, List.of());
            SecurityContextHolder.getContext().setAuthentication(authentication);
        } catch (TokenParseException e) {
            // code는 AuthErrorCode enum에서 오므로 태그로 써도 카디널리티가 늘지 않는다.
            meterRegistry.counter(AUTH_FAILURE_METRIC, "reason", e.getCode()).increment();
            SecurityContextHolder.clearContext();
            writeErrorResponse(response, e.getStatus(), e.getCode(), e.getMessage());
            return;
        } catch (Exception e) {
            meterRegistry.counter(AUTH_FAILURE_METRIC, "reason", "unexpected").increment();
            log.error("[JwtFilter] 예상치 못한 오류: {}", e.getMessage());
            SecurityContextHolder.clearContext();
            writeErrorResponse(response, 401, "INVALID_TOKEN", "유효하지 않은 토큰입니다.");
            return;
        }

        MDC.put(USER_ID_MDC_KEY, userId.toString());
        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(USER_ID_MDC_KEY);
        }
    }

    private void writeErrorResponse(HttpServletResponse response, int status, String code, String message)
            throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(ErrorResponse.of(code, message)));
    }

    private String extractToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            return header.substring(7);
        }
        return null;
    }
}
