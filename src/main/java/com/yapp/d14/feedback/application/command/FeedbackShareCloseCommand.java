package com.yapp.d14.feedback.application.command;

import com.yapp.d14.feedback.domain.FeedbackShareStatus;
import com.yapp.d14.feedback.exception.FeedbackErrorCode;
import com.yapp.d14.feedback.exception.FeedbackException;

import java.util.UUID;

public record FeedbackShareCloseCommand(
        UUID userId,
        Long sessionId,
        FeedbackShareStatus targetStatus
) {

    public static FeedbackShareCloseCommand of(UUID userId, Long sessionId, String rawStatus) {
        FeedbackShareStatus status = parse(rawStatus);
        // 현재는 PRIVATE(되돌릴 수 없는 종료) 전환만 허용한다.
        if (status != FeedbackShareStatus.PRIVATE) {
            throw new FeedbackException(FeedbackErrorCode.INVALID_SHARE_STATUS);
        }
        return new FeedbackShareCloseCommand(userId, sessionId, status);
    }

    private static FeedbackShareStatus parse(String rawStatus) {
        try {
            return FeedbackShareStatus.valueOf(rawStatus);
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new FeedbackException(FeedbackErrorCode.INVALID_SHARE_STATUS);
        }
    }
}
