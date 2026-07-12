package com.yapp.d14.interview.application.service;

import com.yapp.d14.interview.application.port.out.InterviewSessionRepository;
import com.yapp.d14.interview.application.port.out.QuestionCandidateRepository;
import com.yapp.d14.interview.application.port.out.QuestionRepository;
import com.yapp.d14.interview.domain.InterviewSession;
import com.yapp.d14.interview.domain.Question;
import com.yapp.d14.interview.domain.QuestionCandidate;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@RequiredArgsConstructor
class InterviewPreloadResultPersister {

    private final InterviewSessionRepository interviewSessionRepository;
    private final QuestionCandidateRepository questionCandidateRepository;
    private final QuestionRepository questionRepository;

    @Transactional
    void persist(InterviewSession session, List<QuestionCandidate> candidates, Question summaryQuestion) {
        questionCandidateRepository.deleteBySessionId(session.getId());
        questionRepository.deleteBySessionId(session.getId());

        for (QuestionCandidate candidate : candidates) {
            questionCandidateRepository.save(candidate);
        }
        questionRepository.save(summaryQuestion);

        session.markReady();
        interviewSessionRepository.save(session);
    }
}
