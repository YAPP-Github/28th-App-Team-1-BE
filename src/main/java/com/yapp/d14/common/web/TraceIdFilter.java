package com.yapp.d14.common.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * 요청마다 traceId를 발급해 MDC에 심는다. JSON 로깅 시(logback-spring.xml, !local 프로파일)
 * 모든 로그 라인에 필드로 실려 CloudWatch에서 한 요청의 로그를 traceId 기준으로 묶어 조회할 수 있다.
 * 인증 여부와 무관하게 모든 요청에 적용되어야 하므로 JwtAuthenticationFilter보다 앞에 둔다.
 */
@Component
public class TraceIdFilter extends OncePerRequestFilter {

    private static final String TRACE_ID_MDC_KEY = "traceId";

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        MDC.put(TRACE_ID_MDC_KEY, UUID.randomUUID().toString());
        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(TRACE_ID_MDC_KEY);
        }
    }
}
