package com.yapp.d14.portfolio.adapter.out.persistence;

import com.yapp.d14.portfolio.adapter.out.persistence.entity.PortfolioJpaEntity;
import com.yapp.d14.portfolio.domain.PortfolioStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

interface PortfolioJpaRepository extends JpaRepository<PortfolioJpaEntity, UUID> {

    @Query(value = "SELECT pg_advisory_xact_lock(hashtext(:lockKey)::bigint)", nativeQuery = true)
    void acquireRegistrationLock(@Param("lockKey") String lockKey);

    List<PortfolioJpaEntity> findAllByUserIdAndDeletedFalse(UUID userId);

    boolean existsByUserIdAndDeletedFalse(UUID userId);

    boolean existsByUserId(UUID userId);

    boolean existsByUserIdAndReplacementTrueAndStatusAndCreatedAtGreaterThanEqual(
            UUID userId, PortfolioStatus status, LocalDateTime since
    );
}
