package com.yapp.d14.interview.application.port.out;

import com.yapp.d14.interview.domain.ResolutionLevel;
import com.yapp.d14.interview.domain.TestType;

import java.util.List;

public record HeadlineContext(
        boolean severeRedFlagPresent,
        List<AxisTopic> axisTopics,
        int coveredCoreAxisCount,
        int totalCoreAxisCount
) {

    // 다뤄야 할 핵심 축(CORE) 중 일부만 다룬 채로 면접이 짧게 끝났는지 여부.
    // true면 다뤄진 주제 하나가 잘 진행됐더라도 면접 전체가 완결된 것처럼 쓰면 안 된다는 신호.
    public boolean coverageIncomplete() {
        return totalCoreAxisCount > 0 && coveredCoreAxisCount < totalCoreAxisCount;
    }

    public record AxisTopic(
            TestType testType,
            String scoringRationale,
            ResolutionLevel resolutionLevel
    ) {
    }
}
