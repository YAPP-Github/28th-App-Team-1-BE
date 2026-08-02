package com.yapp.d14.interview.application.port.in;

import com.yapp.d14.interview.domain.AbandonCause;

public interface InterviewSessionAbandonIfInProgressUseCase {

    /** sessionId로 세션을 찾아 IN_PROGRESS면 ABANDONED(cause)로 전환한다. 세션이 없거나 이미 종료면 조용히 no-op. */
    void abandon(Long sessionId, AbandonCause cause);
}
