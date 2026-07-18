package com.yapp.d14.feedback.domain;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
public class FeedbackShare {

    private final Long id;
    private final Long sessionId;
    private final String token;
    private final List<AttitudeAxis> axes;
    private FeedbackShareStatus status;
    private final LocalDateTime createdAt;

    @Builder(access = AccessLevel.PRIVATE)
    private FeedbackShare(
            Long id,
            Long sessionId,
            String token,
            List<AttitudeAxis> axes,
            FeedbackShareStatus status,
            LocalDateTime createdAt
    ) {
        this.id = id;
        this.sessionId = sessionId;
        this.token = token;
        this.axes = axes;
        this.status = status;
        this.createdAt = createdAt;
    }

    public static FeedbackShare create(Long sessionId, String token, List<AttitudeAxis> axes) {
        return FeedbackShare.builder()
                .sessionId(sessionId)
                .token(token)
                .axes(axes)
                .status(FeedbackShareStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .build();
    }

    public static FeedbackShare of(
            Long id,
            Long sessionId,
            String token,
            List<AttitudeAxis> axes,
            FeedbackShareStatus status,
            LocalDateTime createdAt
    ) {
        return FeedbackShare.builder()
                .id(id)
                .sessionId(sessionId)
                .token(token)
                .axes(axes)
                .status(status)
                .createdAt(createdAt)
                .build();
    }

    public boolean isActive() {
        return status == FeedbackShareStatus.ACTIVE;
    }

    public void toPrivate() {
        this.status = FeedbackShareStatus.PRIVATE;
    }
}
