package com.yapp.d14.feedback.application.command;

import com.yapp.d14.feedback.domain.AttitudeAxis;
import com.yapp.d14.feedback.exception.FeedbackErrorCode;
import com.yapp.d14.feedback.exception.FeedbackException;

import java.util.List;
import java.util.UUID;

public record FeedbackShareCreateCommand(
        UUID userId,
        Long sessionId,
        List<AttitudeAxis> axes
) {

    public static FeedbackShareCreateCommand of(UUID userId, Long sessionId, List<String> rawAxes) {
        List<AttitudeAxis> axes = parseAxes(rawAxes);
        return new FeedbackShareCreateCommand(userId, sessionId, axes);
    }

    private static List<AttitudeAxis> parseAxes(List<String> rawAxes) {
        if (rawAxes == null || rawAxes.isEmpty()) {
            throw new FeedbackException(FeedbackErrorCode.EMPTY_ATTITUDE_AXES);
        }
        List<AttitudeAxis> axes = rawAxes.stream()
                .map(AttitudeAxis::parse)
                .distinct()
                .toList();
        if (axes.size() > AttitudeAxis.MAX_AXES) {
            throw new FeedbackException(FeedbackErrorCode.TOO_MANY_ATTITUDE_AXES);
        }
        return axes;
    }
}
