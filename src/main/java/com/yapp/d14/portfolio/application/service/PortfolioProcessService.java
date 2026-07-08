package com.yapp.d14.portfolio.application.service;

import com.yapp.d14.portfolio.application.port.in.PortfolioProcessUseCase;
import com.yapp.d14.portfolio.application.port.out.PortfolioFileUploader;
import com.yapp.d14.portfolio.application.port.out.PortfolioRepository;
import com.yapp.d14.portfolio.domain.Portfolio;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
class PortfolioProcessService implements PortfolioProcessUseCase {

    private static final String CONTENT_TYPE = "application/pdf";

    private final PortfolioRepository portfolioRepository;
    private final PortfolioFileUploader portfolioFileUploader;

    @Override
    @Async("portfolioTaskExecutor")
    public void process(UUID userId, UUID portfolioId, byte[] fileContent) {
        log.info("portfolio async processing triggered: portfolioId={}", portfolioId);

        Portfolio portfolio = PortfolioAccessSupport.requireOwned(portfolioRepository, portfolioId, userId);

        try {
            portfolioFileUploader.upload(portfolio.getS3Key(), fileContent, CONTENT_TYPE);
        } catch (Exception e) {
            log.error("[PORTFOLIO PROCESS] S3 업로드 실패: portfolioId={}", portfolioId, e);
            portfolio.failSystem("파일 업로드에 실패했어요. 잠시 후 다시 시도해 주세요.");
            portfolioRepository.save(portfolio);
        }

        // TODO: Tika 파싱/검증, 임베딩은 후속 단계에서 구현
    }
}
