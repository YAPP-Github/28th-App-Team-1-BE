package com.yapp.d14.common.metrics;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.retry.NonTransientAiException;
import org.springframework.ai.retry.TransientAiException;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import reactor.core.publisher.Flux;

import java.net.SocketTimeoutException;
import java.time.Duration;
import java.util.function.Supplier;

/**
 * AI 호출의 소요시간·성공여부·오류종류를 Micrometer Timer 하나로 수집한다(A-6·A-7·A-8, Hilit-BE#176).
 * 호출 수와 실패 수는 Timer의 count로 따라오므로 별도 카운터를 두지 않는다.
 */
@Component
@RequiredArgsConstructor
public class AiCallMetrics {

    private static final String TIMER_NAME = "ai.call.duration";
    private static final String NO_ERROR = "none";
    // 예외 원인 체인을 훑는 최대 깊이. 순환 참조로 무한 루프에 빠지지 않게 막는다.
    private static final int MAX_CAUSE_DEPTH = 10;

    private final MeterRegistry meterRegistry;

    // 예외는 삼키지 않고 그대로 올려보낸다 — 계측이 본 흐름을 바꾸면 안 된다.
    public <T> T record(AiCallStage stage, Supplier<T> call) {
        Timer.Sample sample = Timer.start(meterRegistry);
        try {
            T result = call.get();
            stop(sample, stage, "success", NO_ERROR);
            return result;
        } catch (RuntimeException e) {
            stop(sample, stage, "failure", errorType(e));
            throw e;
        }
    }

    // 스트리밍(TTS)은 구독 시점에 재고 종료·오류·취소 시점에 멈춘다.
    // 취소를 따로 세는 이유: 클라이언트가 끊으면 완료도 오류도 아니라 호출이 통째로 사라져 보인다.
    public <T> Flux<T> recordStream(AiCallStage stage, Supplier<Flux<T>> call) {
        return Flux.defer(() -> {
            Timer.Sample sample = Timer.start(meterRegistry);
            return call.get()
                    .doOnComplete(() -> stop(sample, stage, "success", NO_ERROR))
                    .doOnError(e -> stop(sample, stage, "failure", errorType(e)))
                    .doOnCancel(() -> stop(sample, stage, "cancelled", NO_ERROR));
        });
    }

    private void stop(Timer.Sample sample, AiCallStage stage, String outcome, String errorType) {
        sample.stop(Timer.builder(TIMER_NAME)
                .tag("provider", stage.provider())
                .tag("stage", stage.stage())
                .tag("outcome", outcome)
                .tag("error_type", errorType)
                // 파드가 2개라 P95는 파드별 값이 아니라 합산이어야 한다 → 히스토그램 버킷이 필요하다.
                // 범위를 좁혀 버킷 수를 단계당 30개 남짓으로 묶는다.
                .publishPercentileHistogram()
                .minimumExpectedValue(Duration.ofMillis(200))
                .maximumExpectedValue(Duration.ofSeconds(120))
                .register(meterRegistry));
    }

    private static String errorType(Throwable error) {
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

    private static String classify(Throwable cause) {
        // Spring AI BeanOutputConverter가 JSON 파싱 실패를 RuntimeException으로 감싸므로 원인에서 찾는다(A-7).
        if (cause instanceof JsonProcessingException) {
            return "format_invalid";
        }
        if (cause instanceof SocketTimeoutException || cause instanceof ResourceAccessException) {
            return "timeout";
        }
        // TransientAiException은 429와 5xx를 함께 쓰고 상태코드 접근자가 없어 메시지 앞자리로 가른다.
        if (cause instanceof TransientAiException) {
            String message = cause.getMessage();
            return message != null && message.startsWith("429") ? "rate_limit" : "upstream_error";
        }
        if (cause instanceof NonTransientAiException) {
            return "client_error";
        }
        // 어댑터들이 빈 응답에 한해 IllegalStateException을 던진다.
        if (cause instanceof IllegalStateException) {
            return "empty_response";
        }
        return null;
    }
}
