package com.yapp.d14.interview.domain;

public enum ReportStatus {
    GENERATING,
    READY,
    INSUFFICIENT_ANALYSIS,
    FAILED;

    // 채점이 끝나 사용자에게 리포트를 정상 노출할 수 있는 상태. INSUFFICIENT_ANALYSIS도 READY와 동일하게
    // 취급한다(리포트 표시·지인 피드백 등 기능 차이 없음). 상태 값 자체는 모니터링용으로만 구분한다.
    public boolean isComplete() {
        return this == READY || this == INSUFFICIENT_ANALYSIS;
    }
}
