package com.yapp.d14.common.config;

import com.yapp.d14.common.security.JwtAuthenticationFilter;
import com.yapp.d14.common.web.TraceIdFilter;
import jakarta.servlet.DispatcherType;
import lombok.RequiredArgsConstructor;
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

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
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
                        .requestMatchers("/health").permitAll()
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
                        // 로컬 수동 테스트용 정적 하네스 페이지. 페이지 자체엔 비밀이 없고 API는 그대로 JWT가 필요하다.
                        .requestMatchers("/interview-test.html", "/favicon.ico").permitAll()
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(traceIdFilter, JwtAuthenticationFilter.class)
                .build();
    }
}
