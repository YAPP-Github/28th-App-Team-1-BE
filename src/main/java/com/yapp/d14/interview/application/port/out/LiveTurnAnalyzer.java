package com.yapp.d14.interview.application.port.out;

import com.yapp.d14.interview.domain.JobType;
import com.yapp.d14.interview.domain.TestType;

import java.util.List;

// 매 턴 답변에서 캐물지점을 추출하고(+ 천장 판별·모순 감지는 추후 확장) 결과를 반환하는 run_live_turn 포트
public interface LiveTurnAnalyzer {

    LiveTurnResult analyze(
            Long sessionId,
            String lastQuestion,
            String lastAnswer,
            TestType currentAxis,
            JobType jobRole,
            List<PriorTurn> priorQa
    );
}
