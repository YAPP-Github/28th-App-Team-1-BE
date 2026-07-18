package com.yapp.d14.interview.application.service;

import com.yapp.d14.interview.application.port.in.QuestionBoundaryQueryUseCase;
import com.yapp.d14.interview.application.port.in.result.QuestionBoundaryResult;
import com.yapp.d14.interview.application.port.out.QuestionRepository;
import com.yapp.d14.interview.domain.Question;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
class QuestionBoundaryQueryService implements QuestionBoundaryQueryUseCase {

    private final QuestionRepository questionRepository;

    @Override
    public List<QuestionBoundaryResult> getQuestionBoundaries(Long sessionId) {
        return questionRepository.findAllBySessionId(sessionId).stream()
                .filter(question -> question.getTurnLevel() != null && question.getQuestionStartSec() != null)
                .sorted(Comparator.comparing(Question::getTurnLevel))
                .map(question -> new QuestionBoundaryResult(
                        question.getTurnLevel(), question.getQuestionStartSec(), question.getContent()
                ))
                .toList();
    }
}
