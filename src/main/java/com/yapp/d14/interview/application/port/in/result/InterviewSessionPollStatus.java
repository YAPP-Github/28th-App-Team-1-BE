package com.yapp.d14.interview.application.port.in.result;

import com.yapp.d14.interview.domain.InterviewSessionStatus;

public enum InterviewSessionPollStatus {

    PROCESSING,
    READY,
    FAILED;

    public static InterviewSessionPollStatus from(InterviewSessionStatus status) {
        return switch (status) {
            case PREPARING -> PROCESSING;
            case PRELOAD_FAILED -> FAILED;
            case IN_PROGRESS, COMPLETED, ABANDONED, INVALID -> READY;
        };
    }
}
