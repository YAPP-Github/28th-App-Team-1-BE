package com.yapp.d14.common.config;

import com.yapp.d14.common.web.RequestResponseLoggingFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * 요청/응답 body 로깅 필터를 개발 프로파일({@code local}, {@code dev})에서만 활성화한다.
 * 프로덕션에서는 이 설정 자체가 로드되지 않아 필터 빈이 존재하지 않으므로,
 * {@code SecurityConfig}는 빈이 있을 때만 시큐리티 체인에 추가한다.
 */
@Configuration
@Profile({"local", "dev"})
public class RequestLoggingConfig {

    @Bean
    public RequestResponseLoggingFilter requestResponseLoggingFilter() {
        return new RequestResponseLoggingFilter();
    }

    /**
     * 위 필터는 {@code SecurityConfig}에서 {@code TraceIdFilter} 뒤에 수동으로 등록한다.
     * 서블릿 컨테이너가 Filter 타입 빈을 자동으로 한 번 더 등록하지 않도록 비활성화한다.
     */
    @Bean
    public FilterRegistrationBean<RequestResponseLoggingFilter> requestResponseLoggingFilterRegistration(
            RequestResponseLoggingFilter filter
    ) {
        FilterRegistrationBean<RequestResponseLoggingFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }
}
