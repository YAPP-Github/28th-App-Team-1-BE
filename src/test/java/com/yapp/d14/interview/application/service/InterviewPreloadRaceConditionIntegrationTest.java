package com.yapp.d14.interview.application.service;

import com.yapp.d14.interview.application.port.out.InterviewSessionRepository;
import com.yapp.d14.interview.application.port.out.QuestionCandidateRepository;
import com.yapp.d14.interview.application.port.out.QuestionRepository;
import com.yapp.d14.interview.domain.InterviewSession;
import com.yapp.d14.interview.domain.InterviewSessionStatus;
import com.yapp.d14.interview.domain.JobType;
import com.yapp.d14.interview.domain.Question;
import com.yapp.d14.interview.domain.QuestionCandidate;
import com.yapp.d14.interview.domain.QuestionCandidateSource;
import com.yapp.d14.interview.domain.QuestionCandidateStrength;
import com.yapp.d14.interview.domain.TestType;
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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 폴링 시점의 45초 타임아웃 실패 처리(markFailed)와 뒤늦게 도착한 preload 성공 저장(persist)이
 * 같은 세션 행을 동시에 건드릴 때, findByIdForUpdate 비관적 락이 둘을 직렬화해
 * 어느 한쪽만 완전히 반영되고 다른 쪽은 스스로 물러나는지(부분 반영·상태 뒤집힘 없음) 실제 DB로 검증한다.
 */
@Tag("integration")
@SpringBootTest
class InterviewPreloadRaceConditionIntegrationTest {

    @Autowired
    private InterviewPreloadFailureHandler interviewPreloadFailureHandler;

    @Autowired
    private InterviewPreloadResultPersister interviewPreloadResultPersister;

    @Autowired
    private InterviewSessionRepository interviewSessionRepository;

    @Autowired
    private QuestionRepository questionRepository;

    @Autowired
    private QuestionCandidateRepository questionCandidateRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private TransactionTemplate transactionTemplate;

    private Long sessionId;

    @AfterEach
    void cleanUp() {
        if (sessionId == null) {
            return;
        }
        transactionTemplate.executeWithoutResult(status -> {
            questionCandidateRepository.deleteBySessionId(sessionId);
            questionRepository.deleteBySessionId(sessionId);
            jdbcTemplate.update("DELETE FROM interview_session WHERE id = ?", sessionId);
        });
        sessionId = null;
    }

    private Long createPreparingSession() {
        InterviewSession saved = interviewSessionRepository.save(InterviewSession.of(
                null, UUID.randomUUID(), UUID.randomUUID(), null, JobType.BACKEND, 3, null, null, null,
                LocalDateTime.now().minusSeconds(46),
                InterviewSessionStatus.PREPARING, null, null, null,
                25, 20, 10, 20, 10, 15, 0, 0, null
        ));
        return saved.getId();
    }

    // 45번 반복해 락 획득 순서가 매 회 뒤바뀌어도(먼저 잠근 트랜잭션이 이기는 구조이므로 승자는 매번 다를 수 있다)
    // 결과가 항상 "완전한 FAILED" 또는 "완전한 READY" 둘 중 하나로만 귀결되는지 확인한다.
    @RepeatedTest(20)
    void 타임아웃_실패_처리와_늦은_preload_성공_저장이_동시에_실행되어도_한쪽만_완전히_반영된다() throws InterruptedException {
        sessionId = createPreparingSession();
        InterviewSession session = interviewSessionRepository.findById(sessionId).orElseThrow();
        List<QuestionCandidate> candidates = List.of(
                QuestionCandidate.create(
                        sessionId, QuestionCandidateSource.PORTFOLIO, null, TestType.DEPTH, null,
                        "probe", "echo", null, QuestionCandidateStrength.HIGH, null
                )
        );
        Question summaryQuestion = Question.create(sessionId, "요약 질문", 0, 0, null, null, null, false);

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);

        try {
            executor.submit(() -> {
                ready.countDown();
                awaitUninterruptibly(start);
                interviewPreloadFailureHandler.markFailed(sessionId);
            });
            executor.submit(() -> {
                ready.countDown();
                awaitUninterruptibly(start);
                interviewPreloadResultPersister.persist(session, candidates, summaryQuestion);
            });

            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            executor.shutdown();
            assertThat(executor.awaitTermination(15, TimeUnit.SECONDS)).isTrue();
        } finally {
            executor.shutdownNow();
        }

        InterviewSession finalSession = interviewSessionRepository.findById(sessionId).orElseThrow();
        List<QuestionCandidate> savedCandidates = questionCandidateRepository.findAllBySessionId(sessionId);
        var savedSummaryQuestion = questionRepository.findBySessionIdAndTurnLevel(sessionId, 0);

        if (finalSession.getStatus() == InterviewSessionStatus.PRELOAD_FAILED) {
            assertThat(savedCandidates).isEmpty();
            assertThat(savedSummaryQuestion).isEmpty();
        } else {
            assertThat(finalSession.getStatus()).isEqualTo(InterviewSessionStatus.IN_PROGRESS);
            assertThat(savedCandidates).hasSize(1);
            assertThat(savedSummaryQuestion).isPresent();
        }
    }

    private void awaitUninterruptibly(CountDownLatch latch) {
        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
