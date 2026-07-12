package com.yapp.d14.ticket.adapter.out.persistence;

import com.yapp.d14.ticket.adapter.out.persistence.entity.TicketReservationJpaEntity;
import com.yapp.d14.ticket.application.port.out.TicketReservationRepository;
import com.yapp.d14.ticket.domain.TicketReservation;
import com.yapp.d14.ticket.domain.TicketReservationStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
class TicketReservationPersistenceAdapter implements TicketReservationRepository {

    private final TicketReservationJpaRepository ticketReservationJpaRepository;

    @Override
    public TicketReservation save(TicketReservation ticketReservation) {
        return ticketReservationJpaRepository.save(TicketReservationJpaEntity.from(ticketReservation)).toDomain();
    }

    @Override
    public List<TicketReservation> findExpiredHeld(UUID userId, LocalDateTime heldBefore) {
        return ticketReservationJpaRepository.findAllByUserIdAndStatusAndHeldAtBefore(
                        userId, TicketReservationStatus.HELD, heldBefore
                ).stream()
                .map(TicketReservationJpaEntity::toDomain)
                .toList();
    }

    @Override
    public Optional<TicketReservation> findBySessionId(Long sessionId) {
        return ticketReservationJpaRepository.findBySessionId(sessionId).map(TicketReservationJpaEntity::toDomain);
    }
}
