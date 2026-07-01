package com.yapp.d14.common.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yapp.d14.auth.application.port.out.JwtClaims;
import com.yapp.d14.auth.application.port.out.JwtProvider;
import com.yapp.d14.auth.exception.AuthErrorCode;
import com.yapp.d14.auth.exception.AuthException;
import com.yapp.d14.common.response.ErrorResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtProvider jwtProvider;
    private final ObjectMapper objectMapper;

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

        try {
            JwtClaims claims = jwtProvider.parseAccessToken(token);
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(claims.userId(), null, List.of());
            SecurityContextHolder.getContext().setAuthentication(authentication);
            filterChain.doFilter(request, response);
        } catch (AuthException e) {
            SecurityContextHolder.clearContext();
            writeErrorResponse(response, e.getErrorCode().getHttpStatus().value(),
                    e.getErrorCode().getCode(), e.getErrorCode().getMessage());
        } catch (Exception e) {
            log.error("[JwtFilter] 예상치 못한 오류: {}", e.getMessage());
            SecurityContextHolder.clearContext();
            writeErrorResponse(response, AuthErrorCode.INVALID_TOKEN.getHttpStatus().value(),
                    AuthErrorCode.INVALID_TOKEN.getCode(), AuthErrorCode.INVALID_TOKEN.getMessage());
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
