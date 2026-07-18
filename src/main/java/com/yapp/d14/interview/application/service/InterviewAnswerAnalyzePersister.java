package com.yapp.d14.interview.application.service;

import com.yapp.d14.interview.application.port.out.AnswerRepository;
import com.yapp.d14.interview.application.port.out.InterviewSessionRepository;
import com.yapp.d14.interview.application.port.out.QuestionCandidateRepository;
import com.yapp.d14.interview.application.port.out.QuestionRepository;
import com.yapp.d14.interview.application.port.out.StaleProbeUpdate;
import com.yapp.d14.interview.domain.Answer;
import com.yapp.d14.interview.domain.InterviewSession;
import com.yapp.d14.interview.domain.Question;
import com.yapp.d14.interview.domain.QuestionCandidate;
import com.yapp.d14.interview.exception.InterviewErrorCode;
import com.yapp.d14.interview.exception.InterviewException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

// turnLevel≥1 SKIP·분석 턴의 원자적 저장 단계. select_next_axis/probe/질문 생성은 다루지 않는다(다음 이슈 범위).
@Component
@RequiredArgsConstructor
class InterviewAnswerAnalyzePersister {

    private final AnswerRepository answerRepository;
    private final QuestionRepository questionRepository;
    private final QuestionCandidateRepository questionCandidateRepository;
    private final InterviewSessionRepository interviewSessionRepository;

    record PersistResult(Long answerId) {
    }

    // SKIP 턴: STT/캐물지점 추출/모순 감지 없이 답변·질문 재생구간만 기록한다.
    @Transactional
    PersistResult persistSkipped(Answer answer, Question question) {
        return save(answer, question);
    }

    // 분석 턴: run_live_turn 결과(new_probes/stale_updates)를 probe pool에 반영한다.
    @Transactional
    PersistResult persist(
            InterviewSession session,
            Answer answer,
            Question question,
            List<QuestionCandidate> newProbeCandidates,
            List<StaleProbeUpdate> staleUpdates,
            int currentTurnLevel
    ) {
        PersistResult result = save(answer, question);
        questionCandidateRepository.saveAll(newProbeCandidates);
        applyStaleUpdates(staleUpdates, currentTurnLevel);
        interviewSessionRepository.save(session);
        return result;
    }

    private PersistResult save(Answer answer, Question question) {
        Long answerId;
        try {
            answerId = answerRepository.save(answer).getId();
        } catch (DataIntegrityViolationException e) {
            throw new InterviewException(InterviewErrorCode.ANSWER_ALREADY_SUBMITTED);
        }
        questionRepository.save(question);
        return new PersistResult(answerId);
    }

    private void applyStaleUpdates(List<StaleProbeUpdate> staleUpdates, int currentTurnLevel) {
        for (StaleProbeUpdate update : staleUpdates) {
            questionCandidateRepository.findById(update.probeId()).ifPresent(probe -> {
                probe.markStale(update.reason(), currentTurnLevel);
                questionCandidateRepository.save(probe);
            });
        }
    }
}
