package com.yapp.d14.common.metrics;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.retry.NonTransientAiException;
import org.springframework.ai.retry.TransientAiException;
import org.springframework.web.client.ResourceAccessException;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;

import java.net.SocketTimeoutException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AiCallMetricsTest {

    private SimpleMeterRegistry registry;
    private AiCallMetrics metrics;

    @BeforeEach
    void setUp() {
        registry = new SimpleMeterRegistry();
        metrics = new AiCallMetrics(registry);
    }

    private long count(String outcome, String errorType) {
        return registry.get("ai.call.duration")
                .tag("provider", "openai")
                .tag("stage", "stt")
                .tag("outcome", outcome)
                .tag("error_type", errorType)
                .timer()
                .count();
    }

    // 실패 케이스는 예외 종류만 바꿔가며 error_type을 확인한다.
    private void failWith(RuntimeException error) {
        assertThatThrownBy(() -> metrics.record(AiCallStage.STT, () -> {
            throw error;
        })).isSameAs(error);
    }

    @Test
    void 성공하면_success로_기록하고_결과를_그대로_돌려준다() {
        String result = metrics.record(AiCallStage.STT, () -> "ok");

        assertThat(result).isEqualTo("ok");
        assertThat(count("success", "none")).isEqualTo(1);
    }

    @Test
    void 실패해도_예외를_삼키지_않고_그대로_올려보낸다() {
        failWith(new RuntimeException("boom"));

        assertThat(count("failure", "other")).isEqualTo(1);
    }

    @Test
    void 원인_체인에_JSON_파싱_예외가_있으면_format_invalid로_센다() {
        // Spring AI BeanOutputConverter가 파싱 실패를 RuntimeException으로 감싸는 형태를 재현한다(A-7).
        JsonProcessingException parseError = new JsonProcessingException("깨진 JSON") {
        };
        failWith(new RuntimeException("변환 실패", parseError));

        assertThat(count("failure", "format_invalid")).isEqualTo(1);
    }

    @Test
    void 빈_응답으로_던진_IllegalStateException은_empty_response로_센다() {
        failWith(new IllegalStateException("Anthropic이 빈 질문 문장을 반환했어요."));

        assertThat(count("failure", "empty_response")).isEqualTo(1);
    }

    @Test
    void 타임아웃은_timeout으로_센다() {
        failWith(new ResourceAccessException("read timed out", new SocketTimeoutException()));

        assertThat(count("failure", "timeout")).isEqualTo(1);
    }

    @Test
    void TransientAiException은_429면_rate_limit_아니면_upstream_error로_가른다() {
        failWith(new TransientAiException("429 - Too Many Requests"));
        assertThat(count("failure", "rate_limit")).isEqualTo(1);

        failWith(new TransientAiException("503 - Service Unavailable"));
        assertThat(count("failure", "upstream_error")).isEqualTo(1);
    }

    @Test
    void NonTransientAiException은_client_error로_센다() {
        failWith(new NonTransientAiException("400 - Bad Request"));

        assertThat(count("failure", "client_error")).isEqualTo(1);
    }

    @Test
    void 스트리밍은_완료시_success로_센다() {
        Flux<String> flux = metrics.recordStream(AiCallStage.STT, () -> Flux.just("a", "b"));

        assertThat(flux.collectList().block()).containsExactly("a", "b");
        assertThat(count("success", "none")).isEqualTo(1);
    }

    @Test
    void 스트리밍은_구독을_끊으면_cancelled로_센다() {
        Disposable subscription = metrics.recordStream(AiCallStage.STT, Flux::never).subscribe();
        subscription.dispose();

        assertThat(count("cancelled", "none")).isEqualTo(1);
    }

    @Test
    void 스트리밍은_구독하기_전에는_아무것도_기록하지_않는다() {
        metrics.recordStream(AiCallStage.STT, () -> Flux.just("a"));

        assertThat(registry.find("ai.call.duration").timers()).isEmpty();
    }
}
