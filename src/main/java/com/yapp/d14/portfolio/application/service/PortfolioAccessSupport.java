package com.yapp.d14.portfolio.application.service;

import com.yapp.d14.portfolio.application.port.out.PortfolioRepository;
import com.yapp.d14.portfolio.domain.Portfolio;
import com.yapp.d14.portfolio.exception.PortfolioErrorCode;
import com.yapp.d14.portfolio.exception.PortfolioException;
import lombok.experimental.UtilityClass;

import java.util.UUID;

@UtilityClass
class PortfolioAccessSupport {

    Portfolio requireOwned(PortfolioRepository portfolioRepository, UUID portfolioId, UUID userId) {
        return portfolioRepository.findById(portfolioId)
                .filter(p -> p.getUserId().equals(userId))
                .orElseThrow(() -> new PortfolioException(PortfolioErrorCode.PORTFOLIO_NOT_FOUND));
    }
}
