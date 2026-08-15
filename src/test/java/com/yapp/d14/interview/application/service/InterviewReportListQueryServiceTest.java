package com.yapp.d14.interview.application.service;

import com.yapp.d14.feedback.application.port.in.FeedbackShareExistsUseCase;
import com.yapp.d14.interview.application.port.in.result.InterviewReportListItem;
import com.yapp.d14.interview.application.port.out.InterviewSessionRepository;
import com.yapp.d14.interview.application.port.out.InterviewVideoRepository;
import com.yapp.d14.interview.application.port.out.ReportRepository;
import com.yapp.d14.interview.domain.InterviewSession;
import com.yapp.d14.interview.domain.InterviewSessionStatus;
import com.yapp.d14.interview.domain.InterviewVideo;
import com.yapp.d14.interview.domain.JobType;
import com.yapp.d14.interview.domain.Report;
import com.yapp.d14.interview.domain.ReportStatus;
import com.yapp.d14.portfolio.application.port.in.PortfolioActiveCheckUseCase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class InterviewReportListQueryServiceTest {

    @Mock
    private InterviewSessionRepository interviewSessionRepository;

    @Mock
    private ReportRepository reportRepository;

    @Mock
    private InterviewVideoRepository interviewVideoRepository;

    @Mock
    private PortfolioActiveCheckUseCase portfolioActiveCheckUseCase;

    @Mock
    private FeedbackShareExistsUseCase feedbackShareExistsUseCase;

    @InjectMocks
    private InterviewReportListQueryService service;

    private final UUID userId = UUID.randomUUID();

    private InterviewSession session(Long id, UUID portfolioId, LocalDateTime startedAt) {
        return InterviewSession.of(
                id, userId, portfolioId, "portfolio.pdf", JobType.BACKEND, 2, "https://jd.example.com", null, null,
                startedAt, InterviewSessionStatus.COMPLETED, startedAt, startedAt.plusMinutes(30), null,
                25, 20, 10, 20, 10, 15, 0, 10, null, null
        );
    }

    @Test
    void 레포트가_존재하는_세션만_최신순으로_반환한다() {
        InterviewSession older = session(1L, UUID.randomUUID(), LocalDateTime.now().minusDays(1));
        InterviewSession newer = session(2L, UUID.randomUUID(), LocalDateTime.now());
        InterviewSession noReport = session(3L, UUID.randomUUID(), LocalDateTime.now().minusHours(1));

        given(interviewSessionRepository.findAllByUserId(userId)).willReturn(List.of(older, newer, noReport));
        given(reportRepository.findBySessionId(1L))
                .willReturn(Optional.of(Report.of(10L, 1L, 80.0, null, "headline", null, ReportStatus.READY, older.getCreatedAt())));
        given(reportRepository.findBySessionId(2L))
                .willReturn(Optional.of(Report.of(11L, 2L, 90.0, null, "headline", null, ReportStatus.READY, newer.getCreatedAt())));
        given(reportRepository.findBySessionId(3L)).willReturn(Optional.empty());
        given(portfolioActiveCheckUseCase.isActive(org.mockito.ArgumentMatchers.any())).willReturn(true);
        given(feedbackShareExistsUseCase.existsForSession(org.mockito.ArgumentMatchers.anyLong())).willReturn(false);

        List<InterviewReportListItem> result = service.getReportList(userId);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).sessionId()).isEqualTo(2L);
        assertThat(result.get(1).sessionId()).isEqualTo(1L);
        assertThat(result.get(0).title()).isEqualTo("headline");
    }

    @Test
    void title에_report의_headline을_그대로_노출한다() {
        InterviewSession target = session(1L, null, LocalDateTime.now());

        given(interviewSessionRepository.findAllByUserId(userId)).willReturn(List.of(target));
        given(reportRepository.findBySessionId(1L))
                .willReturn(Optional.of(Report.of(10L, 1L, 80.0, null, "한 줄 요약입니다.", null, ReportStatus.READY, target.getCreatedAt())));
        given(feedbackShareExistsUseCase.existsForSession(1L)).willReturn(false);

        List<InterviewReportListItem> result = service.getReportList(userId);

        assertThat(result.get(0).title()).isEqualTo("한 줄 요약입니다.");
    }

    @Test
    void headline이_null이면_title도_null이다() {
        InterviewSession target = session(1L, null, LocalDateTime.now());

        given(interviewSessionRepository.findAllByUserId(userId)).willReturn(List.of(target));
        given(reportRepository.findBySessionId(1L))
                .willReturn(Optional.of(Report.of(10L, 1L, null, null, null, null, ReportStatus.FAILED, target.getCreatedAt())));

        List<InterviewReportListItem> result = service.getReportList(userId);

        assertThat(result.get(0).title()).isNull();
    }

    @Test
    void 포트폴리오가_삭제되었으면_portfolioDeleted가_true다() {
        UUID portfolioId = UUID.randomUUID();
        InterviewSession target = session(1L, portfolioId, LocalDateTime.now());

        given(interviewSessionRepository.findAllByUserId(userId)).willReturn(List.of(target));
        given(reportRepository.findBySessionId(1L))
                .willReturn(Optional.of(Report.of(10L, 1L, 80.0, null, "headline", null, ReportStatus.READY, target.getCreatedAt())));
        given(portfolioActiveCheckUseCase.isActive(portfolioId)).willReturn(false);
        given(feedbackShareExistsUseCase.existsForSession(1L)).willReturn(false);

        List<InterviewReportListItem> result = service.getReportList(userId);

        assertThat(result.get(0).portfolioDeleted()).isTrue();
    }

    @Test
    void 레포트가_READY이고_공유링크가_없으면_feedbackAvailable이_true다() {
        InterviewSession target = session(1L, null, LocalDateTime.now());

        given(interviewSessionRepository.findAllByUserId(userId)).willReturn(List.of(target));
        given(reportRepository.findBySessionId(1L))
                .willReturn(Optional.of(Report.of(10L, 1L, 80.0, null, "headline", null, ReportStatus.READY, target.getCreatedAt())));
        given(feedbackShareExistsUseCase.existsForSession(1L)).willReturn(false);

        List<InterviewReportListItem> result = service.getReportList(userId);

        assertThat(result.get(0).feedbackAvailable()).isTrue();
        assertThat(result.get(0).portfolioDeleted()).isFalse();
        verify(portfolioActiveCheckUseCase, never()).isActive(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void 레포트가_INSUFFICIENT_ANALYSIS여도_READY와_동일하게_feedbackAvailable이_true다() {
        InterviewSession target = session(1L, null, LocalDateTime.now());

        given(interviewSessionRepository.findAllByUserId(userId)).willReturn(List.of(target));
        given(reportRepository.findBySessionId(1L))
                .willReturn(Optional.of(Report.of(10L, 1L, null, null, "headline", null, ReportStatus.INSUFFICIENT_ANALYSIS, target.getCreatedAt())));
        given(feedbackShareExistsUseCase.existsForSession(1L)).willReturn(false);

        List<InterviewReportListItem> result = service.getReportList(userId);

        assertThat(result.get(0).reportStatus()).isEqualTo(ReportStatus.INSUFFICIENT_ANALYSIS);
        assertThat(result.get(0).feedbackAvailable()).isTrue();
    }

    @Test
    void 채점은_끝났어도_영상_합성이_timeout_전이면_목록에서도_GENERATING으로_보인다() {
        // 상세 조회와 동일한 규칙 — 목록만 READY로 보이고 상세는 GENERATING인 모순을 막는다(#155 리뷰).
        InterviewSession target = session(1L, null, LocalDateTime.now());

        given(interviewSessionRepository.findAllByUserId(userId)).willReturn(List.of(target));
        given(reportRepository.findBySessionId(1L)).willReturn(Optional.of(
                Report.of(10L, 1L, 80.0, null, "headline", null, ReportStatus.READY, LocalDateTime.now())));
        given(interviewVideoRepository.findBySessionId(1L)).willReturn(Optional.of(
                InterviewVideo.of(1L, 1L, LocalDateTime.now(), LocalDateTime.now().plusDays(3), false, true, false, null, null)));

        List<InterviewReportListItem> result = service.getReportList(userId);

        assertThat(result.get(0).reportStatus()).isEqualTo(ReportStatus.GENERATING);
        assertThat(result.get(0).feedbackAvailable()).isFalse();
        verify(feedbackShareExistsUseCase, never()).existsForSession(org.mockito.ArgumentMatchers.anyLong());
    }

    @Test
    void 영상_합성_timeout을_넘기면_목록에도_영상없이_원래_status가_노출된다() {
        InterviewSession target = session(1L, null, LocalDateTime.now());

        given(interviewSessionRepository.findAllByUserId(userId)).willReturn(List.of(target));
        given(reportRepository.findBySessionId(1L)).willReturn(Optional.of(
                Report.of(10L, 1L, 80.0, null, "headline", null, ReportStatus.READY, LocalDateTime.now().minusMinutes(10))));
        given(interviewVideoRepository.findBySessionId(1L)).willReturn(Optional.of(
                InterviewVideo.of(1L, 1L, LocalDateTime.now(), LocalDateTime.now().plusDays(3), false, false, false, null, null)));
        given(feedbackShareExistsUseCase.existsForSession(1L)).willReturn(false);

        List<InterviewReportListItem> result = service.getReportList(userId);

        assertThat(result.get(0).reportStatus()).isEqualTo(ReportStatus.READY);
        assertThat(result.get(0).feedbackAvailable()).isTrue();
    }

    @Test
    void 레포트가_생성실패면_feedbackAvailable이_false다() {
        InterviewSession target = session(1L, null, LocalDateTime.now());

        given(interviewSessionRepository.findAllByUserId(userId)).willReturn(List.of(target));
        given(reportRepository.findBySessionId(1L))
                .willReturn(Optional.of(Report.of(10L, 1L, null, null, null, null, ReportStatus.FAILED, target.getCreatedAt())));

        List<InterviewReportListItem> result = service.getReportList(userId);

        assertThat(result.get(0).reportStatus()).isEqualTo(ReportStatus.FAILED);
        assertThat(result.get(0).feedbackAvailable()).isFalse();
        verify(feedbackShareExistsUseCase, never()).existsForSession(org.mockito.ArgumentMatchers.anyLong());
    }
}
