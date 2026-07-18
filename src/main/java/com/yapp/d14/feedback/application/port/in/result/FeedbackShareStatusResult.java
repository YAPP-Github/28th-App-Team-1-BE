package com.yapp.d14.feedback.application.port.in.result;

import com.yapp.d14.feedback.domain.AttitudeAxis;
import com.yapp.d14.feedback.domain.FeedbackShareStatus;

import java.time.LocalDateTime;
import java.util.List;

public record FeedbackShareStatusResult(
        String token,
        FeedbackShareStatus status,
        List<AttitudeAxis> axes,
        int submittedCount,
        LocalDateTime videoExpiresAt,
        LocalDateTime requestedAt
) {
}
