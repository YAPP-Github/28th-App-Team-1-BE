package com.yapp.d14.interview.adapter.out.persistence;

import com.yapp.d14.interview.adapter.out.persistence.entity.UtteranceSegmentJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

interface UtteranceSegmentJpaRepository extends JpaRepository<UtteranceSegmentJpaEntity, Long> {

    List<UtteranceSegmentJpaEntity> findAllBySessionIdOrderByStartSecAsc(Long sessionId);

    void deleteBySessionId(Long sessionId);
}
