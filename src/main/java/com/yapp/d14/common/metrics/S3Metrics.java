package com.yapp.d14.common.metrics;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.exception.SdkServiceException;

import java.net.SocketTimeoutException;
import java.time.Duration;
import java.util.function.Supplier;

/**
 * S3 호출의 소요시간·성공여부·오류종류를 수집한다(C-6·D-5, Hilit-BE#178).
 * <p>
 * 어댑터들이 재시도를 자기 안에서 돌기 때문에 <b>메서드 전체가 아니라 시도 하나</b>를 감싼다.
 * 메서드를 통째로 감싸면 재시도가 통계에서 사라진다(AI 호출은 재시도가 상위 레이어에 있어 반대다).
 */
@Component
@RequiredArgsConstructor
public class S3Metrics {

    private static final String TIMER_NAME = "s3.operation.duration";
    private static final String ABANDONED_NAME = "s3.operation.abandoned";
    private static final String NO_ERROR = "none";
    private static final int MAX_CAUSE_DEPTH = 10;

    private final MeterRegistry meterRegistry;

    // 예외는 삼키지 않고 그대로 올려보낸다 — 계측이 본 흐름을 바꾸면 안 된다.
    public <T> T record(S3Call call, Supplier<T> attempt) {
        Timer.Sample sample = Timer.start(meterRegistry);
        try {
            T result = attempt.get();
            stop(sample, call, "success", NO_ERROR);
            return result;
        } catch (RuntimeException e) {
            stop(sample, call, "failure", errorType(e));
            throw e;
        }
    }

    // 반환값 없는 호출용. record 와 이름을 나눈 이유는 람다가 두 오버로드에 모두 맞아 모호해지기 때문.
    public void run(S3Call call, Runnable attempt) {
        record(call, () -> {
            attempt.run();
            return null;
        });
    }

    /**
     * 재시도를 다 쓰고 포기한 호출. 시도 단위 실패 수만으로는 "10건이 완전히 유실됐다"와
     * "30번 흔들렸다 다 복구됐다"를 구분할 수 없어 따로 센다 — 답변 음성처럼 예외를
     * 던지지 않고 로그만 남기는 경로에서는 이 값이 유일한 유실 신호다.
     */
    public void abandoned(S3Call call) {
        meterRegistry.counter(ABANDONED_NAME, "operation", call.operation(), "area", call.area()).increment();
    }

    private void stop(Timer.Sample sample, S3Call call, String outcome, String errorType) {
        sample.stop(Timer.builder(TIMER_NAME)
                .tag("operation", call.operation())
                .tag("area", call.area())
                .tag("outcome", outcome)
                .tag("error_type", errorType)
                .publishPercentileHistogram()
                .minimumExpectedValue(Duration.ofMillis(20))
                .maximumExpectedValue(Duration.ofSeconds(30))
                .register(meterRegistry));
    }

    private static String errorType(Throwable error) {
        // 타임아웃을 먼저 훑는다. SdkClientException이 SocketTimeoutException을 감싸는 형태라
        // 한 번만 훑으면 바깥의 SdkClientException이 먼저 걸려 network로 뭉개진다.
        if (hasCause(error, SocketTimeoutException.class)) {
            return "timeout";
        }
        Throwable cause = error;
        for (int depth = 0; cause != null && depth < MAX_CAUSE_DEPTH; depth++) {
            String type = classify(cause);
            if (type != null) {
                return type;
            }
            cause = cause.getCause() == cause ? null : cause.getCause();
        }
        return "other";
    }

    private static boolean hasCause(Throwable error, Class<? extends Throwable> type) {
        Throwable cause = error;
        for (int depth = 0; cause != null && depth < MAX_CAUSE_DEPTH; depth++) {
            if (type.isInstance(cause)) {
                return true;
            }
            cause = cause.getCause() == cause ? null : cause.getCause();
        }
        return false;
    }

    private static String classify(Throwable cause) {
        if (cause instanceof SdkServiceException serviceException) {
            if (serviceException.isThrottlingException()) {
                return "throttled";
            }
            return byStatusCode(serviceException.statusCode());
        }
        // 서비스가 응답조차 못 준 경우(연결 실패·DNS·자격증명). 우리 쪽 네트워크를 의심한다.
        if (cause instanceof SdkClientException) {
            return "network";
        }
        return null;
    }

    private static String byStatusCode(int statusCode) {
        if (statusCode == 404) {
            return "not_found";
        }
        if (statusCode == 403) {
            return "access_denied";
        }
        if (statusCode >= 500) {
            return "upstream_error";
        }
        if (statusCode >= 400) {
            return "client_error";
        }
        return "other";
    }
}
