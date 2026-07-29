package com.yapp.d14.interview.application.service;

import com.yapp.d14.feedback.application.port.in.FeedbackShareExistsUseCase;
import com.yapp.d14.interview.application.port.in.InterviewReportListQueryUseCase;
import com.yapp.d14.interview.application.port.in.result.InterviewReportListItem;
import com.yapp.d14.interview.application.port.out.InterviewSessionRepository;
import com.yapp.d14.interview.application.port.out.ReportRepository;
import com.yapp.d14.interview.domain.InterviewSession;
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
        boolean feedbackAvailable = report.getStatus() == ReportStatus.READY
                && !feedbackShareExistsUseCase.existsForSession(session.getId());

        return new InterviewReportListItem(
                session.getId(),
                session.getSnapshotJobType(),
                session.getSnapshotYearsOfExperience(),
                interviewedAt(session),
                session.getPortfolioFilename(),
                portfolioDeleted,
                session.getJdUrl(),
                report.getStatus(),
                feedbackAvailable
        );
    }

    private LocalDateTime interviewedAt(InterviewSession session) {
        return Optional.ofNullable(session.getStartedAt()).orElse(session.getCreatedAt());
    }
}
