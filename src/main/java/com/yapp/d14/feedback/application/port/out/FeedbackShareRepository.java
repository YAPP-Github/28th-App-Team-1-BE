package com.yapp.d14.feedback.application.port.out;

import com.yapp.d14.feedback.domain.FeedbackShare;

import java.util.Optional;

public interface FeedbackShareRepository {

    FeedbackShare save(FeedbackShare feedbackShare);

    Optional<FeedbackShare> findBySessionId(Long sessionId);

    Optional<FeedbackShare> findByToken(String token);
}
