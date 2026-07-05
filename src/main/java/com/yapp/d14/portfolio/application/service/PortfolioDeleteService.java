package com.yapp.d14.portfolio.application.service;

import com.yapp.d14.portfolio.application.port.in.PortfolioDeleteResult;
import com.yapp.d14.portfolio.application.port.in.PortfolioDeleteUseCase;
import com.yapp.d14.portfolio.application.port.out.PortfolioRepository;
import com.yapp.d14.portfolio.domain.Portfolio;
import com.yapp.d14.portfolio.exception.PortfolioErrorCode;
import com.yapp.d14.portfolio.exception.PortfolioException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
class PortfolioDeleteService implements PortfolioDeleteUseCase {

    private final PortfolioRepository portfolioRepository;

    @Override
    public PortfolioDeleteResult delete(UUID userId, UUID portfolioId) {
        Portfolio portfolio = portfolioRepository.findById(portfolioId)
                .filter(p -> p.getUserId().equals(userId))
                .orElseThrow(() -> new PortfolioException(PortfolioErrorCode.PORTFOLIO_NOT_FOUND));

        // TODO: S3에서 portfolio.getS3Key() 파일 삭제 (S3 연동 구현 예정)
        // TODO: pgvector에서 portfolioId 기준 청크 일괄 삭제 (임베딩 연동 구현 예정)

        portfolioRepository.deleteById(portfolio.getId());

        return new PortfolioDeleteResult(portfolio.getId(), LocalDateTime.now());
    }
}
