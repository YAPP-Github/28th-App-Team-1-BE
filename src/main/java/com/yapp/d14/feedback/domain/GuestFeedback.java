package com.yapp.d14.feedback.domain;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
public class GuestFeedback {

    private final Long id;
    private final Long sessionId;
    private final String nickname;
    private final String deviceId;
    private final List<AttitudeRating> ratings;
    private final String overallFeedback;
    private final LocalDateTime submittedAt;

    @Builder(access = AccessLevel.PRIVATE)
    private GuestFeedback(
            Long id,
            Long sessionId,
            String nickname,
            String deviceId,
            List<AttitudeRating> ratings,
            String overallFeedback,
            LocalDateTime submittedAt
    ) {
        this.id = id;
        this.sessionId = sessionId;
        this.nickname = nickname;
        this.deviceId = deviceId;
        this.ratings = ratings;
        this.overallFeedback = overallFeedback;
        this.submittedAt = submittedAt;
    }

    public static GuestFeedback create(
            Long sessionId,
            String nickname,
            String deviceId,
            List<AttitudeRating> ratings,
            String overallFeedback
    ) {
        return GuestFeedback.builder()
                .sessionId(sessionId)
                .nickname(nickname)
                .deviceId(deviceId)
                .ratings(ratings)
                .overallFeedback(overallFeedback)
                .submittedAt(LocalDateTime.now())
                .build();
    }

    public static GuestFeedback of(
            Long id,
            Long sessionId,
            String nickname,
            String deviceId,
            List<AttitudeRating> ratings,
            String overallFeedback,
            LocalDateTime submittedAt
    ) {
        return GuestFeedback.builder()
                .id(id)
                .sessionId(sessionId)
                .nickname(nickname)
                .deviceId(deviceId)
                .ratings(ratings)
                .overallFeedback(overallFeedback)
                .submittedAt(submittedAt)
                .build();
    }
}
