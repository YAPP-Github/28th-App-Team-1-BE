package com.yapp.d14.feedback.application.port.out;

import com.yapp.d14.feedback.domain.GuestFeedback;

import java.util.List;

public interface GuestFeedbackRepository {

    GuestFeedback save(GuestFeedback guestFeedback);

    List<GuestFeedback> findAllBySessionId(Long sessionId);

    long countBySessionId(Long sessionId);

    boolean existsBySessionIdAndDeviceId(Long sessionId, String deviceId);
}
