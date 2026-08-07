package com.yapp.d14.portfolio.application.service;

import com.yapp.d14.common.util.AfterCommitExecutor;
import com.yapp.d14.portfolio.application.port.out.PortfolioEmbeddingStore;
import com.yapp.d14.portfolio.application.port.out.PortfolioFileUploader;
import com.yapp.d14.portfolio.application.port.out.PortfolioRepository;
import com.yapp.d14.portfolio.domain.Portfolio;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
class PortfolioProcessingTimeoutHandler {

    private final PortfolioRepository portfolioRepository;
    private final PortfolioEmbeddingStore portfolioEmbeddingStore;
    private final PortfolioFileUploader portfolioFileUploader;

    boolean failAndCleanup(Portfolio portfolio) {
        if (!portfolio.failIfProcessingTimedOut()) {
            return false;
        }
        log.warn("[PORTFOLIO TIMEOUT] 처리 시간 초과로 실패 처리하고 남은 리소스를 정리함: portfolioId={}", portfolio.getId());

        portfolioEmbeddingStore.deleteByPortfolioId(portfolio.getId());
        portfolioRepository.save(portfolio);
        AfterCommitExecutor.runAfterCommit(() -> portfolioFileUploader.delete(portfolio.getS3Key()));
        return true;
    }
}
