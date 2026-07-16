package com.yapp.d14.ticket.application.port.out;

import com.yapp.d14.ticket.domain.TicketReservation;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TicketReservationRepository {

    TicketReservation save(TicketReservation ticketReservation);

    List<TicketReservation> findExpiredHeld(UUID userId, LocalDateTime heldBefore);

    Optional<TicketReservation> findBySessionId(Long sessionId);

    int releaseIfHeld(Long id, String outcomeReason);

    int commitIfHeld(Long id, String outcomeReason);
}
