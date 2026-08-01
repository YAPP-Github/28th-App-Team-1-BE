package com.yapp.d14.interview.application.port.in;

public interface InterviewSessionAbandonOnHoldExpiryUseCase {

    /** sessionId로 세션을 찾아 IN_PROGRESS면 ABANDONED(HOLD_EXPIRED)로 전환한다. 세션이 없거나 이미 종료면 조용히 no-op. */
    void abandonForHoldExpiry(Long sessionId);
}
