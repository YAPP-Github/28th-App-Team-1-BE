package com.yapp.d14.interview.application.service;

import com.yapp.d14.interview.application.command.InterviewAbandonCommand;
import com.yapp.d14.interview.application.port.in.result.InterviewAbandonResult;
import com.yapp.d14.interview.application.port.out.InterviewSessionRepository;
import com.yapp.d14.interview.domain.AbandonCause;
import com.yapp.d14.interview.domain.InterviewSession;
import com.yapp.d14.interview.domain.InterviewSessionStatus;
import com.yapp.d14.interview.domain.JobType;
import com.yapp.d14.interview.exception.InterviewErrorCode;
import com.yapp.d14.interview.exception.InterviewException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 같은 세션에 대한 중단(abandon) 요청 두 건이 동시에 들어와도 — InterviewAbandonService의 IN_PROGRESS 확인과
 * InterviewAbandonPersister.persist()의 ABANDONED 전이 사이에 경합이 끼어들 수 있는 창이 있었다 —
 * findByIdForUpdate 비관적 락으로 직렬화되어 한쪽만 성공하고 다른 쪽은 항상 일관되게 SESSION_ALREADY_ENDED로
 * 실패하는지 실제 DB로 검증한다.
 */
@Tag("integration")
@SpringBootTest
class InterviewAbandonRaceConditionIntegrationTest {

    @Autowired
    private InterviewAbandonService interviewAbandonService;

    @Autowired
    private InterviewSessionRepository interviewSessionRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private TransactionTemplate transactionTemplate;

    private final UUID userId = UUID.randomUUID();
    private Long sessionId;

    @AfterEach
    void cleanUp() {
        if (sessionId == null) {
            return;
        }
        transactionTemplate.executeWithoutResult(status ->
                jdbcTemplate.update("DELETE FROM interview_session WHERE id = ?", sessionId));
        sessionId = null;
    }

    private Long createInProgressSession() {
        InterviewSession saved = interviewSessionRepository.save(InterviewSession.of(
                null, userId, UUID.randomUUID(), null, JobType.BACKEND, 3, null, null, null,
                LocalDateTime.now(), InterviewSessionStatus.IN_PROGRESS, LocalDateTime.now(), null, null,
                25, 20, 10, 20, 10, 15, 0, 0, null, null
        ));
        return saved.getId();
    }

    @RepeatedTest(20)
    void 동시_중단_요청_두_건_중_하나만_성공하고_나머지는_이미_종료됨으로_일관되게_실패한다() throws InterruptedException {
        sessionId = createInProgressSession();
        InterviewAbandonCommand command = new InterviewAbandonCommand(sessionId, AbandonCause.NETWORK_DISCONNECT);

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);

        Callable<Object> attempt = () -> {
            ready.countDown();
            awaitUninterruptibly(start);
            try {
                return interviewAbandonService.abandon(userId, command);
            } catch (InterviewException e) {
                return e;
            }
        };

        List<Future<Object>> futures;
        try {
            futures = List.of(executor.submit(attempt), executor.submit(attempt));
            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            executor.shutdown();
            assertThat(executor.awaitTermination(15, TimeUnit.SECONDS)).isTrue();
        } finally {
            executor.shutdownNow();
        }

        List<Object> results = futures.stream().map(this::getUninterruptibly).toList();
        long successCount = results.stream().filter(InterviewAbandonResult.class::isInstance).count();
        long alreadyEndedCount = results.stream()
                .filter(InterviewException.class::isInstance)
                .map(InterviewException.class::cast)
                .filter(e -> e.getErrorCode() == InterviewErrorCode.SESSION_ALREADY_ENDED)
                .count();

        assertThat(successCount).isEqualTo(1);
        assertThat(alreadyEndedCount).isEqualTo(1);

        InterviewSession finalSession = interviewSessionRepository.findById(sessionId).orElseThrow();
        assertThat(finalSession.getStatus()).isEqualTo(InterviewSessionStatus.ABANDONED);
        assertThat(finalSession.getAbandonCause()).isEqualTo(AbandonCause.NETWORK_DISCONNECT);
    }

    private void awaitUninterruptibly(CountDownLatch latch) {
        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private Object getUninterruptibly(Future<Object> future) {
        try {
            return future.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        } catch (java.util.concurrent.ExecutionException e) {
            throw new IllegalStateException(e.getCause());
        }
    }
}
