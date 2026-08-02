package com.yapp.d14.interview.application.port.in;

import com.yapp.d14.interview.domain.AbandonCause;

public interface InterviewSessionAbandonIfInProgressUseCase {

    /**
     * sessionId로 세션을 찾아 IN_PROGRESS면 ABANDONED(cause)로 전환한다.
     * 세션이 없거나 이미 종료(COMPLETED 등 IN_PROGRESS가 아닌 상태)면 조용히 no-op.
     *
     * @return 실제로 전환이 일어났으면 true. 호출자는 이 값으로만 후속 처리(이용권 환급 등) 여부를 판단해야 한다 —
     *         세션이 이미 COMPLETED/ABANDONED인데 이용권만 보고서 생성 결과를 기다리며 HELD로 남아있는 경우와
     *         구분하기 위함이다.
     */
    boolean abandon(Long sessionId, AbandonCause cause);
}
