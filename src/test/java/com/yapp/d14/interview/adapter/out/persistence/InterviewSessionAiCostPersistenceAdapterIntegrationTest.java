package com.yapp.d14.interview.adapter.out.persistence;

import com.yapp.d14.interview.application.port.out.AiCostDelta;
import com.yapp.d14.interview.application.port.out.InterviewSessionAiCostRepository;
import com.yapp.d14.interview.domain.AiProvider;
import com.yapp.d14.interview.domain.InterviewSessionAiCost;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("integration")
@SpringBootTest
class InterviewSessionAiCostPersistenceAdapterIntegrationTest {

    private static final Long SESSION_ID = 999_999L;

    @Autowired
    private InterviewSessionAiCostRepository interviewSessionAiCostRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @AfterEach
    void tearDown() {
        jdbcTemplate.update("DELETE FROM interview_session_ai_cost WHERE interview_session_id = ?", SESSION_ID);
    }

    @Test
    @DisplayName("첫 기록은 행을 만들고 이후 기록은 같은 행에 누적한다")
    void 같은_세션의_사용량을_누적한다() {
        interviewSessionAiCostRepository.accumulate(anthropic(1_000, 200, 500, 300, 1_500_000));
        interviewSessionAiCostRepository.accumulate(anthropic(2_000, 400, 0, 900, 2_090_000));

        InterviewSessionAiCost cost = findCost();
        assertThat(cost.getAnthropicInputTokens()).isEqualTo(3_000);
        assertThat(cost.getAnthropicOutputTokens()).isEqualTo(600);
        assertThat(cost.getAnthropicCacheWriteTokens()).isEqualTo(500);
        assertThat(cost.getAnthropicCacheReadTokens()).isEqualTo(1_200);
        assertThat(cost.getAnthropicCostNanoUsd()).isEqualTo(3_590_000);
        assertThat(cost.getTotalCostNanoUsd()).isEqualTo(3_590_000);
        assertThat(cost.getCallCount()).isEqualTo(2);
    }

    @Test
    @DisplayName("provider별로 컬럼이 나뉘고 총액에는 둘 다 합산된다")
    void provider별로_나눠_기록하고_총액에_합산한다() {
        interviewSessionAiCostRepository.accumulate(anthropic(1_000, 0, 0, 0, 1_000_000));
        interviewSessionAiCostRepository.accumulate(new AiCostDelta(
                SESSION_ID, AiProvider.OPENAI, 0, 0, 0, 0, 300, 90_000, 13_500_000));

        InterviewSessionAiCost cost = findCost();
        assertThat(cost.getAnthropicCostNanoUsd()).isEqualTo(1_000_000);
        assertThat(cost.getOpenAiTtsCharacters()).isEqualTo(300);
        assertThat(cost.getOpenAiSttDurationMillis()).isEqualTo(90_000);
        assertThat(cost.getOpenAiCostNanoUsd()).isEqualTo(13_500_000);
        assertThat(cost.getTotalCostNanoUsd()).isEqualTo(14_500_000);
    }

    @Test
    @DisplayName("여러 스레드가 동시에 같은 세션을 기록해도 증분이 유실되지 않는다")
    void 동시_기록에도_증분이_유실되지_않는다() throws Exception {
        int threadCount = 32;

        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            List<Runnable> tasks = IntStream.range(0, threadCount)
                    .<Runnable>mapToObj(i -> () -> interviewSessionAiCostRepository.accumulate(
                            anthropic(10, 5, 0, 0, 1_000)))
                    .toList();
            tasks.forEach(executor::execute);
        }

        InterviewSessionAiCost cost = findCost();
        assertThat(cost.getCallCount()).isEqualTo(threadCount);
        assertThat(cost.getAnthropicInputTokens()).isEqualTo(10L * threadCount);
        assertThat(cost.getAnthropicOutputTokens()).isEqualTo(5L * threadCount);
        assertThat(cost.getTotalCostNanoUsd()).isEqualTo(1_000L * threadCount);
    }

    @Test
    @DisplayName("기록이 없는 세션은 비어 있다")
    void 기록이_없으면_비어있다() {
        Optional<InterviewSessionAiCost> cost = interviewSessionAiCostRepository.findBySessionId(SESSION_ID);

        assertThat(cost).isEmpty();
    }

    private AiCostDelta anthropic(
            long inputTokens, long outputTokens, long cacheWriteTokens, long cacheReadTokens, long costNanoUsd
    ) {
        return new AiCostDelta(SESSION_ID, AiProvider.ANTHROPIC,
                inputTokens, outputTokens, cacheWriteTokens, cacheReadTokens, 0, 0, costNanoUsd);
    }

    private InterviewSessionAiCost findCost() {
        return interviewSessionAiCostRepository.findBySessionId(SESSION_ID).orElseThrow();
    }
}
