package com.yapp.d14.common.config;

import com.yapp.d14.common.security.JwtAuthenticationFilter;
import com.yapp.d14.common.web.RequestResponseLoggingFilter;
import com.yapp.d14.common.web.TraceIdFilter;
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
                        // Spring Security 5.8+ 기본값은 ASYNC 디스패치에도 인가를 재적용한다. AudioStreamController가
                        // StreamingResponseBody(별도 스레드)로 오디오를 흘려보내는데, 그 스레드엔 SecurityContext가
                        // 없어 스트림 완료 시 재인가가 실패하고 "응답이 이미 커밋됨" 에러가 난다. 원 요청은 이미
                        // 인가를 통과한 뒤라 ASYNC 재진입 시점의 재검사가 불필요하므로 껐다.
                        .shouldFilterAllDispatcherTypes(false)
                        .requestMatchers("/api/v1/auth/social/login", "/api/v1/auth/token/refresh").permitAll()
                        .requestMatchers("/api/v1/feedback/guest/**").permitAll()
                        .requestMatchers("/health").permitAll()
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
                        // 면접 진행 수동 테스트 하네스(정적 페이지). 액세스 토큰은 페이지 안에서 직접 입력하고,
                        // 실제 API 호출은 JwtAuthenticationFilter를 그대로 거친다.
                        .requestMatchers("/interview-harness/**").permitAll()
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(traceIdFilter, JwtAuthenticationFilter.class);

        // TraceIdFilter 바로 뒤(안쪽)에 두어 로깅 시점에 traceId가 MDC에 살아 있도록 한다.
        // 인증 실패(401) 요청까지 남기기 위해 JwtAuthenticationFilter보다 바깥에 둔다.
        RequestResponseLoggingFilter loggingFilter = requestResponseLoggingFilterProvider.getIfAvailable();
        if (loggingFilter != null) {
            http.addFilterAfter(loggingFilter, TraceIdFilter.class);
        }

        return http.build();
    }
}
