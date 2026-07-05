package com.yapp.d14.portfolio.adapter.out.persistence;

import com.yapp.d14.portfolio.adapter.out.persistence.entity.PortfolioJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

interface PortfolioJpaRepository extends JpaRepository<PortfolioJpaEntity, UUID> {

    List<PortfolioJpaEntity> findAllByUserId(UUID userId);
}
