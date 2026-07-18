package com.yapp.d14.feedback.application.service;

import com.yapp.d14.feedback.application.command.GuestFeedbackSubmitCommand;
import com.yapp.d14.feedback.application.port.in.GuestFeedbackSubmitUseCase;
import com.yapp.d14.feedback.application.port.out.FeedbackShareRepository;
import com.yapp.d14.feedback.application.port.out.GuestFeedbackRepository;
import com.yapp.d14.feedback.domain.AttitudeAxis;
import com.yapp.d14.feedback.domain.FeedbackShare;
import com.yapp.d14.feedback.exception.FeedbackErrorCode;
import com.yapp.d14.feedback.exception.FeedbackException;
import com.yapp.d14.interview.application.port.out.InterviewVideoRepository;
import com.yapp.d14.interview.domain.InterviewVideo;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.IntFunction;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * exists/count 사전 체크와 save 사이의 TOCTOU 경쟁 상태를 실제 DB·트랜잭션 위에서 검증한다.
 * GuestFeedbackSubmitService가 FeedbackShare 행에 비관적 락을 걸어 동시 제출을 세션 단위로
 * 직렬화하므로, 정원(4명) 초과나 동일 기기 중복 제출이 통과해서는 안 된다.
 */
@Tag("integration")
@SpringBootTest
class GuestFeedbackSubmitServiceIntegrationTest {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    @Autowired
    private GuestFeedbackSubmitUseCase guestFeedbackSubmitUseCase;

    @Autowired
    private FeedbackShareRepository feedbackShareRepository;

    @Autowired
    private GuestFeedbackRepository guestFeedbackRepository;

    @Autowired
    private InterviewVideoRepository interviewVideoRepository;

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
            jdbcTemplate.update(
                    "DELETE FROM guest_feedback_rating WHERE guest_feedback_id IN "
                            + "(SELECT id FROM guest_feedback WHERE session_id = ?)", sessionId
            );
            jdbcTemplate.update("DELETE FROM guest_feedback WHERE session_id = ?", sessionId);
            jdbcTemplate.update(
                    "DELETE FROM feedback_share_axis WHERE feedback_share_id IN "
                            + "(SELECT id FROM feedback_share WHERE session_id = ?)", sessionId
            );
            jdbcTemplate.update("DELETE FROM feedback_share WHERE session_id = ?", sessionId);
            jdbcTemplate.update("DELETE FROM interview_video WHERE session_id = ?", sessionId);
        });
    }

    private String setUpShare(List<AttitudeAxis> axes) {
        sessionId = SECURE_RANDOM.nextLong(Long.MAX_VALUE);
        interviewVideoRepository.save(InterviewVideo.create(sessionId, LocalDateTime.now()));
        String token = generateToken();
        feedbackShareRepository.save(FeedbackShare.create(sessionId, token, axes));
        return token;
    }

    private GuestFeedbackSubmitCommand commandWith(String token, String deviceId) {
        return GuestFeedbackSubmitCommand.of(
                token, deviceId, "지인", List.of(new GuestFeedbackSubmitCommand.RawRating("GAZE", 2, null)), "전반 피드백"
        );
    }

    private String generateToken() {
        byte[] bytes = new byte[16];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    @Test
    void 정원을_초과하는_동시_제출은_4명만_성공한다() throws InterruptedException {
        String token = setUpShare(List.of(AttitudeAxis.GAZE));
        int attempts = 6;

        ConcurrentSubmitResult result = submitConcurrently(
                attempts, i -> commandWith(token, "device-" + i)
        );

        assertThat(result.successCount()).isEqualTo(4);
        assertThat(guestFeedbackRepository.countBySessionId(sessionId)).isEqualTo(4);
        assertThat(result.failures()).hasSize(2);
        assertThat(result.failures()).allMatch(code -> code == FeedbackErrorCode.FEEDBACK_CAPACITY_FULL);
    }

    @Test
    void 같은_기기의_동시_제출은_한_번만_성공한다() throws InterruptedException {
        String token = setUpShare(List.of(AttitudeAxis.GAZE));
        int attempts = 5;

        ConcurrentSubmitResult result = submitConcurrently(
                attempts, i -> commandWith(token, "device-duplicate")
        );

        assertThat(result.successCount()).isEqualTo(1);
        assertThat(guestFeedbackRepository.countBySessionId(sessionId)).isEqualTo(1);
        assertThat(result.failures()).hasSize(4);
        assertThat(result.failures()).allMatch(code -> code == FeedbackErrorCode.FEEDBACK_ALREADY_SUBMITTED);
    }

    private record ConcurrentSubmitResult(int successCount, List<FeedbackErrorCode> failures) {
    }

    private ConcurrentSubmitResult submitConcurrently(
            int attempts, IntFunction<GuestFeedbackSubmitCommand> commandFactory
    ) throws InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(attempts);
        CountDownLatch ready = new CountDownLatch(attempts);
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger successCount = new AtomicInteger();
        List<FeedbackErrorCode> failures = new CopyOnWriteArrayList<>();

        try {
            for (int i = 0; i < attempts; i++) {
                int index = i;
                executor.submit(() -> {
                    ready.countDown();
                    try {
                        start.await();
                        guestFeedbackSubmitUseCase.submit(commandFactory.apply(index));
                        successCount.incrementAndGet();
                    } catch (FeedbackException e) {
                        failures.add((FeedbackErrorCode) e.getErrorCode());
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                });
            }

            ready.await(5, TimeUnit.SECONDS);
            start.countDown();
            executor.shutdown();
            executor.awaitTermination(15, TimeUnit.SECONDS);
        } finally {
            executor.shutdownNow();
        }

        return new ConcurrentSubmitResult(successCount.get(), failures);
    }
}
