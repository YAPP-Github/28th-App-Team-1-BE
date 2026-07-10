package com.yapp.d14.interview.adapter.out.persistence;

import com.yapp.d14.interview.adapter.out.persistence.entity.AnswerJpaEntity;
import com.yapp.d14.interview.application.port.out.AnswerRepository;
import com.yapp.d14.interview.domain.Answer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
class AnswerPersistenceAdapter implements AnswerRepository {

    private final AnswerJpaRepository answerJpaRepository;

    @Override
    public Answer save(Answer answer) {
        return answerJpaRepository.save(AnswerJpaEntity.from(answer)).toDomain();
    }

    @Override
    public Optional<Answer> findById(Long id) {
        return answerJpaRepository.findById(id).map(AnswerJpaEntity::toDomain);
    }

    @Override
    public List<Answer> findAllBySessionId(Long sessionId) {
        return answerJpaRepository.findAllBySessionId(sessionId).stream()
                .map(AnswerJpaEntity::toDomain)
                .toList();
    }

    @Override
    public Optional<Answer> findByQuestionId(Long questionId) {
        return answerJpaRepository.findByQuestionId(questionId).map(AnswerJpaEntity::toDomain);
    }
}
