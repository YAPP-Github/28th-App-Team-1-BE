package com.yapp.d14.jd.application.service;

import com.yapp.d14.jd.application.command.JdValidateCommand;
import com.yapp.d14.jd.application.port.in.JdCrawlResult;
import com.yapp.d14.jd.application.port.out.JdContentExtractor;
import com.yapp.d14.jd.application.port.out.JdContentFetcher;
import com.yapp.d14.jd.application.port.out.JdContentRepository;
import com.yapp.d14.jd.application.port.out.JdValidationRateLimiter;
import com.yapp.d14.jd.exception.JdCrawlingFailedException;
import com.yapp.d14.jd.exception.JdErrorCode;
import com.yapp.d14.jd.exception.JdException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class JdValidateServiceTest {

    private static final String JD_URL = "https://example.com/jd";

    @Mock
    private JdContentFetcher jdContentFetcher;

    @Mock
    private JdContentExtractor jdContentExtractor;

    @Mock
    private JdContentRepository jdContentRepository;

    @Mock
    private JdValidationRateLimiter jdValidationRateLimiter;

    @InjectMocks
    private JdValidateService service;

    @Test
    void 캐시된_결과가_있으면_재크롤링_없이_반환한다() {
        UUID userId = UUID.randomUUID();
        given(jdContentRepository.get(JD_URL)).willReturn(Optional.of("정제된 JD"));

        JdCrawlResult result = service.validate(new JdValidateCommand(JD_URL, userId));

        assertThat(result.isValid()).isTrue();
        assertThat(result.getContent()).isEqualTo("정제된 JD");
        verify(jdContentFetcher, never()).fetch(JD_URL);
        verify(jdValidationRateLimiter, never()).getTodayCount(userId);
    }

    @Test
    void 캐시_미스이고_횟수_제한_이내면_검증을_진행하고_성공시_카운트를_증가시킨다() {
        UUID userId = UUID.randomUUID();
        given(jdContentRepository.get(JD_URL)).willReturn(Optional.empty());
        given(jdValidationRateLimiter.getTodayCount(userId)).willReturn(4);
        given(jdContentFetcher.fetch(JD_URL)).willReturn("a".repeat(200));
        given(jdContentExtractor.extract("a".repeat(200))).willReturn("정제된 JD");

        JdCrawlResult result = service.validate(new JdValidateCommand(JD_URL, userId));

        assertThat(result.isValid()).isTrue();
        verify(jdContentRepository).save(JD_URL, "정제된 JD");
        verify(jdValidationRateLimiter).increment(userId);
    }

    @Test
    void 캐시_미스이고_횟수_제한을_초과하면_예외를_던지고_크롤링하지_않는다() {
        UUID userId = UUID.randomUUID();
        given(jdContentRepository.get(JD_URL)).willReturn(Optional.empty());
        given(jdValidationRateLimiter.getTodayCount(userId)).willReturn(5);

        assertThatThrownBy(() -> service.validate(new JdValidateCommand(JD_URL, userId)))
                .isInstanceOf(JdException.class)
                .hasFieldOrPropertyWithValue("errorCode", JdErrorCode.JD_VALIDATION_LIMIT_EXCEEDED);
        verify(jdContentFetcher, never()).fetch(JD_URL);
        verify(jdValidationRateLimiter, never()).increment(userId);
    }

    @Test
    void 크롤링에_실패하면_카운트를_증가시키지_않는다() {
        UUID userId = UUID.randomUUID();
        given(jdContentRepository.get(JD_URL)).willReturn(Optional.empty());
        given(jdValidationRateLimiter.getTodayCount(userId)).willReturn(0);
        given(jdContentFetcher.fetch(JD_URL))
                .willThrow(new JdCrawlingFailedException("크롤링 실패"));

        JdCrawlResult result = service.validate(new JdValidateCommand(JD_URL, userId));

        assertThat(result.isValid()).isFalse();
        verify(jdValidationRateLimiter, never()).increment(userId);
    }
}
