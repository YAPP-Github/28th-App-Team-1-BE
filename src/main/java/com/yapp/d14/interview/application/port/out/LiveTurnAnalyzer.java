package com.yapp.d14.interview.application.port.out;

import com.yapp.d14.interview.domain.JobType;
import com.yapp.d14.interview.domain.QuestionCandidate;
import com.yapp.d14.interview.domain.TestType;

import java.util.List;
import java.util.UUID;

public interface LiveTurnAnalyzer {

    LiveTurnResult analyze(
            Long sessionId,
            UUID portfolioId,
            String lastQuestion,
            String lastAnswer,
            TestType currentAxis,
            JobType jobRole,
            List<PriorTurn> priorQa,
            List<QuestionCandidate> openProbesForAxis
    );
}
