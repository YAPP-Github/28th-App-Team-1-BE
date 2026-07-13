package com.yapp.d14.interview.adapter.out.persistence;

import com.yapp.d14.interview.adapter.out.persistence.entity.RedFlagJpaEntity;
import com.yapp.d14.interview.application.port.out.RedFlagRepository;
import com.yapp.d14.interview.domain.RedFlag;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
class RedFlagPersistenceAdapter implements RedFlagRepository {

    private final RedFlagJpaRepository redFlagJpaRepository;

    @Override
    public RedFlag save(RedFlag redFlag) {
        return redFlagJpaRepository.save(RedFlagJpaEntity.from(redFlag)).toDomain();
    }

    @Override
    public List<RedFlag> findAllBySessionId(Long sessionId) {
        return redFlagJpaRepository.findAllBySessionId(sessionId).stream()
                .map(RedFlagJpaEntity::toDomain)
                .toList();
    }

    @Override
    public void deleteBySessionId(Long sessionId) {
        redFlagJpaRepository.deleteBySessionId(sessionId);
    }
}
