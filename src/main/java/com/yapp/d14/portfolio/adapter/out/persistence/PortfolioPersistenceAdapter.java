package com.yapp.d14.portfolio.adapter.out.persistence;

import com.yapp.d14.portfolio.adapter.out.persistence.entity.PortfolioJpaEntity;
import com.yapp.d14.portfolio.application.port.out.PortfolioRepository;
import com.yapp.d14.portfolio.domain.Portfolio;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

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
    public Optional<Portfolio> findById(UUID id) {
        return portfolioJpaRepository.findById(id).map(PortfolioJpaEntity::toDomain);
    }

    @Override
    public List<Portfolio> findAllByUserId(UUID userId) {
        return portfolioJpaRepository.findAllByUserId(userId).stream()
                .map(PortfolioJpaEntity::toDomain)
                .toList();
    }

    @Override
    public void deleteById(UUID id) {
        portfolioJpaRepository.deleteById(id);
    }
}
