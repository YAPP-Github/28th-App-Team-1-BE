package com.yapp.d14.interview.application.port.out;

import com.yapp.d14.interview.domain.TestType;

// run_live_turn의 모순 감지용 이전 턴 이력 1건 (같은 axis로 캔 과거 질문·답변)
public record PriorTurn(
        int turnIndex,
        String question,
        String answer,
        TestType axis
) {
}
