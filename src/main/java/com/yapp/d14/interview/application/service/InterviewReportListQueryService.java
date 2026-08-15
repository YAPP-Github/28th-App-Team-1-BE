package com.yapp.d14.interview.application.service;

import com.yapp.d14.feedback.application.port.in.FeedbackShareExistsUseCase;
import com.yapp.d14.interview.application.port.in.InterviewReportListQueryUseCase;
import com.yapp.d14.interview.application.port.in.result.InterviewReportListItem;
import com.yapp.d14.interview.application.port.out.InterviewSessionRepository;
import com.yapp.d14.interview.application.port.out.InterviewVideoRepository;
import com.yapp.d14.interview.application.port.out.ReportRepository;
import com.yapp.d14.interview.domain.InterviewSession;
import com.yapp.d14.interview.domain.InterviewVideo;
import com.yapp.d14.interview.domain.Report;
import com.yapp.d14.interview.domain.ReportStatus;
import com.yapp.d14.portfolio.application.port.in.PortfolioActiveCheckUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
class InterviewReportListQueryService implements InterviewReportListQueryUseCase {

    private final InterviewSessionRepository interviewSessionRepository;
    private final ReportRepository reportRepository;
    private final InterviewVideoRepository interviewVideoRepository;
    private final PortfolioActiveCheckUseCase portfolioActiveCheckUseCase;
    private final FeedbackShareExistsUseCase feedbackShareExistsUseCase;

    @Override
    public List<InterviewReportListItem> getReportList(UUID userId) {
        return interviewSessionRepository.findAllByUserId(userId).stream()
                .flatMap(session -> reportRepository.findBySessionId(session.getId())
                        .map(report -> toItem(session, report))
                        .stream())
                .sorted(Comparator.comparing(InterviewReportListItem::interviewedAt).reversed())
                .toList();
    }

    private InterviewReportListItem toItem(InterviewSession session, Report report) {
        UUID portfolioId = session.getPortfolioId();
        boolean portfolioDeleted = portfolioId != null && !portfolioActiveCheckUseCase.isActive(portfolioId);
        // 상세 조회(InterviewReportQueryService)와 동일한 기준으로 가린다 — 목록만 READY로 보이고
        // 상세는 아직 GENERATING인 모순을 막는다(#155 리뷰). feedbackAvailable도 이 기준을 따른다.
        InterviewVideo video = interviewVideoRepository.findBySessionId(session.getId()).orElse(null);
        ReportStatus effectiveStatus = report.effectiveStatus(video);
        boolean feedbackAvailable = effectiveStatus.isComplete()
                && !feedbackShareExistsUseCase.existsForSession(session.getId());

        return new InterviewReportListItem(
                session.getId(),
                session.getSnapshotJobType(),
                session.getSnapshotYearsOfExperience(),
                interviewedAt(session),
                session.getPortfolioFilename(),
                portfolioDeleted,
                session.getJdUrl(),
                effectiveStatus,
                feedbackAvailable,
                report.getHeadline()
        );
    }

    private LocalDateTime interviewedAt(InterviewSession session) {
        return Optional.ofNullable(session.getStartedAt()).orElse(session.getCreatedAt());
    }
}
