package com.yapp.d14.portfolio.adapter.out.persistence;

import com.yapp.d14.portfolio.adapter.out.persistence.entity.PortfolioJpaEntity;
import com.yapp.d14.portfolio.application.port.out.PortfolioRepository;
import com.yapp.d14.portfolio.domain.Portfolio;
import com.yapp.d14.portfolio.domain.PortfolioStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
class PortfolioPersistenceAdapter implements PortfolioRepository {

    private final PortfolioJpaRepository portfolioJpaRepository;

    @Override
    public Portfolio save(Portfolio portfolio) {
        return portfolioJpaRepository.save(PortfolioJpaEntity.from(portfolio)).toDomain();
    }

    @Override
    public void acquireRegistrationLock(UUID userId) {
        portfolioJpaRepository.acquireRegistrationLock(userId.toString());
    }

    @Override
    public boolean existsActiveByUserId(UUID userId) {
        return portfolioJpaRepository.existsByUserIdAndDeletedFalse(userId);
    }

    @Override
    public boolean existsAnyByUserId(UUID userId) {
        return portfolioJpaRepository.existsByUserId(userId);
    }

    @Override
    public boolean existsReplacementSince(UUID userId, LocalDateTime since) {
        return portfolioJpaRepository.existsByUserIdAndReplacementTrueAndStatusAndUploadedAtGreaterThanEqual(
                userId, PortfolioStatus.READY, since
        );
    }

    @Override
    public Optional<Portfolio> findById(UUID id) {
        return portfolioJpaRepository.findById(id).map(PortfolioJpaEntity::toDomain);
    }

    @Override
    public List<Portfolio> findAllActiveByUserId(UUID userId) {
        return portfolioJpaRepository.findAllByUserIdAndDeletedFalse(userId).stream()
                .map(PortfolioJpaEntity::toDomain)
                .toList();
    }
}
