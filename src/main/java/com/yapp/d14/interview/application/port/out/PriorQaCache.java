package com.yapp.d14.interview.application.port.out;

import com.yapp.d14.interview.domain.TestType;

import java.util.List;

// run_live_turn의 prior_qa 파라미터/get_prior_qa tool이 공유하는 axis별 이전 턴 이력 캐시 (3단계 4-1장).
public interface PriorQaCache {

    List<PriorTurn> get(Long sessionId, TestType axis);

    void append(Long sessionId, TestType axis, PriorTurn turn);

    // 세션 종료(정상 종료/STT_RESET) 시 해당 세션의 모든 axis 캐시를 정리한다.
    void clear(Long sessionId);
}
