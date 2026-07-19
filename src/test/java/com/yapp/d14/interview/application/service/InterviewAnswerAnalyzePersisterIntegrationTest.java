package com.yapp.d14.interview.application.service;

import com.yapp.d14.interview.application.port.out.AnswerRepository;
import com.yapp.d14.interview.application.port.out.InterviewAxisPlanRepository;
import com.yapp.d14.interview.application.port.out.InterviewSessionRepository;
import com.yapp.d14.interview.application.port.out.QuestionCandidateRepository;
import com.yapp.d14.interview.application.port.out.QuestionRepository;
import com.yapp.d14.interview.application.port.out.StaleProbeUpdate;
import com.yapp.d14.interview.domain.Answer;
import com.yapp.d14.interview.domain.AxisTier;
import com.yapp.d14.interview.domain.InterviewAxisPlan;
import com.yapp.d14.interview.domain.InterviewSession;
import com.yapp.d14.interview.domain.InterviewSessionStatus;
import com.yapp.d14.interview.domain.JobType;
import com.yapp.d14.interview.domain.Question;
import com.yapp.d14.interview.domain.QuestionCandidate;
import com.yapp.d14.interview.domain.QuestionCandidateSource;
import com.yapp.d14.interview.domain.QuestionCandidateStaleReason;
import com.yapp.d14.interview.domain.QuestionCandidateStatus;
import com.yapp.d14.interview.domain.QuestionCandidateStrength;
import com.yapp.d14.interview.domain.TestType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

// exhaustOpenBySessionIdAndTestType(벌크 UPDATE)가 flushAutomatically 없이 실행되면,
// 같은 트랜잭션에서 방금 saveAll()한 신규 OPEN 후보를 못 보고 놓칠 수 있다는 것을 실제 DB로 검증한다.
@Tag("integration")
@SpringBootTest
class InterviewAnswerAnalyzePersisterIntegrationTest {

    @Autowired
    private InterviewAnswerAnalyzePersister interviewAnswerAnalyzePersister;

    @Autowired
    private InterviewSessionRepository interviewSessionRepository;

    @Autowired
    private QuestionRepository questionRepository;

    @Autowired
    private AnswerRepository answerRepository;

    @Autowired
    private InterviewAxisPlanRepository interviewAxisPlanRepository;

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
            jdbcTemplate.update("DELETE FROM answer WHERE session_id = ?", sessionId);
            jdbcTemplate.update("DELETE FROM interview_axis_plan WHERE session_id = ?", sessionId);
            jdbcTemplate.update("DELETE FROM interview_session WHERE id = ?", sessionId);
        });
    }

    private Long createSession() {
        InterviewSession saved = interviewSessionRepository.save(InterviewSession.of(
                null, UUID.randomUUID(), UUID.randomUUID(), JobType.BACKEND, 3, null, null, null,
                InterviewSessionStatus.IN_PROGRESS, null, null, null,
                25, 20, 10, 20, 10, 15, 0, 0
        ));
        return saved.getId();
    }

    private QuestionCandidate saveOpenCandidate(TestType testType, String probeText) {
        return questionCandidateRepository.save(QuestionCandidate.create(
                sessionId, QuestionCandidateSource.PORTFOLIO, null, testType, null,
                probeText, "echo", null, QuestionCandidateStrength.MID, null
        ));
    }

    @Test
    void axis_전환이_확정되면_이번_턴에_새로_저장된_OPEN_후보까지_EXHAUSTED로_반영되고_STALE_후보는_덮어쓰지_않는다() {
        sessionId = createSession();
        Question question = questionRepository.save(
                Question.create(sessionId, "질문", 1, 1, TestType.DEPTH, null, null, false)
        );
        InterviewAxisPlan depthPlan = interviewAxisPlanRepository.save(
                InterviewAxisPlan.create(sessionId, TestType.DEPTH, AxisTier.CORE, 3)
        );
        InterviewAxisPlan boundaryPlan = interviewAxisPlanRepository.save(
                InterviewAxisPlan.create(sessionId, TestType.BOUNDARY, AxisTier.CORE, 3)
        );

        // 트랜잭션 시작 전 이미 존재하는 OPEN 후보 (사전 preload로 쌓여있던 상황을 흉내)
        QuestionCandidate preExistingOpen = saveOpenCandidate(TestType.DEPTH, "기존 probe");
        // 모순 감지로 이번 턴에 STALE 처리될 후보 — EXHAUSTED로 덮어써지면 안 된다.
        QuestionCandidate toBeStale = saveOpenCandidate(TestType.DEPTH, "모순된 probe");
        // 다른 axis 후보 — 영향받으면 안 된다.
        QuestionCandidate otherAxisOpen = saveOpenCandidate(TestType.BOUNDARY, "다른 축 probe");

        Answer answer = Answer.create(
                sessionId, question.getId(), "답변", 0f, 5f, 5f,
                false, null, null, null, null, false, false, TestType.DEPTH
        );
        // 이번 턴에 새로 추출된 OPEN 후보 — persist() 내부 saveAll()로 저장되고, 아직 flush되지 않은 채로
        // 곧바로 벌크 UPDATE(exhaustOpenBySessionIdAndTestType) 대상이 되어야 한다.
        QuestionCandidate newlyExtracted = QuestionCandidate.create(
                sessionId, QuestionCandidateSource.ANSWER, "턴 1", TestType.DEPTH, null,
                "새 probe", "새 echo", null, QuestionCandidateStrength.HIGH, null
        );
        Question nextQuestion = Question.create(sessionId, "다음 질문", 2, 1, TestType.BOUNDARY, null, null, false);
        List<StaleProbeUpdate> staleUpdates = List.of(
                new StaleProbeUpdate(toBeStale.getId(), QuestionCandidateStaleReason.CONTRADICTED, null)
        );

        InterviewSession session = interviewSessionRepository.findById(sessionId).orElseThrow();
        interviewAnswerAnalyzePersister.persist(
                session, answer, question,
                List.of(newlyExtracted), staleUpdates, question.getTurnLevel(),
                null, 2, boundaryPlan, depthPlan, nextQuestion
        );

        List<QuestionCandidate> depthCandidates = questionCandidateRepository.findAllBySessionId(sessionId).stream()
                .filter(candidate -> candidate.getTestType() == TestType.DEPTH)
                .toList();

        assertThat(depthCandidates)
                .filteredOn(candidate -> candidate.getId().equals(preExistingOpen.getId()))
                .extracting(QuestionCandidate::getStatus)
                .containsExactly(QuestionCandidateStatus.EXHAUSTED);
        assertThat(depthCandidates)
                .filteredOn(candidate -> candidate.getProbeText().equals("새 probe"))
                .extracting(QuestionCandidate::getStatus)
                .containsExactly(QuestionCandidateStatus.EXHAUSTED);
        assertThat(depthCandidates)
                .filteredOn(candidate -> candidate.getId().equals(toBeStale.getId()))
                .extracting(QuestionCandidate::getStatus)
                .containsExactly(QuestionCandidateStatus.STALE);

        QuestionCandidate refreshedOtherAxis = questionCandidateRepository.findById(otherAxisOpen.getId()).orElseThrow();
        assertThat(refreshedOtherAxis.getStatus()).isEqualTo(QuestionCandidateStatus.OPEN);
    }
}
