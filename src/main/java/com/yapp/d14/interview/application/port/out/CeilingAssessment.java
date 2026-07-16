package com.yapp.d14.interview.application.port.out;

import com.yapp.d14.interview.domain.CeilingKind;

// run_live_turn의 천장 판별 결과. current_axis가 없는 첫 턴은 판별 대상이 아니라 kind=null로 반환됨
public record CeilingAssessment(
        boolean reached,
        CeilingKind kind,
        String reason
) {
}
