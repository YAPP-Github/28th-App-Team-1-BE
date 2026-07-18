package com.yapp.d14.interview.domain;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class InterviewVideoTest {

    @Test
    void 만료_시각이_지나면_만료된_것으로_판단한다() {
        InterviewVideo video = InterviewVideo.of(
                1L, 100L, LocalDateTime.now(), LocalDateTime.now().minusDays(1), false
        );

        assertThat(video.isExpired()).isTrue();
    }

    @Test
    void 삭제됐으면_만료_시각과_무관하게_만료된_것으로_판단한다() {
        InterviewVideo video = InterviewVideo.of(
                1L, 100L, LocalDateTime.now(), LocalDateTime.now().plusDays(1), true
        );

        assertThat(video.isExpired()).isTrue();
    }

    @Test
    void 삭제되지_않았고_만료_시각_이전이면_만료되지_않은_것으로_판단한다() {
        InterviewVideo video = InterviewVideo.of(
                1L, 100L, LocalDateTime.now(), LocalDateTime.now().plusDays(1), false
        );

        assertThat(video.isExpired()).isFalse();
    }
}
