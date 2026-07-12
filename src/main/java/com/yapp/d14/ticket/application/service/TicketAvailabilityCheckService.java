package com.yapp.d14.ticket.application.service;

import com.yapp.d14.ticket.application.port.in.TicketAvailabilityCheckUseCase;
import com.yapp.d14.ticket.application.port.out.TicketReservationRepository;
import com.yapp.d14.ticket.application.port.out.UserTicketRepository;
import com.yapp.d14.ticket.domain.TicketReservation;
import com.yapp.d14.ticket.domain.UserTicket;
import com.yapp.d14.ticket.exception.TicketErrorCode;
import com.yapp.d14.ticket.exception.TicketException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
class TicketAvailabilityCheckService implements TicketAvailabilityCheckUseCase {

    private static final Duration HOLD_TTL = Duration.ofMinutes(20);
    private static final String OUTCOME_HOLD_EXPIRED = "HOLD_EXPIRED";

    private final UserTicketRepository userTicketRepository;
    private final TicketReservationRepository ticketReservationRepository;

    @Override
    @Transactional
    public void checkAvailable(UUID userId) {
        cleanupExpiredHolds(userId);

        UserTicket userTicket = userTicketRepository.findByUserId(userId)
                .orElseGet(() -> userTicketRepository.save(UserTicket.create(userId)));

        if (!userTicket.hasRemaining()) {
            throw new TicketException(TicketErrorCode.NO_REMAINING_TICKET);
        }
    }

    private void cleanupExpiredHolds(UUID userId) {
        List<TicketReservation> expiredHolds = ticketReservationRepository.findExpiredHeld(
                userId, LocalDateTime.now().minus(HOLD_TTL)
        );

        for (TicketReservation reservation : expiredHolds) {
            reservation.release(OUTCOME_HOLD_EXPIRED);
            ticketReservationRepository.save(reservation);
            userTicketRepository.increment(userId);
        }
    }
}
