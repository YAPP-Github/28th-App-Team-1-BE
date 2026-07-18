package com.yapp.d14.feedback.adapter.out.persistence;

import com.yapp.d14.feedback.adapter.out.persistence.entity.GuestFeedbackJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

interface GuestFeedbackJpaRepository extends JpaRepository<GuestFeedbackJpaEntity, Long> {

    List<GuestFeedbackJpaEntity> findAllBySessionId(Long sessionId);

    long countBySessionId(Long sessionId);

    boolean existsBySessionIdAndDeviceId(Long sessionId, String deviceId);
}
