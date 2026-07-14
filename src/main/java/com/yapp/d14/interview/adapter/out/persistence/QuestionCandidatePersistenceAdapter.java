package com.yapp.d14.interview.adapter.out.persistence;

import com.yapp.d14.interview.adapter.out.persistence.entity.QuestionCandidateJpaEntity;
import com.yapp.d14.interview.application.port.out.QuestionCandidateRepository;
import com.yapp.d14.interview.domain.QuestionCandidate;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
class QuestionCandidatePersistenceAdapter implements QuestionCandidateRepository {

    private final QuestionCandidateJpaRepository questionCandidateJpaRepository;

    @Override
    public QuestionCandidate save(QuestionCandidate questionCandidate) {
        return questionCandidateJpaRepository.save(QuestionCandidateJpaEntity.from(questionCandidate)).toDomain();
    }

    @Override
    public Optional<QuestionCandidate> findById(Long id) {
        return questionCandidateJpaRepository.findById(id).map(QuestionCandidateJpaEntity::toDomain);
    }

    @Override
    public List<QuestionCandidate> findAllBySessionId(Long sessionId) {
        return questionCandidateJpaRepository.findAllBySessionId(sessionId).stream()
                .map(QuestionCandidateJpaEntity::toDomain)
                .toList();
    }

    @Override
    public void deleteBySessionId(Long sessionId) {
        questionCandidateJpaRepository.deleteAllBySessionId(sessionId);
    }
}
