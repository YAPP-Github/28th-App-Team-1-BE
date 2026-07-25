package com.yapp.d14.portfolio.application.service;

import com.yapp.d14.common.util.S3KeyGenerator;
import com.yapp.d14.portfolio.application.command.PortfolioRegisterCommand;
import com.yapp.d14.portfolio.application.port.out.PortfolioRepository;
import com.yapp.d14.portfolio.domain.Portfolio;
import com.yapp.d14.portfolio.exception.PortfolioErrorCode;
import com.yapp.d14.portfolio.exception.PortfolioException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Component
@RequiredArgsConstructor
class PortfolioRegistrationPersister {

    private final PortfolioRepository portfolioRepository;

    @Transactional
    Portfolio persist(PortfolioRegisterCommand command, int pageCount) {
        portfolioRepository.acquireRegistrationLock(command.userId());

        if (portfolioRepository.existsActiveByUserId(command.userId())) {
            throw new PortfolioException(PortfolioErrorCode.PORTFOLIO_ALREADY_EXISTS);
        }

        boolean replacement = portfolioRepository.existsAnyByUserId(command.userId());
        if (replacement && portfolioRepository.existsReplacementSince(command.userId(), PortfolioReplacementPolicy.currentMonthStart())) {
            throw new PortfolioException(PortfolioErrorCode.REPLACEMENT_LIMIT_EXCEEDED);
        }

        UUID portfolioId = UUID.randomUUID();
        String s3Key = S3KeyGenerator.portfolioKey(command.userId(), portfolioId);
        Portfolio portfolio = Portfolio.create(
                portfolioId,
                command.userId(),
                command.fileName(),
                command.fileContent().length,
                pageCount,
                s3Key,
                replacement
        );
        return portfolioRepository.save(portfolio);
    }
}
