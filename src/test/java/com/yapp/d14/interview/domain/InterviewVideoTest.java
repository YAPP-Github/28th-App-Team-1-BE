package com.yapp.d14.interview.domain;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class InterviewVideoTest {

    @Test
    void 만료_시각이_지나면_만료된_것으로_판단한다() {
        InterviewVideo video = InterviewVideo.of(
                1L, 100L, LocalDateTime.now(), LocalDateTime.now().minusDays(1), false, false, false
        );

        assertThat(video.isExpired()).isTrue();
    }

    @Test
    void 삭제됐으면_만료_시각과_무관하게_만료된_것으로_판단한다() {
        InterviewVideo video = InterviewVideo.of(
                1L, 100L, LocalDateTime.now(), LocalDateTime.now().plusDays(1), true, false, false
        );

        assertThat(video.isExpired()).isTrue();
    }

    @Test
    void 삭제되지_않았고_만료_시각_이전이면_만료되지_않은_것으로_판단한다() {
        InterviewVideo video = InterviewVideo.of(
                1L, 100L, LocalDateTime.now(), LocalDateTime.now().plusDays(1), false, false, false
        );

        assertThat(video.isExpired()).isFalse();
    }

    @Test
    void 소유자_단계형_만료_시각이_지났어도_30일_이내면_지인은_만료되지_않은_것으로_판단한다() {
        LocalDateTime baseAt = LocalDateTime.now().minusDays(10);
        InterviewVideo video = InterviewVideo.of(
                1L, 100L, baseAt, baseAt.plusHours(48), false, false, false
        );

        assertThat(video.isExpired()).isTrue();
        assertThat(video.isExpiredForGuest()).isFalse();
    }

    @Test
    void baseAt_기준_30일이_지나면_지인도_만료된_것으로_판단한다() {
        LocalDateTime baseAt = LocalDateTime.now().minusDays(31);
        InterviewVideo video = InterviewVideo.of(
                1L, 100L, baseAt, LocalDateTime.now().plusDays(1), false, false, false
        );

        assertThat(video.isExpiredForGuest()).isTrue();
    }

    @Test
    void 삭제됐으면_30일_이내여도_지인은_만료된_것으로_판단한다() {
        LocalDateTime baseAt = LocalDateTime.now();
        InterviewVideo video = InterviewVideo.of(
                1L, 100L, baseAt, baseAt.plusDays(1), true, false, false
        );

        assertThat(video.isExpiredForGuest()).isTrue();
    }

    @Test
    void 지인_하드캡_시각은_소유자_단계형_만료_시각과_무관하게_baseAt_30일이다() {
        LocalDateTime baseAt = LocalDateTime.now().minusDays(10);
        InterviewVideo video = InterviewVideo.of(
                1L, 100L, baseAt, baseAt.plusHours(48), false, false, false
        );

        assertThat(video.getGuestExpiresAt()).isEqualTo(baseAt.plusDays(30));
    }
}
