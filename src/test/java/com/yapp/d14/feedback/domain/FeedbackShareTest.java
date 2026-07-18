package com.yapp.d14.feedback.domain;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class FeedbackShareTest {

    @Test
    void create하면_ACTIVE_상태로_생성된다() {
        FeedbackShare share = FeedbackShare.create(1L, "token", List.of(AttitudeAxis.GAZE));

        assertThat(share.getStatus()).isEqualTo(FeedbackShareStatus.ACTIVE);
        assertThat(share.isActive()).isTrue();
    }

    @Test
    void toPrivate하면_PRIVATE_상태로_바뀐다() {
        FeedbackShare share = FeedbackShare.create(1L, "token", List.of(AttitudeAxis.GAZE));

        share.toPrivate();

        assertThat(share.getStatus()).isEqualTo(FeedbackShareStatus.PRIVATE);
        assertThat(share.isActive()).isFalse();
    }
}
