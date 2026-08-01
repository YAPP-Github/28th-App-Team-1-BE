package com.yapp.d14.interview.application.port.out;

import com.yapp.d14.interview.domain.Question;

import java.util.List;
import java.util.Optional;

public interface QuestionRepository {

    Question save(Question question);

    Optional<Question> findById(Long id);

    List<Question> findAllBySessionId(Long sessionId);

    Optional<Question> findBySessionIdAndTurnLevel(Long sessionId, int turnLevel);

    // turnLevel이 가장 큰 Question 하나 — 재개(API A') 시 "이어서 답할 질문"을 결정하는 데 쓴다.
    Optional<Question> findLatestBySessionId(Long sessionId);

    void deleteBySessionId(Long sessionId);
}
