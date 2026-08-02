package com.yapp.d14.common.config;

import com.yapp.d14.common.security.JwtAuthenticationFilter;
import com.yapp.d14.common.web.RequestResponseLoggingFilter;
import com.yapp.d14.common.web.TraceIdFilter;
import jakarta.servlet.DispatcherType;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final TraceIdFilter traceIdFilter;
    // 개발 프로파일(local/dev)에서만 존재하는 요청/응답 로깅 필터. 프로덕션에는 빈이 없어 비어 있다.
    private final ObjectProvider<RequestResponseLoggingFilter> requestResponseLoggingFilterProvider;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // 커스텀 SecurityFilterChain은 Spring Boot 기본의 디스패치 타입 허용을 대체하므로 직접 넣는다.
                        // StreamingResponseBody(질문 음성 스트림) 같은 비동기 응답은 ASYNC 디스패치로 필터 체인을 다시 타는데,
                        // 그때는 SecurityContext가 없어 anyRequest().authenticated()가 Access Denied를 던진다(응답은 이미 커밋됨).
                        // ASYNC/FORWARD/ERROR는 외부 요청이 아니라 이미 인가된 요청의 내부 연속이므로 허용해야 정상 동작한다.
                        .dispatcherTypeMatchers(DispatcherType.ASYNC, DispatcherType.FORWARD, DispatcherType.ERROR).permitAll()
                        .requestMatchers("/api/v1/auth/social/login", "/api/v1/auth/token/refresh").permitAll()
                        .requestMatchers("/api/v1/feedback/guest/**").permitAll()
                        // 로그인 전 스플래시에서 호출하는 앱 버전 정책 조회
                        .requestMatchers("/api/v1/app-versions/check").permitAll()
                        .requestMatchers("/health").permitAll()
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
                        // 로컬 수동 테스트용 정적 하네스 페이지. dev 프로파일에서만 컨트롤러가 등록되어(DevInterviewTestPageController)
                        // 운영에서는 이 경로 자체가 404이므로 permitAll이어도 노출되지 않는다. API는 그대로 JWT가 필요하다.
                        .requestMatchers("/interview-test.html", "/mp4-muxer.js", "/favicon.ico").permitAll()
                        // 면접 진행 수동 테스트 하네스(정적 페이지, dev 전용). 액세스 토큰은 페이지 안에서 직접 입력하고,
                        // 실제 API 호출은 JwtAuthenticationFilter를 그대로 거친다.
                        .requestMatchers("/interview-harness/**").permitAll()
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        // 필터 실행 순서를 TraceId → (Logging) → Jwt 로 명시한다.
        // - Logging을 Jwt보다 바깥에 둬야 인증 실패(401)로 체인이 끊기는 요청까지 로깅된다.
        // - TraceId를 가장 바깥에 둬야 Logging이 로그를 남기는 시점(체인 되감기)에 traceId가 MDC에 살아 있다.
        RequestResponseLoggingFilter loggingFilter = requestResponseLoggingFilterProvider.getIfAvailable();
        if (loggingFilter != null) {
            http.addFilterBefore(loggingFilter, JwtAuthenticationFilter.class)
                    .addFilterBefore(traceIdFilter, RequestResponseLoggingFilter.class);
        } else {
            http.addFilterBefore(traceIdFilter, JwtAuthenticationFilter.class);
        }

        return http.build();
    }
}
