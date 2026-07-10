package com.yapp.d14.interview.application.port.out;

import com.yapp.d14.interview.domain.Question;

import java.util.List;
import java.util.Optional;

public interface QuestionRepository {

    Question save(Question question);

    Optional<Question> findById(Long id);

    List<Question> findAllBySessionId(Long sessionId);
}
