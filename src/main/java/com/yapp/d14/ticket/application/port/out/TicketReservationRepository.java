package com.yapp.d14.ticket.application.port.out;

import com.yapp.d14.ticket.domain.TicketReservation;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface TicketReservationRepository {

    TicketReservation save(TicketReservation ticketReservation);

    List<TicketReservation> findExpiredHeld(UUID userId, LocalDateTime heldBefore);
}
