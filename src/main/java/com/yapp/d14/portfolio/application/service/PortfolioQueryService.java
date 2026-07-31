package com.yapp.d14.portfolio.application.service;

import com.yapp.d14.interview.application.port.in.InterviewSessionInProgressCheckUseCase;
import com.yapp.d14.portfolio.application.port.in.PortfolioActiveCheckUseCase;
import com.yapp.d14.portfolio.application.port.in.PortfolioListUseCase;
import com.yapp.d14.portfolio.application.port.in.result.PortfolioStatusResult;
import com.yapp.d14.portfolio.application.port.in.PortfolioStatusUseCase;
import com.yapp.d14.portfolio.application.port.in.result.PortfolioSummary;
import com.yapp.d14.portfolio.application.port.out.PortfolioRepository;
import com.yapp.d14.portfolio.domain.Portfolio;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
class PortfolioQueryService implements PortfolioStatusUseCase, PortfolioListUseCase, PortfolioActiveCheckUseCase {

    private final PortfolioRepository portfolioRepository;
    private final InterviewSessionInProgressCheckUseCase interviewSessionInProgressCheckUseCase;

    @Override
    public boolean isActive(UUID portfolioId) {
        if (portfolioId == null) {
            return false;
        }
        return portfolioRepository.findById(portfolioId)
                .map(portfolio -> !portfolio.isDeleted())
                .orElse(false);
    }

    @Override
    @Transactional
    public PortfolioStatusResult getStatus(UUID userId, UUID portfolioId) {
        Portfolio portfolio = PortfolioAccessSupport.requireOwned(portfolioRepository, portfolioId, userId);
        timeoutIfStale(portfolio);

        return new PortfolioStatusResult(portfolio.getId(), portfolio.getStatus(), portfolio.getMessage(), portfolio.getFileName());
    }

    @Override
    @Transactional
    public List<PortfolioSummary> getList(UUID userId) {
        return portfolioRepository.findAllActiveByUserId(userId).stream()
                .peek(this::timeoutIfStale)
                .map(this::toSummary)
                .toList();
    }

    private void timeoutIfStale(Portfolio portfolio) {
        if (portfolio.failIfProcessingTimedOut()) {
            portfolioRepository.save(portfolio);
        }
    }

    private PortfolioSummary toSummary(Portfolio portfolio) {
        LocalDateTime monthStart = PortfolioReplacementPolicy.currentMonthStart();
        boolean replaceBlocked = portfolioRepository.existsReplacementSince(portfolio.getUserId(), monthStart);
        boolean deleteBlocked = portfolioRepository.existsDeletionSince(portfolio.getUserId(), monthStart);

        return new PortfolioSummary(
                portfolio.getId(),
                portfolio.getFileName(),
                portfolio.getFileSize(),
                portfolio.getPageCount(),
                portfolio.getStatus(),
                portfolio.getUploadedAt(),
                !replaceBlocked,
                replaceBlocked ? PortfolioReplacementPolicy.nextMonthStart() : null,
                !deleteBlocked,
                deleteBlocked ? PortfolioReplacementPolicy.nextMonthStart() : null,
                interviewSessionInProgressCheckUseCase.existsInProgress(portfolio.getId())
        );
    }
}
