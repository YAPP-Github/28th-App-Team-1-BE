package com.yapp.d14.common.metrics;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.net.SocketTimeoutException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class S3MetricsTest {

    private SimpleMeterRegistry registry;
    private S3Metrics metrics;

    @BeforeEach
    void setUp() {
        registry = new SimpleMeterRegistry();
        metrics = new S3Metrics(registry);
    }

    private long count(String outcome, String errorType) {
        return registry.get("s3.operation.duration")
                .tag("operation", "put")
                .tag("area", "voice")
                .tag("outcome", outcome)
                .tag("error_type", errorType)
                .timer()
                .count();
    }

    private void failWith(RuntimeException error) {
        assertThatThrownBy(() -> metrics.record(S3Call.VOICE_PUT, () -> {
            throw error;
        })).isSameAs(error);
    }

    private static S3Exception withStatus(int statusCode) {
        return (S3Exception) S3Exception.builder().message("boom").statusCode(statusCode).build();
    }

    @Test
    void 성공하면_success로_기록하고_결과를_그대로_돌려준다() {
        String result = metrics.record(S3Call.VOICE_PUT, () -> "ok");

        assertThat(result).isEqualTo("ok");
        assertThat(count("success", "none")).isEqualTo(1);
    }

    @Test
    void 반환값이_없는_호출도_잰다() {
        metrics.run(S3Call.VOICE_PUT, () -> {
        });

        assertThat(count("success", "none")).isEqualTo(1);
    }

    @Test
    void 실패해도_예외를_삼키지_않고_그대로_올려보낸다() {
        failWith(new RuntimeException("boom"));

        assertThat(count("failure", "other")).isEqualTo(1);
    }

    @Test
    void 상태코드로_오류_종류를_가른다() {
        failWith(withStatus(404));
        assertThat(count("failure", "not_found")).isEqualTo(1);

        failWith(withStatus(403));
        assertThat(count("failure", "access_denied")).isEqualTo(1);

        failWith(withStatus(400));
        assertThat(count("failure", "client_error")).isEqualTo(1);

        failWith(withStatus(503));
        assertThat(count("failure", "upstream_error")).isEqualTo(1);
    }

    @Test
    void 요청_과다는_throttled로_센다() {
        failWith(withStatus(429));

        assertThat(count("failure", "throttled")).isEqualTo(1);
    }

    @Test
    void 서비스가_응답을_못_준_경우는_network로_센다() {
        failWith(SdkClientException.create("연결 실패"));

        assertThat(count("failure", "network")).isEqualTo(1);
    }

    @Test
    void SdkClientException이_타임아웃을_감싸도_timeout으로_센다() {
        // 이 케이스가 network 로 뭉개지지 않도록 원인 체인에서 타임아웃을 먼저 훑는다.
        failWith(SdkClientException.builder().message("read timed out").cause(new SocketTimeoutException()).build());

        assertThat(count("failure", "timeout")).isEqualTo(1);
    }

    @Test
    void 재시도를_소진하고_포기하면_따로_센다() {
        metrics.abandoned(S3Call.ANSWER_PUT);
        metrics.abandoned(S3Call.ANSWER_PUT);

        assertThat(registry.get("s3.operation.abandoned")
                .tag("operation", "put")
                .tag("area", "answer")
                .counter()
                .count()).isEqualTo(2.0);
    }

    @Test
    void 호출하기_전에는_아무것도_기록하지_않는다() {
        assertThat(registry.find("s3.operation.duration").timers()).isEmpty();
        assertThat(registry.find("s3.operation.abandoned").counters()).isEmpty();
    }
}
