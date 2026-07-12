package com.yapp.d14.ticket.adapter.out.persistence;

import com.yapp.d14.ticket.adapter.out.persistence.entity.TicketReservationJpaEntity;
import com.yapp.d14.ticket.domain.TicketReservationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface TicketReservationJpaRepository extends JpaRepository<TicketReservationJpaEntity, Long> {

    List<TicketReservationJpaEntity> findAllByUserIdAndStatusAndHeldAtBefore(
            UUID userId, TicketReservationStatus status, LocalDateTime heldAtBefore
    );

    Optional<TicketReservationJpaEntity> findBySessionId(Long sessionId);
}
