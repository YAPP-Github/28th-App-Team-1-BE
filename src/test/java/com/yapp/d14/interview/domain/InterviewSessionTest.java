package com.yapp.d14.interview.domain;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class InterviewSessionTest {

    private static InterviewSession create() {
        return InterviewSession.create(
                UUID.randomUUID(), UUID.randomUUID(), JobType.BACKEND, 3, null, null, null
        );
    }

    @Test
    void 생성_직후에는_STT_누적_카운트가_0이다() {
        InterviewSession session = create();

        assertThat(session.getSttFailedSegmentCount()).isZero();
        assertThat(session.getSttTotalSegmentCount()).isZero();
        assertThat(session.isSttFailureRateExceeded()).isFalse();
    }

    @Test
    void 세그먼트_기록이_누적된다() {
        InterviewSession session = create();

        session.recordSttSegments(1, 4);
        session.recordSttSegments(2, 4);

        assertThat(session.getSttFailedSegmentCount()).isEqualTo(3);
        assertThat(session.getSttTotalSegmentCount()).isEqualTo(8);
    }

    @Test
    void 누적_실패율이_30퍼센트_이하이면_초과로_판단하지_않는다() {
        InterviewSession session = create();

        session.recordSttSegments(3, 10);

        assertThat(session.isSttFailureRateExceeded()).isFalse();
    }

    @Test
    void 누적_실패율이_30퍼센트를_초과하면_초과로_판단한다() {
        InterviewSession session = create();

        session.recordSttSegments(4, 10);

        assertThat(session.isSttFailureRateExceeded()).isTrue();
    }

    @Test
    void markInvalid_호출_시_상태가_invalid로_바뀌고_종료시각이_기록된다() {
        InterviewSession session = create();

        session.markInvalid();

        assertThat(session.getStatus()).isEqualTo(InterviewSessionStatus.INVALID);
        assertThat(session.getEndedAt()).isNotNull();
    }
}
