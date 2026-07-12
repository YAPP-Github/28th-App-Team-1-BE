package com.yapp.d14.ticket.adapter.out.persistence;

import com.yapp.d14.ticket.adapter.out.persistence.entity.TicketReservationJpaEntity;
import com.yapp.d14.ticket.domain.TicketReservationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface TicketReservationJpaRepository extends JpaRepository<TicketReservationJpaEntity, Long> {

    List<TicketReservationJpaEntity> findAllByUserIdAndStatusAndHeldAtBefore(
            UUID userId, TicketReservationStatus status, LocalDateTime heldAtBefore
    );

    Optional<TicketReservationJpaEntity> findBySessionId(Long sessionId);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE TicketReservationJpaEntity r SET r.status = 'RELEASED', r.outcomeReason = :outcomeReason, " +
            "r.resolvedAt = CURRENT_TIMESTAMP WHERE r.id = :id AND r.status = 'HELD'")
    int releaseIfHeld(@Param("id") Long id, @Param("outcomeReason") String outcomeReason);
}
