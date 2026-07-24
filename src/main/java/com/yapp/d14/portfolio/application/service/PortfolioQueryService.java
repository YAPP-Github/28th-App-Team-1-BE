package com.yapp.d14.portfolio.application.service;

import com.yapp.d14.portfolio.application.port.in.PortfolioListUseCase;
import com.yapp.d14.portfolio.application.port.in.result.PortfolioStatusResult;
import com.yapp.d14.portfolio.application.port.in.PortfolioStatusUseCase;
import com.yapp.d14.portfolio.application.port.in.result.PortfolioSummary;
import com.yapp.d14.portfolio.application.port.out.PortfolioRepository;
import com.yapp.d14.portfolio.domain.Portfolio;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
class PortfolioQueryService implements PortfolioStatusUseCase, PortfolioListUseCase {

    private final PortfolioRepository portfolioRepository;

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
        boolean blocked = portfolioRepository.existsReplacementSince(
                portfolio.getUserId(), PortfolioReplacementPolicy.currentMonthStart()
        );

        return new PortfolioSummary(
                portfolio.getId(),
                portfolio.getFileName(),
                portfolio.getFileSize(),
                portfolio.getPageCount(),
                portfolio.getStatus(),
                portfolio.getUploadedAt(),
                !blocked,
                blocked ? PortfolioReplacementPolicy.nextMonthStart() : null
        );
    }
}
