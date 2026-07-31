package com.yapp.d14.portfolio.application.service;

import com.yapp.d14.common.util.AfterCommitExecutor;
import com.yapp.d14.interview.application.port.in.InterviewSessionInProgressCheckUseCase;
import com.yapp.d14.portfolio.application.port.in.result.PortfolioDeleteResult;
import com.yapp.d14.portfolio.application.port.in.PortfolioDeleteUseCase;
import com.yapp.d14.portfolio.application.port.out.PortfolioEmbeddingStore;
import com.yapp.d14.portfolio.application.port.out.PortfolioFileUploader;
import com.yapp.d14.portfolio.application.port.out.PortfolioRepository;
import com.yapp.d14.portfolio.domain.Portfolio;
import com.yapp.d14.portfolio.exception.PortfolioErrorCode;
import com.yapp.d14.portfolio.exception.PortfolioException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
class PortfolioDeleteService implements PortfolioDeleteUseCase {

    private final PortfolioRepository portfolioRepository;
    private final PortfolioFileUploader portfolioFileUploader;
    private final PortfolioEmbeddingStore portfolioEmbeddingStore;
    private final InterviewSessionInProgressCheckUseCase interviewSessionInProgressCheckUseCase;

    @Override
    @Transactional
    public PortfolioDeleteResult delete(UUID userId, UUID portfolioId) {
        // 세션 생성 쪽(InterviewSessionPersister)이 같은 키로 이 락을 잡으므로, 둘 중 먼저 잡은 트랜잭션이
        // 끝날 때까지 나머지가 대기한다 — "확인 직후 삭제된 포트폴리오를 참조하는 세션이 생성"되는 경합을 막는다.
        portfolioRepository.acquirePortfolioLock(portfolioId);
        Portfolio portfolio = PortfolioAccessSupport.requireOwned(portfolioRepository, portfolioId, userId);

        if (interviewSessionInProgressCheckUseCase.existsInProgress(portfolioId)) {
            throw new PortfolioException(PortfolioErrorCode.PORTFOLIO_DELETE_BLOCKED_BY_INTERVIEW);
        }
        if (portfolioRepository.existsDeletionSince(userId, PortfolioReplacementPolicy.currentMonthStart())) {
            throw new PortfolioException(PortfolioErrorCode.DELETE_LIMIT_EXCEEDED);
        }

        // pgvector는 같은 PostgreSQL 데이터소스를 쓰므로 소프트 삭제(save)와 같은 트랜잭션 안에서 처리한다.
        // 실패하면 함께 롤백되어, 포트폴리오는 삭제 처리됐는데 임베딩만 고아로 남는 상황을 막는다.
        portfolioEmbeddingStore.deleteByPortfolioId(portfolio.getId());
        portfolio.softDelete();
        portfolioRepository.save(portfolio);
        AfterCommitExecutor.runAfterCommit(() -> portfolioFileUploader.delete(portfolio.getS3Key()));

        return new PortfolioDeleteResult(portfolio.getId(), LocalDateTime.now());
    }
}
