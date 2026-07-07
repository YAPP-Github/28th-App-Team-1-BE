package com.yapp.d14.interview.application.port.out;

import com.yapp.d14.interview.domain.Answer;

import java.util.List;
import java.util.Optional;

public interface AnswerRepository {

    Answer save(Answer answer);

    Optional<Answer> findById(Long id);

    List<Answer> findAllBySessionId(Long sessionId);

    Optional<Answer> findByQuestionId(Long questionId);
}
