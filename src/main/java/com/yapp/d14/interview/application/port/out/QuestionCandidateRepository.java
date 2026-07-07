package com.yapp.d14.interview.application.port.out;

import com.yapp.d14.interview.domain.QuestionCandidate;

import java.util.List;
import java.util.Optional;

public interface QuestionCandidateRepository {

    QuestionCandidate save(QuestionCandidate questionCandidate);

    Optional<QuestionCandidate> findById(String id);

    List<QuestionCandidate> findAllBySessionId(Long sessionId);
}
