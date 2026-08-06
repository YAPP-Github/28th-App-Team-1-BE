package com.yapp.d14.interview.application.port.out;

import java.util.List;
import java.util.Optional;

public interface PriorQuestionCache {

    Optional<List<String>> get(Long sessionId);

    void save(Long sessionId, List<String> priorQuestions);

    void clear(Long sessionId);
}
