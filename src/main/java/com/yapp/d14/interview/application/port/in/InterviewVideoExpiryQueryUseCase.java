package com.yapp.d14.interview.application.port.in;

import com.yapp.d14.interview.application.port.in.result.InterviewVideoExpiryResult;

import java.util.UUID;

public interface InterviewVideoExpiryQueryUseCase {

    /** 프론트가 카운트다운 표시를 위해 주기적으로 폴링한다. 본인 소유 세션이 아니면 예외를 던진다. */
    InterviewVideoExpiryResult getExpiry(UUID userId, Long sessionId);
}
