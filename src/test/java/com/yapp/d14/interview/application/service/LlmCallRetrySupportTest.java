package com.yapp.d14.interview.application.service;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LlmCallRetrySupportTest {

    @Test
    void 첫_시도에_성공하면_추가_호출_없이_값을_반환한다() {
        AtomicInteger callCount = new AtomicInteger();
        Supplier<String> call = () -> {
            callCount.incrementAndGet();
            return "성공";
        };

        String result = LlmCallRetrySupport.retry(call, 2, "TEST");

        assertThat(result).isEqualTo("성공");
        assertThat(callCount.get()).isEqualTo(1);
    }

    @Test
    void 지정한_횟수만큼_실패하다가_성공하면_그_값을_반환한다() {
        AtomicInteger callCount = new AtomicInteger();
        Supplier<String> call = () -> {
            int attempt = callCount.incrementAndGet();
            if (attempt < 2) {
                throw new RuntimeException("일시적 오류");
            }
            return "재시도 성공";
        };

        String result = LlmCallRetrySupport.retry(call, 1, "TEST");

        assertThat(result).isEqualTo("재시도 성공");
        assertThat(callCount.get()).isEqualTo(2);
    }

    @Test
    void maxRetries를_초과해_실패하면_마지막_예외를_그대로_던진다() {
        AtomicInteger callCount = new AtomicInteger();
        Supplier<String> call = () -> {
            callCount.incrementAndGet();
            throw new RuntimeException("계속 실패");
        };

        assertThatThrownBy(() -> LlmCallRetrySupport.retry(call, 1, "TEST"))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("계속 실패");
        assertThat(callCount.get()).isEqualTo(2);
    }
}
