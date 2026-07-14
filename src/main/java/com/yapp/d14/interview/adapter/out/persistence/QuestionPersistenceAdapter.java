package com.yapp.d14.interview.adapter.out.persistence;

import com.yapp.d14.interview.adapter.out.persistence.entity.QuestionJpaEntity;
import com.yapp.d14.interview.application.port.out.QuestionRepository;
import com.yapp.d14.interview.domain.Question;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
class QuestionPersistenceAdapter implements QuestionRepository {

    private final QuestionJpaRepository questionJpaRepository;

    @Override
    public Question save(Question question) {
        return questionJpaRepository.save(QuestionJpaEntity.from(question)).toDomain();
    }

    @Override
    public Optional<Question> findById(Long id) {
        return questionJpaRepository.findById(id).map(QuestionJpaEntity::toDomain);
    }

    @Override
    public List<Question> findAllBySessionId(Long sessionId) {
        return questionJpaRepository.findAllBySessionId(sessionId).stream()
                .map(QuestionJpaEntity::toDomain)
                .toList();
    }

    @Override
    public Optional<Question> findBySessionIdAndTurnLevel(Long sessionId, int turnLevel) {
        return questionJpaRepository.findBySessionIdAndTurnLevel(sessionId, turnLevel).map(QuestionJpaEntity::toDomain);
    }

    @Override
    public void deleteBySessionId(Long sessionId) {
        questionJpaRepository.deleteAllBySessionId(sessionId);
    }
}
