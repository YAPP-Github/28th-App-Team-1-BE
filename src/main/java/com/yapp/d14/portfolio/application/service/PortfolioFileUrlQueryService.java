package com.yapp.d14.portfolio.application.service;

import com.yapp.d14.portfolio.application.port.in.PortfolioFileUrlQueryUseCase;
import com.yapp.d14.portfolio.application.port.in.result.PortfolioFileUrlResult;
import com.yapp.d14.portfolio.application.port.out.PortfolioFileUploader;
import com.yapp.d14.portfolio.application.port.out.PortfolioRepository;
import com.yapp.d14.portfolio.domain.Portfolio;
import com.yapp.d14.portfolio.exception.PortfolioErrorCode;
import com.yapp.d14.portfolio.exception.PortfolioException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
class PortfolioFileUrlQueryService implements PortfolioFileUrlQueryUseCase {

    private final PortfolioRepository portfolioRepository;
    private final PortfolioFileUploader portfolioFileUploader;

    @Override
    public PortfolioFileUrlResult getFileUrl(UUID userId, UUID portfolioId) {
        Portfolio portfolio = PortfolioAccessSupport.requireOwned(portfolioRepository, portfolioId, userId);

        switch (portfolio.getStatus()) {
            case READY -> { }
            case PROCESSING -> throw new PortfolioException(PortfolioErrorCode.PORTFOLIO_PROCESSING);
            case FAILED_FILE, FAILED_SYSTEM, CANCELLED -> throw new PortfolioException(PortfolioErrorCode.PORTFOLIO_UPLOAD_FAILED);
        }

        String fileUrl = portfolioFileUploader.presignDownload(portfolio.getS3Key());
        return new PortfolioFileUrlResult(portfolio.getId(), fileUrl);
    }
}
