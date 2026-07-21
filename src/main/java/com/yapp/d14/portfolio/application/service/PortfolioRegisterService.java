package com.yapp.d14.portfolio.application.service;

import com.yapp.d14.common.util.S3KeyGenerator;
import com.yapp.d14.portfolio.application.command.PortfolioRegisterCommand;
import com.yapp.d14.portfolio.application.port.in.result.PortfolioRegisterResult;
import com.yapp.d14.portfolio.application.port.in.PortfolioProcessUseCase;
import com.yapp.d14.portfolio.application.port.in.PortfolioRegisterUseCase;
import com.yapp.d14.portfolio.application.port.out.PdfMetadataReader;
import com.yapp.d14.portfolio.application.port.out.PortfolioRepository;
import com.yapp.d14.portfolio.domain.Portfolio;
import com.yapp.d14.portfolio.exception.PortfolioErrorCode;
import com.yapp.d14.portfolio.exception.PortfolioException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.RejectedExecutionException;

@Slf4j
@Service
@RequiredArgsConstructor
class PortfolioRegisterService implements PortfolioRegisterUseCase {

    private final PortfolioRepository portfolioRepository;
    private final PdfMetadataReader pdfMetadataReader;
    private final PortfolioProcessUseCase portfolioProcessUseCase;

    @Override
    public PortfolioRegisterResult register(PortfolioRegisterCommand command) {
        if (portfolioRepository.existsActiveByUserId(command.userId())) {
            throw new PortfolioException(PortfolioErrorCode.PORTFOLIO_ALREADY_EXISTS);
        }

        boolean replacement = portfolioRepository.existsAnyByUserId(command.userId());
        if (replacement && portfolioRepository.existsReplacementSince(command.userId(), PortfolioReplacementPolicy.currentMonthStart())) {
            throw new PortfolioException(PortfolioErrorCode.REPLACEMENT_LIMIT_EXCEEDED);
        }

        int pageCount = pdfMetadataReader.countPages(command.fileContent());
        if (pageCount > PortfolioRegisterCommand.MAX_PAGE_COUNT) {
            throw new PortfolioException(PortfolioErrorCode.PAGE_COUNT_EXCEEDED);
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
        Portfolio saved = portfolioRepository.save(portfolio);

        try {
            portfolioProcessUseCase.process(command.userId(), saved.getId(), command.fileContent());
        } catch (RejectedExecutionException e) {
            log.error("[PORTFOLIO REGISTER] 비동기 처리 큐가 가득 찼어요: portfolioId={}", saved.getId(), e);
            saved.failSystem("현재 요청이 많아 처리가 지연되고 있어요. 잠시 후 다시 시도해 주세요.");
            saved = portfolioRepository.save(saved);
        }

        return new PortfolioRegisterResult(saved.getId(), saved.getStatus(), saved.getMessage());
    }
}
