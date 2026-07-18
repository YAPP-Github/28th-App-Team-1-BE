package com.yapp.d14.feedback.adapter.out.persistence;

import com.yapp.d14.feedback.adapter.out.persistence.entity.GuestFeedbackJpaEntity;
import com.yapp.d14.feedback.application.port.out.GuestFeedbackRepository;
import com.yapp.d14.feedback.domain.GuestFeedback;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
class GuestFeedbackPersistenceAdapter implements GuestFeedbackRepository {

    private final GuestFeedbackJpaRepository guestFeedbackJpaRepository;

    @Override
    public GuestFeedback save(GuestFeedback guestFeedback) {
        return guestFeedbackJpaRepository.save(GuestFeedbackJpaEntity.from(guestFeedback)).toDomain();
    }

    @Override
    public List<GuestFeedback> findAllBySessionId(Long sessionId) {
        return guestFeedbackJpaRepository.findAllBySessionId(sessionId).stream()
                .map(GuestFeedbackJpaEntity::toDomain)
                .toList();
    }

    @Override
    public long countBySessionId(Long sessionId) {
        return guestFeedbackJpaRepository.countBySessionId(sessionId);
    }

    @Override
    public boolean existsBySessionIdAndDeviceId(Long sessionId, String deviceId) {
        return guestFeedbackJpaRepository.existsBySessionIdAndDeviceId(sessionId, deviceId);
    }
}
