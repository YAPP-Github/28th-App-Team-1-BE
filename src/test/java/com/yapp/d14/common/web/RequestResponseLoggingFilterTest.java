package com.yapp.d14.common.web;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class RequestResponseLoggingFilterTest {

    private final RequestResponseLoggingFilter filter = new RequestResponseLoggingFilter();
    private final Logger logger = (Logger) LoggerFactory.getLogger(RequestResponseLoggingFilter.class);
    private final ListAppender<ILoggingEvent> appender = new ListAppender<>();

    @BeforeEach
    void setUp() {
        appender.start();
        logger.addAppender(appender);
    }

    @AfterEach
    void tearDown() {
        logger.detachAppender(appender);
    }

    private String loggedMessage() {
        assertThat(appender.list).hasSize(1);
        return appender.list.get(0).getFormattedMessage();
    }

    @Test
    void 요청_body의_토큰성_필드는_마스킹되고_일반_필드는_유지된다() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/auth/social/login");
        request.setContentType("application/json");
        request.setContent("{\"provider\":\"KAKAO\",\"credential\":\"super-secret-credential\",\"password\":\"pw1234\"}"
                .getBytes(StandardCharsets.UTF_8));

        filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

        String message = loggedMessage();
        assertThat(message).contains("\"provider\":\"KAKAO\"");
        assertThat(message).contains("\"credential\":\"***\"");
        assertThat(message).contains("\"password\":\"***\"");
        assertThat(message).doesNotContain("super-secret-credential");
        assertThat(message).doesNotContain("pw1234");
    }

    @Test
    void 응답_body의_accessToken과_refreshToken도_마스킹된다() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/auth/token/refresh");
        request.setContentType("application/json");
        request.setContent("{}".getBytes(StandardCharsets.UTF_8));

        FilterChain chain = (req, res) -> {
            res.setContentType("application/json");
            res.getWriter().write("{\"accessToken\":\"ACCESS-XYZ\",\"refreshToken\":\"REFRESH-XYZ\"}");
        };

        filter.doFilter(request, new MockHttpServletResponse(), chain);

        String message = loggedMessage();
        assertThat(message).contains("\"accessToken\":\"***\"");
        assertThat(message).contains("\"refreshToken\":\"***\"");
        assertThat(message).doesNotContain("ACCESS-XYZ");
        assertThat(message).doesNotContain("REFRESH-XYZ");
    }

    @Test
    void 마스킹은_대소문자를_구분하지_않는다() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/auth/token/refresh");
        request.setContentType("application/json");
        request.setContent("{\"RefreshToken\":\"case-insensitive-secret\"}".getBytes(StandardCharsets.UTF_8));

        filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

        assertThat(loggedMessage()).doesNotContain("case-insensitive-secret");
    }

    @Test
    void 멀티파트_요청은_로깅에서_제외된다() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/portfolios");
        request.setContentType("multipart/form-data; boundary=boundary");
        request.setContent("binary-pdf-bytes".getBytes(StandardCharsets.UTF_8));

        filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

        assertThat(appender.list).isEmpty();
    }

    @Test
    void 제외_경로_요청은_로깅에서_제외된다() throws Exception {
        MockHttpServletRequest request =
                new MockHttpServletRequest("GET", "/api/v1/interview/sessions/s1/questions/q1/audio/stream");

        filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

        assertThat(appender.list).isEmpty();
    }
}
