package com.yapp.d14.feedback.application.command;

import com.yapp.d14.feedback.domain.AttitudeAxis;
import com.yapp.d14.feedback.exception.FeedbackErrorCode;
import com.yapp.d14.feedback.exception.FeedbackException;

import java.util.List;

public record GuestFeedbackSubmitCommand(
        String token,
        String deviceId,
        String nickname,
        List<Rating> ratings,
        String overallFeedback
) {

    public record Rating(AttitudeAxis axis, int level, String comment) {
    }

    public static GuestFeedbackSubmitCommand of(
            String token,
            String deviceId,
            String nickname,
            List<RawRating> rawRatings,
            String overallFeedback
    ) {
        if (deviceId == null || deviceId.isBlank()) {
            throw new FeedbackException(FeedbackErrorCode.MISSING_DEVICE_ID);
        }
        if (rawRatings == null || rawRatings.isEmpty()) {
            throw new FeedbackException(FeedbackErrorCode.INCOMPLETE_RATINGS);
        }
        List<Rating> ratings = rawRatings.stream()
                .map(GuestFeedbackSubmitCommand::parseRating)
                .toList();
        return new GuestFeedbackSubmitCommand(token, deviceId, nickname, ratings, overallFeedback);
    }

    private static Rating parseRating(RawRating raw) {
        if (raw.level() == null || raw.level() < 1 || raw.level() > 4) {
            throw new FeedbackException(FeedbackErrorCode.INVALID_RATING_LEVEL);
        }
        return new Rating(AttitudeAxis.parse(raw.axis()), raw.level(), raw.comment());
    }

    /** 웹 계층에서 넘어온 미검증 척도 입력. */
    public record RawRating(String axis, Integer level, String comment) {
    }
}
