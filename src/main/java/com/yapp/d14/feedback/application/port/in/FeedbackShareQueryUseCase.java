package com.yapp.d14.feedback.application.port.in;

import com.yapp.d14.feedback.application.port.in.result.FeedbackShareStatusResult;

import java.util.UUID;

public interface FeedbackShareQueryUseCase {

    FeedbackShareStatusResult get(UUID userId, Long sessionId);
}
