package com.yapp.d14.interview.application.port.out;

import com.yapp.d14.interview.domain.QuestionCandidate;
import com.yapp.d14.interview.domain.TestType;

import java.util.List;
import java.util.Optional;

public interface QuestionCandidateRepository {

    QuestionCandidate save(QuestionCandidate questionCandidate);

    List<QuestionCandidate> saveAll(List<QuestionCandidate> questionCandidates);

    Optional<QuestionCandidate> findById(Long id);

    List<QuestionCandidate> findAllBySessionId(Long sessionId);

    List<QuestionCandidate> findOpenBySessionIdAndTestType(Long sessionId, TestType testType);

    void exhaustOpenBySessionIdAndTestType(Long sessionId, TestType testType);

    void deleteBySessionId(Long sessionId);
}
