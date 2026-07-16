package com.yapp.d14.interview.application.port.out;

import com.yapp.d14.interview.domain.QuestionCandidateStaleReason;

// run_live_turn이 모순·자진정정을 감지해 기존 캐물지점(probeId)을 stale 처리해야 할 때의 갱신 지시 1건
public record StaleProbeUpdate(
        Long probeId,
        QuestionCandidateStaleReason reason,
        String flagRef
) {
}
