package com.yapp.d14.interview.application.port.out;

import java.util.List;

// run_live_turn 1회 호출의 전체 반환값 (새 캐물지점, 천장 판별, 모순 갱신 지시)
public record LiveTurnResult(
        List<ProbeCandidateDraft> newProbes,
        CeilingAssessment ceiling,
        List<StaleProbeUpdate> staleUpdates
) {
}
