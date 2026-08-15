package com.yapp.d14.interview.domain;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class ReportTest {

    private Report report(ReportStatus status, LocalDateTime createdAt) {
        return Report.of(1L, 100L, 80.0, null, "headline", HeadlineBranch.NORMAL, status, createdAt);
    }

    private InterviewVideo video(LocalDateTime baseAt, boolean composited) {
        return InterviewVideo.of(1L, 100L, baseAt, baseAt.plusDays(1), false, true, composited, null, null);
    }

    @Test
    void FAILED이면_영상과_무관하게_FAILED를_그대로_노출한다() {
        Report report = report(ReportStatus.FAILED, LocalDateTime.now());

        assertThat(report.effectiveStatus(video(LocalDateTime.now(), false))).isEqualTo(ReportStatus.FAILED);
        assertThat(report.effectiveStatus(null)).isEqualTo(ReportStatus.FAILED);
    }

    @Test
    void 영상이_없으면_원래_status를_그대로_노출한다() {
        Report report = report(ReportStatus.READY, LocalDateTime.now());

        assertThat(report.effectiveStatus(null)).isEqualTo(ReportStatus.READY);
    }

    @Test
    void 영상이_이미_합성됐으면_원래_status를_그대로_노출한다() {
        Report report = report(ReportStatus.READY, LocalDateTime.now());

        assertThat(report.effectiveStatus(video(LocalDateTime.now(), true))).isEqualTo(ReportStatus.READY);
    }

    @Test
    void 영상이_합성전이고_리포트_저장후_timeout_전이면_GENERATING이다() {
        Report report = report(ReportStatus.READY, LocalDateTime.now());
        // video.baseAt은 훨씬 이전(업로드가 채점보다 먼저 끝난 경우)이어도 기준은 report.createdAt이어야 한다.
        InterviewVideo video = video(LocalDateTime.now().minusMinutes(20), false);

        assertThat(report.effectiveStatus(video)).isEqualTo(ReportStatus.GENERATING);
    }

    @Test
    void 영상이_합성전이어도_리포트_저장후_timeout을_넘기면_원래_status를_노출한다() {
        Report report = report(ReportStatus.READY, LocalDateTime.now().minusMinutes(10));
        InterviewVideo video = video(LocalDateTime.now(), false);

        assertThat(report.effectiveStatus(video)).isEqualTo(ReportStatus.READY);
    }

    @Test
    void INSUFFICIENT_ANALYSIS도_READY와_동일하게_영상_대기_규칙을_적용한다() {
        Report report = report(ReportStatus.INSUFFICIENT_ANALYSIS, LocalDateTime.now());
        InterviewVideo video = video(LocalDateTime.now(), false);

        assertThat(report.effectiveStatus(video)).isEqualTo(ReportStatus.GENERATING);
    }
}
