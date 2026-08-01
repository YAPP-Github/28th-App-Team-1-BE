package com.yapp.d14.interview.application.port.out;

import com.yapp.d14.interview.domain.RedFlagType;
import com.yapp.d14.interview.domain.TestType;
import com.yapp.d14.interview.domain.TimeRange;

import java.util.List;

public record RedFlagVerdict(
        RedFlagType type,
        TestType affectedTestType,
        Integer capValue,
        boolean knockout,
        List<TimeRange> evidenceTimestamps,
        // 이 레드플래그의 근거가 된 턴 번호들(turnLevel 기준). CONTRADICTION처럼 특정 축에 매이지 않는
        // 레드플래그를 리포트 카드에 이어붙이는 연결고리로 쓴다. 근거 턴이 불명확하면 빈 리스트.
        List<Integer> relatedTurns,
        String rationale
) {
}
