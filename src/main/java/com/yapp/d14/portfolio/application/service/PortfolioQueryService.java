package com.yapp.d14.portfolio.application.service;

import com.yapp.d14.portfolio.application.port.in.PortfolioListUseCase;
import com.yapp.d14.portfolio.application.port.in.PortfolioStatusResult;
import com.yapp.d14.portfolio.application.port.in.PortfolioStatusUseCase;
import com.yapp.d14.portfolio.application.port.in.PortfolioSummary;
import com.yapp.d14.portfolio.application.port.out.PortfolioRepository;
import com.yapp.d14.portfolio.domain.Portfolio;
import com.yapp.d14.portfolio.exception.PortfolioErrorCode;
import com.yapp.d14.portfolio.exception.PortfolioException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
class PortfolioQueryService implements PortfolioStatusUseCase, PortfolioListUseCase {

    private final PortfolioRepository portfolioRepository;

    @Override
    public PortfolioStatusResult getStatus(UUID userId, UUID portfolioId) {
        Portfolio portfolio = portfolioRepository.findById(portfolioId)
                .filter(p -> p.getUserId().equals(userId))
                .orElseThrow(() -> new PortfolioException(PortfolioErrorCode.PORTFOLIO_NOT_FOUND));

        return new PortfolioStatusResult(portfolio.getId(), portfolio.getStatus(), portfolio.getMessage());
    }

    @Override
    public List<PortfolioSummary> getList(UUID userId) {
        return portfolioRepository.findAllByUserId(userId).stream()
                .map(this::toSummary)
                .toList();
    }

    private PortfolioSummary toSummary(Portfolio portfolio) {
        return new PortfolioSummary(
                portfolio.getId(),
                portfolio.getFileName(),
                portfolio.getFileSize(),
                portfolio.getPageCount(),
                portfolio.getStatus(),
                portfolio.getUploadedAt()
        );
    }
}
