package com.yapp.d14.common.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.regex.Pattern;

/**
 * 개발 단계 프론트-백엔드 연동 디버깅용. 한 요청의 method/URI/status/소요시간과
 * request·response body를 한 줄로 남긴다. {@link TraceIdFilter} 뒤(안쪽)에 등록되어
 * 로그에 traceId가 실리므로, CloudWatch/콘솔에서 traceId 기준으로 요청 전체를 묶어 볼 수 있다.
 *
 * <p>프로덕션에서는 PII·성능 문제로 켜지 않는다 — {@code local}/{@code dev} 프로파일에서만
 * 빈이 등록된다({@code RequestLoggingConfig}). 토큰성 필드는 값을 마스킹하고, 멀티파트 업로드나
 * 오디오 스트리밍처럼 body를 캐싱하면 안 되는 경로는 {@link #shouldNotFilter}로 제외한다.
 */
@Slf4j
public class RequestResponseLoggingFilter extends OncePerRequestFilter {

    private static final int MAX_BODY_LENGTH = 4096;

    // 바이너리/스트리밍 응답이거나 로깅 의미가 없는 경로. 특히 오디오 스트리밍은
    // ContentCachingResponseWrapper로 감싸면 전체 오디오가 메모리에 버퍼링되므로 반드시 제외한다.
    private static final List<String> EXCLUDED_URI_KEYWORDS = List.of(
            "/audio/stream", "/swagger-ui", "/v3/api-docs", "/health", "/interview-harness"
    );

    // JSON body에서 값을 마스킹할 토큰성 필드. 로그로 새어 나가면 계정 탈취로 이어질 수 있는 값들.
    private static final Pattern SENSITIVE_FIELD = Pattern.compile(
            "(\"(?:refreshToken|accessToken|credential|idToken|appleRefreshToken|authorizationCode|token|password)\"\\s*:\\s*\")(.*?)(\")",
            Pattern.CASE_INSENSITIVE
    );

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String uri = request.getRequestURI();
        if (EXCLUDED_URI_KEYWORDS.stream().anyMatch(uri::contains)) {
            return true;
        }
        // 멀티파트 업로드(포트폴리오 PDF 등)는 body가 대용량 바이너리라 캐싱·로깅하지 않는다.
        String contentType = request.getContentType();
        return contentType != null && contentType.toLowerCase().startsWith("multipart/");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        ContentCachingRequestWrapper wrappedRequest = new ContentCachingRequestWrapper(request);
        ContentCachingResponseWrapper wrappedResponse = new ContentCachingResponseWrapper(response);

        long start = System.currentTimeMillis();
        try {
            filterChain.doFilter(wrappedRequest, wrappedResponse);
        } finally {
            long tookMs = System.currentTimeMillis() - start;
            logExchange(wrappedRequest, wrappedResponse, tookMs);
            // 캐싱된 응답 body를 실제 응답 스트림으로 반드시 복사한다. 빠뜨리면 클라이언트가 빈 응답을 받는다.
            wrappedResponse.copyBodyToResponse();
        }
    }

    private void logExchange(ContentCachingRequestWrapper request, ContentCachingResponseWrapper response, long tookMs) {
        String query = request.getQueryString() == null ? "" : "?" + request.getQueryString();
        String requestBody = readBody(request.getContentAsByteArray(), request.getContentType());
        String responseBody = readBody(response.getContentAsByteArray(), response.getContentType());

        log.info("[HTTP] {} {}{} -> {} ({}ms) | request={} | response={}",
                request.getMethod(),
                request.getRequestURI(),
                query,
                response.getStatus(),
                tookMs,
                requestBody,
                responseBody
        );
    }

    private String readBody(byte[] content, String contentType) {
        if (content == null || content.length == 0) {
            return "";
        }
        if (!isTextual(contentType)) {
            return "[binary omitted, " + content.length + " bytes]";
        }
        String body = mask(new String(content, StandardCharsets.UTF_8));
        if (body.length() > MAX_BODY_LENGTH) {
            body = body.substring(0, MAX_BODY_LENGTH) + "...(truncated, total " + body.length() + " chars)";
        }
        // 로그 한 줄을 유지하기 위해 개행/연속 공백을 정리한다.
        return body.replaceAll("\\s*\\n\\s*", " ").trim();
    }

    private boolean isTextual(String contentType) {
        if (contentType == null) {
            return false;
        }
        String ct = contentType.toLowerCase();
        return ct.contains("json") || ct.contains("text") || ct.contains("xml") || ct.contains("x-www-form-urlencoded");
    }

    private String mask(String body) {
        return SENSITIVE_FIELD.matcher(body).replaceAll("$1***$3");
    }
}
