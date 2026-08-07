package com.yapp.d14.portfolio.adapter.out.persistence;

import com.yapp.d14.portfolio.adapter.out.persistence.entity.PortfolioJpaEntity;
import com.yapp.d14.portfolio.domain.PortfolioStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface PortfolioJpaRepository extends JpaRepository<PortfolioJpaEntity, UUID> {

    @Query(value = "SELECT pg_advisory_xact_lock(hashtext(:lockKey)::bigint)", nativeQuery = true)
    void acquireRegistrationLock(@Param("lockKey") String lockKey);

    @Query(value = "SELECT pg_advisory_xact_lock(hashtext(:lockKey)::bigint)", nativeQuery = true)
    void acquirePortfolioLock(@Param("lockKey") String lockKey);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM PortfolioJpaEntity p WHERE p.id = :id")
    Optional<PortfolioJpaEntity> findByIdForUpdate(@Param("id") UUID id);

    List<PortfolioJpaEntity> findAllByUserIdAndDeletedFalseAndStatusIn(UUID userId, List<PortfolioStatus> statuses);

    Optional<PortfolioJpaEntity> findFirstByUserIdOrderByCreatedAtDesc(UUID userId);

    boolean existsByUserIdAndDeletedFalseAndStatus(UUID userId, PortfolioStatus status);

    boolean existsByUserIdAndStatus(UUID userId, PortfolioStatus status);

    boolean existsByUserIdAndReplacementTrueAndStatusAndUploadedAtGreaterThanEqual(
            UUID userId, PortfolioStatus status, LocalDateTime since
    );

    boolean existsByUserIdAndDeletedTrueAndStatusAndDeletedAtGreaterThanEqual(
            UUID userId, PortfolioStatus status, LocalDateTime since
    );
}
