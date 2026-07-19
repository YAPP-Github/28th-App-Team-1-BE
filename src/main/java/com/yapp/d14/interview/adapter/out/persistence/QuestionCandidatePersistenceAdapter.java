package com.yapp.d14.interview.adapter.out.persistence;

import com.yapp.d14.interview.adapter.out.persistence.entity.QuestionCandidateJpaEntity;
import com.yapp.d14.interview.application.port.out.QuestionCandidateRepository;
import com.yapp.d14.interview.domain.QuestionCandidate;
import com.yapp.d14.interview.domain.QuestionCandidateStatus;
import com.yapp.d14.interview.domain.TestType;
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
    public List<QuestionCandidate> saveAll(List<QuestionCandidate> questionCandidates) {
        List<QuestionCandidateJpaEntity> entities = questionCandidates.stream()
                .map(QuestionCandidateJpaEntity::from)
                .toList();
        return questionCandidateJpaRepository.saveAll(entities).stream()
                .map(QuestionCandidateJpaEntity::toDomain)
                .toList();
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
    public List<QuestionCandidate> findOpenBySessionIdAndTestType(Long sessionId, TestType testType) {
        return questionCandidateJpaRepository
                .findAllBySessionIdAndTestTypeAndStatus(sessionId, testType, QuestionCandidateStatus.OPEN).stream()
                .map(QuestionCandidateJpaEntity::toDomain)
                .toList();
    }

    @Override
    public void exhaustOpenBySessionIdAndTestType(Long sessionId, TestType testType) {
        questionCandidateJpaRepository.exhaustOpenBySessionIdAndTestType(sessionId, testType);
    }

    @Override
    public void deleteBySessionId(Long sessionId) {
        questionCandidateJpaRepository.deleteAllBySessionId(sessionId);
    }
}
