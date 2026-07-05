package com.yapp.d14.portfolio.application.service;

import com.yapp.d14.common.util.S3Directory;
import com.yapp.d14.common.util.S3KeyGenerator;
import com.yapp.d14.portfolio.application.command.PortfolioRegisterCommand;
import com.yapp.d14.portfolio.application.port.in.PortfolioRegisterResult;
import com.yapp.d14.portfolio.application.port.in.PortfolioRegisterUseCase;
import com.yapp.d14.portfolio.application.port.out.PdfMetadataReader;
import com.yapp.d14.portfolio.application.port.out.PortfolioProcessor;
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

    private final PortfolioRepository portfolioRepository;
    private final PdfMetadataReader pdfMetadataReader;
    private final PortfolioProcessor portfolioProcessor;

    @Override
    public PortfolioRegisterResult register(PortfolioRegisterCommand command) {
        if (portfolioRepository.existsByUserId(command.userId())) {
            throw new PortfolioException(PortfolioErrorCode.PORTFOLIO_ALREADY_EXISTS);
        }

        PortfolioFileValidator.validateContentType(command.contentType());
        PortfolioFileValidator.validateFileSize(command.fileContent().length);
        PortfolioFileValidator.validatePageCount(command.declaredPageCount());

        int pageCount = pdfMetadataReader.countPages(command.fileContent());
        PortfolioFileValidator.validatePageCount(pageCount);

        UUID portfolioId = UUID.randomUUID();
        String s3Key = S3KeyGenerator.generate(S3Directory.PORTFOLIOS, command.userId(), portfolioId, "pdf");
        Portfolio portfolio = Portfolio.create(
                portfolioId,
                command.userId(),
                command.fileName(),
                command.fileContent().length,
                pageCount,
                s3Key
        );
        Portfolio saved = portfolioRepository.save(portfolio);

        portfolioProcessor.process(saved.getId());

        return new PortfolioRegisterResult(saved.getId(), saved.getStatus(), saved.getMessage());
    }
}
