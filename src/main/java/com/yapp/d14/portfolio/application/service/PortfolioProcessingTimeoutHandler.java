package com.yapp.d14.portfolio.application.service;

import com.yapp.d14.common.util.AfterCommitExecutor;
import com.yapp.d14.portfolio.application.port.out.PortfolioEmbeddingStore;
import com.yapp.d14.portfolio.application.port.out.PortfolioFileUploader;
import com.yapp.d14.portfolio.application.port.out.PortfolioRepository;
import com.yapp.d14.portfolio.domain.Portfolio;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

// 워커 스레드가 죽거나 앱이 재시작되면 PortfolioProcessService의 정리 코드가 실행되지 않는다.
// 폴링 시점의 타임아웃 감지가 그때 남은 S3 원본·임베딩을 회수하는 유일한 지점이다.
@Slf4j
@Component
@RequiredArgsConstructor
class PortfolioProcessingTimeoutHandler {

    private final PortfolioRepository portfolioRepository;
    private final PortfolioEmbeddingStore portfolioEmbeddingStore;
    private final PortfolioFileUploader portfolioFileUploader;

    // 호출부(PortfolioQueryService)의 트랜잭션에 참여한다 — 여기서 새 트랜잭션을 열면
    // runAfterCommit이 바깥 커밋보다 먼저 실행돼 롤백 시 파일만 지워진다.
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
