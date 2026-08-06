package com.yapp.d14.interview.adapter.out.persistence;

import com.yapp.d14.interview.adapter.out.persistence.entity.InterviewSessionJpaEntity;
import com.yapp.d14.interview.domain.AbandonCause;
import com.yapp.d14.interview.domain.InterviewSessionStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface InterviewSessionJpaRepository extends JpaRepository<InterviewSessionJpaEntity, Long> {

    List<InterviewSessionJpaEntity> findAllByUserId(UUID userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM InterviewSessionJpaEntity s WHERE s.id = :id")
    Optional<InterviewSessionJpaEntity> findByIdForUpdate(@Param("id") Long id);

    boolean existsByPortfolioIdAndStatus(UUID portfolioId, InterviewSessionStatus status);

    @Query("SELECT s.id FROM InterviewSessionJpaEntity s "
            + "WHERE s.userId = :userId AND s.status IN :statuses "
            + "AND (:excludeSessionId IS NULL OR s.id <> :excludeSessionId) "
            + "ORDER BY s.createdAt DESC")
    List<Long> findRecentIdsByUserIdAndStatusIn(
            @Param("userId") UUID userId,
            @Param("statuses") List<InterviewSessionStatus> statuses,
            @Param("excludeSessionId") Long excludeSessionId,
            Pageable pageable
    );

    long countByUserIdAndAbandonCause(UUID userId, AbandonCause abandonCause);
}
