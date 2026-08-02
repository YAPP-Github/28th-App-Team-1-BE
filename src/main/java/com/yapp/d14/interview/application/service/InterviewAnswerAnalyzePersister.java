package com.yapp.d14.interview.application.service;

import com.yapp.d14.interview.application.port.out.AnswerRepository;
import com.yapp.d14.interview.application.port.out.InterviewAxisPlanRepository;
import com.yapp.d14.interview.application.port.out.InterviewSessionRepository;
import com.yapp.d14.interview.application.port.out.QuestionCandidateRepository;
import com.yapp.d14.interview.application.port.out.QuestionRepository;
import com.yapp.d14.interview.application.port.out.StaleProbeUpdate;
import com.yapp.d14.interview.domain.Answer;
import com.yapp.d14.interview.domain.InterviewAxisPlan;
import com.yapp.d14.interview.domain.InterviewSession;
import com.yapp.d14.interview.domain.Question;
import com.yapp.d14.interview.domain.QuestionCandidate;
import com.yapp.d14.interview.domain.TestType;
import com.yapp.d14.interview.exception.InterviewErrorCode;
import com.yapp.d14.interview.exception.InterviewException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

// turnLevel≥1 SKIP·분석 턴의 원자적 저장 단계.
@Component
@RequiredArgsConstructor
class InterviewAnswerAnalyzePersister {

    private final AnswerRepository answerRepository;
    private final QuestionRepository questionRepository;
    private final QuestionCandidateRepository questionCandidateRepository;
    private final InterviewAxisPlanRepository interviewAxisPlanRepository;
    private final InterviewSessionRepository interviewSessionRepository;

    record PersistResult(Long answerId, Question nextQuestion) {
    }

    @Transactional
    PersistResult persistSkipped(
            Answer answer,
            Question question,
            QuestionCandidate selectedProbe,
            int nextTurnLevel,
            InterviewAxisPlan nextAxisPlan,
            InterviewAxisPlan completedAxisPlan,
            Question nextQuestion
    ) {
        Long answerId = save(answer, question);
        return finishTurn(answerId, selectedProbe, nextTurnLevel, nextAxisPlan, completedAxisPlan, nextQuestion);
    }

    @Transactional
    PersistResult persist(
            InterviewSession session,
            Answer answer,
            Question question,
            List<QuestionCandidate> newProbeCandidates,
            List<StaleProbeUpdate> staleUpdates,
            int currentTurnLevel,
            QuestionCandidate selectedProbe,
            int nextTurnLevel,
            InterviewAxisPlan nextAxisPlan,
            InterviewAxisPlan completedAxisPlan,
            Question nextQuestion
    ) {
        Long answerId = save(answer, question);

        List<QuestionCandidate> candidatesToInsert = newProbeCandidates.stream()
                .filter(candidate -> candidate != selectedProbe)
                .toList();
        questionCandidateRepository.saveAll(candidatesToInsert);
        applyStaleUpdates(staleUpdates, currentTurnLevel);
        interviewSessionRepository.save(session);

        return finishTurn(answerId, selectedProbe, nextTurnLevel, nextAxisPlan, completedAxisPlan, nextQuestion);
    }

    private Long save(Answer answer, Question question) {
        Long answerId;
        try {
            answerId = answerRepository.save(answer).getId();
        } catch (DataIntegrityViolationException e) {
            throw new InterviewException(InterviewErrorCode.ANSWER_ALREADY_SUBMITTED);
        }
        questionRepository.save(question);
        return answerId;
    }

    private PersistResult finishTurn(
            Long answerId,
            QuestionCandidate selectedProbe,
            int nextTurnLevel,
            InterviewAxisPlan nextAxisPlan,
            InterviewAxisPlan completedAxisPlan,
            Question nextQuestion
    ) {
        if (completedAxisPlan != null) {
            exhaustOpenProbes(completedAxisPlan.getSessionId(), completedAxisPlan.getTestType());
            interviewAxisPlanRepository.save(completedAxisPlan);
        }
        if (selectedProbe != null) {
            selectedProbe.markUsed(nextTurnLevel);
            questionCandidateRepository.save(selectedProbe);
        }
        nextAxisPlan.incrementUsedCount();
        interviewAxisPlanRepository.save(nextAxisPlan);

        Question savedNextQuestion = questionRepository.save(nextQuestion);
        return new PersistResult(answerId, savedNextQuestion);
    }

    private void exhaustOpenProbes(Long sessionId, TestType testType) {
        questionCandidateRepository.exhaustOpenBySessionIdAndTestType(sessionId, testType);
    }

    // 건별 findById+save 반복 대신 한 번에 조회·저장한다 — 존재하지 않는 probeId는 기존과 동일하게 조용히 건너뛴다.
    private void applyStaleUpdates(List<StaleProbeUpdate> staleUpdates, int currentTurnLevel) {
        if (staleUpdates.isEmpty()) {
            return;
        }
        List<Long> probeIds = staleUpdates.stream().map(StaleProbeUpdate::probeId).toList();
        Map<Long, QuestionCandidate> probesById = questionCandidateRepository.findAllById(probeIds).stream()
                .collect(Collectors.toMap(QuestionCandidate::getId, Function.identity()));

        List<QuestionCandidate> toSave = new ArrayList<>();
        for (StaleProbeUpdate update : staleUpdates) {
            QuestionCandidate probe = probesById.get(update.probeId());
            if (probe == null) {
                continue;
            }
            probe.markStale(update.reason(), currentTurnLevel);
            toSave.add(probe);
        }
        questionCandidateRepository.saveAll(toSave);
    }
}
