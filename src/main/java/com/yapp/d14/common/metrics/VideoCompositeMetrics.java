package com.yapp.d14.common.metrics;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * 영상 합성(ffmpeg)의 소요시간과 실패 이유를 수집한다(C-4, Hilit-BE#178).
 * <p>
 * 실패 이유를 인자로 받는다 — 합성 실패는 대부분 {@code IllegalStateException} 한 종류로 던져지므로
 * 예외 타입만으로는 타임아웃과 종료코드 오류를 가를 수 없고, 메시지 문자열 매칭은 문구가 바뀌면 깨진다.
 */
@Component
@RequiredArgsConstructor
public class VideoCompositeMetrics {

    private static final String TIMER_NAME = "video.composite.duration";
    private static final String NO_ERROR = "none";

    private final MeterRegistry meterRegistry;

    public Timer.Sample start() {
        return Timer.start(meterRegistry);
    }

    public void success(Timer.Sample sample) {
        stop(sample, "success", NO_ERROR);
    }

    public void failure(Timer.Sample sample, String errorType) {
        stop(sample, "failure", errorType);
    }

    private void stop(Timer.Sample sample, String outcome, String errorType) {
        sample.stop(Timer.builder(TIMER_NAME)
                .tag("outcome", outcome)
                .tag("error_type", errorType)
                // 합성은 분 단위로 걸리고 타임아웃이 600초라 그 범위를 덮는다.
                .publishPercentileHistogram()
                .minimumExpectedValue(Duration.ofSeconds(1))
                .maximumExpectedValue(Duration.ofSeconds(600))
                .register(meterRegistry));
    }
}
