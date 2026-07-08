package com.yapp.d14.portfolio.application.service;

import com.yapp.d14.common.util.S3Directory;
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
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
class PortfolioRegisterService implements PortfolioRegisterUseCase {

    private static final String ORIGINAL_FILE_NAME = "original.pdf";

    private final PortfolioRepository portfolioRepository;
    private final PdfMetadataReader pdfMetadataReader;
    private final PortfolioProcessUseCase portfolioProcessUseCase;

    @Override
    public PortfolioRegisterResult register(PortfolioRegisterCommand command) {
        if (portfolioRepository.existsByUserId(command.userId())) {
            throw new PortfolioException(PortfolioErrorCode.PORTFOLIO_ALREADY_EXISTS);
        }

        int pageCount = pdfMetadataReader.countPages(command.fileContent());
        if (pageCount > PortfolioRegisterCommand.MAX_PAGE_COUNT) {
            throw new PortfolioException(PortfolioErrorCode.PAGE_COUNT_EXCEEDED);
        }

        UUID portfolioId = UUID.randomUUID();
        String s3Key = S3KeyGenerator.generate(S3Directory.PORTFOLIOS, command.userId(), portfolioId, ORIGINAL_FILE_NAME);
        Portfolio portfolio = Portfolio.create(
                portfolioId,
                command.userId(),
                command.fileName(),
                command.fileContent().length,
                pageCount,
                s3Key
        );
        Portfolio saved = portfolioRepository.save(portfolio);

        portfolioProcessUseCase.process(saved.getId(), command.fileContent());

        return new PortfolioRegisterResult(saved.getId(), saved.getStatus(), saved.getMessage());
    }
}
