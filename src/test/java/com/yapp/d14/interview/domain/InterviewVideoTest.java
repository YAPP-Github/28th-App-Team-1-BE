package com.yapp.d14.interview.domain;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class InterviewVideoTest {

    @Test
    void 만료_시각이_지나면_만료된_것으로_판단한다() {
        InterviewVideo video = InterviewVideo.of(
                1L, 100L, LocalDateTime.now(), LocalDateTime.now().minusDays(1), false, false, false, null, null
        );

        assertThat(video.isExpired()).isTrue();
    }

    @Test
    void 삭제됐으면_만료_시각과_무관하게_만료된_것으로_판단한다() {
        InterviewVideo video = InterviewVideo.of(
                1L, 100L, LocalDateTime.now(), LocalDateTime.now().plusDays(1), true, false, false, null, null
        );

        assertThat(video.isExpired()).isTrue();
    }

    @Test
    void 삭제되지_않았고_만료_시각_이전이면_만료되지_않은_것으로_판단한다() {
        InterviewVideo video = InterviewVideo.of(
                1L, 100L, LocalDateTime.now(), LocalDateTime.now().plusDays(1), false, false, false, null, null
        );

        assertThat(video.isExpired()).isFalse();
    }

    @Test
    void 소유자_단계형_만료_시각이_지났어도_30일_이내면_지인은_만료되지_않은_것으로_판단한다() {
        LocalDateTime baseAt = LocalDateTime.now().minusDays(10);
        InterviewVideo video = InterviewVideo.of(
                1L, 100L, baseAt, baseAt.plusHours(48), false, false, false, null, null
        );

        assertThat(video.isExpired()).isTrue();
        assertThat(video.isExpiredForGuest()).isFalse();
    }

    @Test
    void baseAt_기준_30일이_지나면_지인도_만료된_것으로_판단한다() {
        LocalDateTime baseAt = LocalDateTime.now().minusDays(31);
        InterviewVideo video = InterviewVideo.of(
                1L, 100L, baseAt, LocalDateTime.now().plusDays(1), false, false, false, null, null
        );

        assertThat(video.isExpiredForGuest()).isTrue();
    }

    @Test
    void 삭제됐으면_30일_이내여도_지인은_만료된_것으로_판단한다() {
        LocalDateTime baseAt = LocalDateTime.now();
        InterviewVideo video = InterviewVideo.of(
                1L, 100L, baseAt, baseAt.plusDays(1), true, false, false, null, null
        );

        assertThat(video.isExpiredForGuest()).isTrue();
    }

    @Test
    void 합성이_이미_끝났으면_timeout이_지났어도_overdue가_아니다() {
        InterviewVideo video = InterviewVideo.of(
                1L, 100L, LocalDateTime.now(), LocalDateTime.now().plusDays(1), false, true, true, null, null
        );

        assertThat(video.isCompositeOverdue(LocalDateTime.now().minusMinutes(10))).isFalse();
    }

    @Test
    void 합성_전이고_기준시각으로부터_timeout이_지나면_overdue다() {
        InterviewVideo video = InterviewVideo.of(
                1L, 100L, LocalDateTime.now(), LocalDateTime.now().plusDays(1), false, true, false, null, null
        );

        assertThat(video.isCompositeOverdue(LocalDateTime.now().minusMinutes(10))).isTrue();
    }

    @Test
    void 합성_전이어도_기준시각으로부터_timeout_전이면_overdue가_아니다() {
        InterviewVideo video = InterviewVideo.of(
                1L, 100L, LocalDateTime.now(), LocalDateTime.now().plusDays(1), false, false, false, null, null
        );

        assertThat(video.isCompositeOverdue(LocalDateTime.now())).isFalse();
    }

    @Test
    void overdue_판단은_baseAt이_아니라_호출자가_넘긴_기준시각을_따른다() {
        // baseAt이 오래전이어도(업로드가 채점보다 먼저 끝나 baseAt이 일찍 찍힌 경우) 기준시각이 최근이면 overdue가 아니다.
        InterviewVideo video = InterviewVideo.of(
                1L, 100L, LocalDateTime.now().minusMinutes(20), LocalDateTime.now().plusDays(1), false, true, false, null, null
        );

        assertThat(video.isCompositeOverdue(LocalDateTime.now())).isFalse();
    }

    @Test
    void 지인_하드캡_시각은_소유자_단계형_만료_시각과_무관하게_baseAt_30일이다() {
        LocalDateTime baseAt = LocalDateTime.now().minusDays(10);
        InterviewVideo video = InterviewVideo.of(
                1L, 100L, baseAt, baseAt.plusHours(48), false, false, false, null, null
        );

        assertThat(video.getGuestExpiresAt()).isEqualTo(baseAt.plusDays(30));
    }
}
