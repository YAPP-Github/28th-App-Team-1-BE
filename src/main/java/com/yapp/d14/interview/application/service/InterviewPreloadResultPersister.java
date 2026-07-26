package com.yapp.d14.interview.application.service;

import com.yapp.d14.interview.application.port.out.InterviewSessionRepository;
import com.yapp.d14.interview.application.port.out.QuestionCandidateRepository;
import com.yapp.d14.interview.application.port.out.QuestionRepository;
import com.yapp.d14.interview.domain.InterviewSession;
import com.yapp.d14.interview.domain.InterviewSessionStatus;
import com.yapp.d14.interview.domain.Question;
import com.yapp.d14.interview.domain.QuestionCandidate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
class InterviewPreloadResultPersister {

    private final InterviewSessionRepository interviewSessionRepository;
    private final QuestionCandidateRepository questionCandidateRepository;
    private final QuestionRepository questionRepository;

    @Transactional
    void persist(InterviewSession session, List<QuestionCandidate> candidates, Question summaryQuestion) {
        // 재시도가 늦게 성공해 실행되는 사이, 폴링 시점의 45초 타임아웃 가드가 이미 세션을 PRELOAD_FAILED로
        // 바꿔놨을 수 있다 — 그 경우 결과를 버려 늦게 도착한 성공이 실패 처리를 덮어쓰지 않게 한다.
        InterviewSession current = interviewSessionRepository.findById(session.getId()).orElse(null);
        if (current == null || current.getStatus() != InterviewSessionStatus.PREPARING) {
            log.warn("[INTERVIEW PRELOAD] 이미 처리된 세션이라 늦게 도착한 preload 결과를 버립니다: sessionId={}, currentStatus={}",
                    session.getId(), current == null ? null : current.getStatus());
            return;
        }

        questionCandidateRepository.deleteBySessionId(session.getId());
        questionRepository.deleteBySessionId(session.getId());

        for (QuestionCandidate candidate : candidates) {
            questionCandidateRepository.save(candidate);
        }
        questionRepository.save(summaryQuestion);

        current.markReady();
        interviewSessionRepository.save(current);
    }
}
