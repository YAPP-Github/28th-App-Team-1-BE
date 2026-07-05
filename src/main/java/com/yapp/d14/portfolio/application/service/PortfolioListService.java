package com.yapp.d14.portfolio.application.service;

import com.yapp.d14.portfolio.application.port.in.PortfolioListUseCase;
import com.yapp.d14.portfolio.application.port.in.PortfolioSummary;
import com.yapp.d14.portfolio.application.port.out.PortfolioRepository;
import com.yapp.d14.portfolio.domain.Portfolio;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
class PortfolioListService implements PortfolioListUseCase {

    private final PortfolioRepository portfolioRepository;

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
