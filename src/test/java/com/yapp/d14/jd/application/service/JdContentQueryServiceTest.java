package com.yapp.d14.jd.application.service;

import com.yapp.d14.jd.application.port.out.JdContentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class JdContentQueryServiceTest {

    @Mock
    private JdContentRepository jdContentRepository;

    @InjectMocks
    private JdContentQueryService service;

    @Test
    void 캐시된_원문이_있으면_반환한다() {
        given(jdContentRepository.get("https://example.com/jd")).willReturn(Optional.of("JD 원문"));

        assertThat(service.getContent("https://example.com/jd")).contains("JD 원문");
    }

    @Test
    void 캐시된_원문이_없으면_빈값을_반환한다() {
        given(jdContentRepository.get("https://example.com/jd")).willReturn(Optional.empty());

        assertThat(service.getContent("https://example.com/jd")).isEmpty();
    }
}
