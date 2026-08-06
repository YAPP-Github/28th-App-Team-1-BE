package com.yapp.d14.interview.adapter.out.persistence;

import com.yapp.d14.interview.application.port.out.InterviewSessionRepository;
import com.yapp.d14.interview.application.port.out.PriorQuestionReader;
import com.yapp.d14.interview.application.port.out.QuestionRepository;
import com.yapp.d14.interview.domain.InterviewSession;
import com.yapp.d14.interview.domain.InterviewSessionStatus;
import com.yapp.d14.interview.domain.JobType;
import com.yapp.d14.interview.domain.Question;
import com.yapp.d14.interview.domain.TestType;
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

// 이전 면접 질문 이력 조회가 "사용자에게 실제로 노출된 질문"만 돌려주는지 실제 DB로 고정한다(#133).
@Tag("integration")
@SpringBootTest
class PriorQuestionPersistenceAdapterIntegrationTest {

    @Autowired
    private PriorQuestionReader priorQuestionReader;

    @Autowired
    private InterviewSessionRepository interviewSessionRepository;

    @Autowired
    private QuestionRepository questionRepository;

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
                questionRepository.deleteBySessionId(sessionId);
                jdbcTemplate.update("DELETE FROM interview_session WHERE id = ?", sessionId);
            }
        });
        createdSessionIds.clear();
    }

    private Long createSession(InterviewSessionStatus status, LocalDateTime createdAt) {
        InterviewSession saved = interviewSessionRepository.save(InterviewSession.of(
                null, userId, UUID.randomUUID(), null, JobType.BACKEND, 3, null, null, null, createdAt,
                status, null, null, null,
                25, 20, 10, 20, 10, 15, 0, 0, null
        ));
        createdSessionIds.add(saved.getId());
        return saved.getId();
    }

    private void saveQuestion(Long sessionId, String content, int turnLevel) {
        questionRepository.save(
                Question.create(sessionId, content, turnLevel, 1, TestType.DEPTH, null, null, false)
        );
    }

    @Test
    void 완료와_중단_세션의_질문만_최신순으로_돌려준다() {
        LocalDateTime now = LocalDateTime.now();
        Long completed = createSession(InterviewSessionStatus.COMPLETED, now.minusDays(3));
        Long abandoned = createSession(InterviewSessionStatus.ABANDONED, now.minusDays(2));
        Long inProgress = createSession(InterviewSessionStatus.IN_PROGRESS, now.minusDays(1));
        Long preloadFailed = createSession(InterviewSessionStatus.PRELOAD_FAILED, now.minusDays(1));
        saveQuestion(completed, "완료 세션 질문", 1);
        saveQuestion(abandoned, "중단 세션 질문", 1);
        saveQuestion(inProgress, "진행중 세션 질문", 1);
        saveQuestion(preloadFailed, "실패 세션 질문", 1);

        List<String> priorQuestions = priorQuestionReader.readRecentQuestions(userId, null);

        assertThat(priorQuestions)
                .containsExactlyInAnyOrder("완료 세션 질문", "중단 세션 질문");
    }

    @Test
    void 현재_세션의_질문은_제외한다() {
        LocalDateTime now = LocalDateTime.now();
        Long previous = createSession(InterviewSessionStatus.COMPLETED, now.minusDays(1));
        Long current = createSession(InterviewSessionStatus.COMPLETED, now);
        saveQuestion(previous, "이전 세션 질문", 1);
        saveQuestion(current, "현재 세션 질문", 1);

        List<String> priorQuestions = priorQuestionReader.readRecentQuestions(userId, current);

        assertThat(priorQuestions).containsExactly("이전 세션 질문");
    }

    @Test
    void 요약_질문은_고정_템플릿이라_제외한다() {
        Long completed = createSession(InterviewSessionStatus.COMPLETED, LocalDateTime.now().minusDays(1));
        saveQuestion(completed, "요약해서 설명해 주세요", 0);
        saveQuestion(completed, "실제 출제된 질문", 1);

        List<String> priorQuestions = priorQuestionReader.readRecentQuestions(userId, null);

        assertThat(priorQuestions).containsExactly("실제 출제된 질문");
    }

    @Test
    void 다른_사용자의_질문은_돌려주지_않는다() {
        Long mine = createSession(InterviewSessionStatus.COMPLETED, LocalDateTime.now().minusDays(1));
        saveQuestion(mine, "내 질문", 1);

        List<String> priorQuestions = priorQuestionReader.readRecentQuestions(UUID.randomUUID(), null);

        assertThat(priorQuestions).isEmpty();
    }

    @Test
    void 최근_5개_세션까지만_조회한다() {
        LocalDateTime now = LocalDateTime.now();
        for (int i = 0; i < 7; i++) {
            Long sessionId = createSession(InterviewSessionStatus.COMPLETED, now.minusDays(7 - i));
            saveQuestion(sessionId, "질문" + i, 1);
        }

        List<String> priorQuestions = priorQuestionReader.readRecentQuestions(userId, null);

        assertThat(priorQuestions)
                .hasSize(5)
                .containsExactlyInAnyOrder("질문2", "질문3", "질문4", "질문5", "질문6");
    }
}
