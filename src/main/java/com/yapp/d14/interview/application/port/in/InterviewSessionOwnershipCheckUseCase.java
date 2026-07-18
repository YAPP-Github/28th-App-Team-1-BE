package com.yapp.d14.interview.application.port.in;

import java.util.UUID;

public interface InterviewSessionOwnershipCheckUseCase {

    /** 세션이 존재하지 않거나 본인 소유가 아니면 예외를 던진다. */
    void requireOwned(UUID userId, Long sessionId);
}
