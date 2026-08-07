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

    // 반환값은 전환 이후의 현재 상태다. 호출부(PortfolioQueryService)는 넘긴 객체 대신 이 값을 응답에 쓴다 —
    // 락 안에서 다시 읽은 쪽이 권위 있는 상태이고, 넘긴 객체는 조회 시점의 스냅샷이라 이미 낡았을 수 있다.
    Portfolio failAndCleanup(Portfolio portfolio) {
        // 폴링마다 행을 잠그지 않도록 스냅샷으로 먼저 걸러낸다.
        if (!portfolio.isProcessingTimedOut()) {
            return portfolio;
        }

        // PortfolioCompletionPersister가 같은 락으로 완료 전환을 하므로, 둘 중 먼저 잡은 쪽만 반영된다.
        Portfolio current = portfolioRepository.findByIdForUpdate(portfolio.getId()).orElse(null);
        if (current == null) {
            return portfolio;
        }
        if (!current.failIfProcessingTimedOut()) {
            return current;
        }
        log.warn("[PORTFOLIO TIMEOUT] 처리 시간 초과로 실패 처리하고 남은 리소스를 정리함: portfolioId={}", current.getId());

        // pgvector는 같은 데이터소스라 상태 갱신과 한 트랜잭션에서 처리하고, S3만 커밋 이후로 미룬다
        // (PortfolioDeleteService와 동일 원칙 — 롤백됐는데 파일만 지워지는 불일치 방지).
        portfolioEmbeddingStore.deleteByPortfolioId(current.getId());
        portfolioRepository.save(current);
        AfterCommitExecutor.runAfterCommit(() -> portfolioFileUploader.delete(current.getS3Key()));
        return current;
    }
}
