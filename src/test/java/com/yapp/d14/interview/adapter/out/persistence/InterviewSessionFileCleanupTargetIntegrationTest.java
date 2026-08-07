package com.yapp.d14.interview.adapter.out.persistence;

import com.yapp.d14.interview.application.port.out.FileCleanupTarget;
import com.yapp.d14.interview.application.port.out.InterviewSessionRepository;
import com.yapp.d14.interview.domain.AbandonCause;
import com.yapp.d14.interview.domain.InterviewSession;
import com.yapp.d14.interview.domain.InterviewSessionStatus;
import com.yapp.d14.interview.domain.JobType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

// 정리 대상 조회 조건을 실제 DB로 고정한다. 특히 endedAt을 남기지 않는 PRELOAD_FAILED가
// COALESCE(ended_at, created_at)로 걸리는지는 목 기반 단위 테스트로 검증되지 않는다(#136).
@Tag("integration")
@SpringBootTest
class InterviewSessionFileCleanupTargetIntegrationTest {

    private static final List<InterviewSessionStatus> REPORTLESS_END_STATUSES =
            List.of(InterviewSessionStatus.INVALID, InterviewSessionStatus.PRELOAD_FAILED);

    @Autowired
    private InterviewSessionRepository interviewSessionRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private TransactionTemplate transactionTemplate;

    private final UUID userId = UUID.randomUUID();
    private final List<Long> createdSessionIds = new ArrayList<>();

    @AfterEach
    void cleanUp() {
        transactionTemplate.executeWithoutResult(status -> {
            for (Long sessionId : createdSessionIds) {
                jdbcTemplate.update("DELETE FROM interview_session WHERE id = ?", sessionId);
            }
        });
        createdSessionIds.clear();
    }

    @Test
    void endedAt이_없는_PRELOAD_FAILED도_createdAt_기준으로_정리_대상이_된다() {
        Long sessionId = createSession(
                InterviewSessionStatus.PRELOAD_FAILED, LocalDateTime.now().minusHours(3), null, null
        );

        assertThat(findTargets()).extracting(FileCleanupTarget::sessionId).contains(sessionId);
    }

    @Test
    void endedAt이_없어도_createdAt이_유예_시간_안이면_대상이_아니다() {
        Long sessionId = createSession(
                InterviewSessionStatus.PRELOAD_FAILED, LocalDateTime.now().minusMinutes(10), null, null
        );

        assertThat(findTargets()).extracting(FileCleanupTarget::sessionId).doesNotContain(sessionId);
    }

    @Test
    void USER_EXIT로_중단된_세션은_대상이_아니다() {
        Long sessionId = createSession(
                InterviewSessionStatus.ABANDONED, LocalDateTime.now().minusHours(3),
                LocalDateTime.now().minusHours(2), AbandonCause.USER_EXIT
        );

        assertThat(findTargets()).extracting(FileCleanupTarget::sessionId).doesNotContain(sessionId);
    }

    @Test
    void 다른_사유로_중단된_세션은_대상이_된다() {
        Long sessionId = createSession(
                InterviewSessionStatus.ABANDONED, LocalDateTime.now().minusHours(3),
                LocalDateTime.now().minusHours(2), AbandonCause.NETWORK_DISCONNECT
        );

        assertThat(findTargets()).extracting(FileCleanupTarget::sessionId).contains(sessionId);
    }

    @Test
    void 정리_시각이_기록된_세션은_다시_대상이_되지_않는다() {
        Long sessionId = createSession(
                InterviewSessionStatus.PRELOAD_FAILED, LocalDateTime.now().minusHours(3), null, null
        );

        interviewSessionRepository.markFilesCleaned(List.of(sessionId), LocalDateTime.now());

        assertThat(findTargets()).extracting(FileCleanupTarget::sessionId).doesNotContain(sessionId);
    }

    @Test
    void 완료된_세션은_대상이_아니다() {
        Long sessionId = createSession(
                InterviewSessionStatus.COMPLETED, LocalDateTime.now().minusHours(3),
                LocalDateTime.now().minusHours(2), null
        );

        assertThat(findTargets()).extracting(FileCleanupTarget::sessionId).doesNotContain(sessionId);
    }

    private List<FileCleanupTarget> findTargets() {
        return interviewSessionRepository.findFileCleanupTargets(
                REPORTLESS_END_STATUSES, AbandonCause.USER_EXIT, LocalDateTime.now().minusHours(1), 500
        );
    }

    private Long createSession(
            InterviewSessionStatus status,
            LocalDateTime createdAt,
            LocalDateTime endedAt,
            AbandonCause abandonCause
    ) {
        InterviewSession saved = interviewSessionRepository.save(InterviewSession.of(
                null, userId, UUID.randomUUID(), null, JobType.BACKEND, 3, null, null, null, createdAt,
                status, null, endedAt, null,
                25, 20, 10, 20, 10, 15, 0, 0, abandonCause, null
        ));
        createdSessionIds.add(saved.getId());
        return saved.getId();
    }
}
