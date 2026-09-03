package com.yapp.d14.interview.adapter.out.metrics;

import com.yapp.d14.interview.application.port.out.ProbeSelectionRecorder;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class MicrometerProbeSelectionRecorderAdapter implements ProbeSelectionRecorder {

    private static final String SUMMARY_NAME = "probe.pool.size";
    // 풀 크기는 한 자릿수라 백분위 히스토그램(수십 버킷)은 과하다. 궁금한 경계만 버킷으로 둔다 —
    // le=1 비율이 곧 "고를 게 하나뿐이라 강제 선택이었던 비율"이다.
    private static final double[] BUCKETS = {1, 2, 3, 5, 10};

    private final MeterRegistry meterRegistry;

    @Override
    public void recordPoolSize(int poolSize) {
        DistributionSummary.builder(SUMMARY_NAME)
                .serviceLevelObjectives(BUCKETS)
                .register(meterRegistry)
                .record(poolSize);
    }
}
