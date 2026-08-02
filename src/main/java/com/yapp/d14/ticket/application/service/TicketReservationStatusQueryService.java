package com.yapp.d14.ticket.application.service;

import com.yapp.d14.ticket.application.port.in.TicketReservationStatusQueryUseCase;
import com.yapp.d14.ticket.application.port.in.result.TicketReservationHoldStatusResult;
import com.yapp.d14.ticket.application.port.out.TicketReservationRepository;
import com.yapp.d14.ticket.domain.TicketReservationStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
class TicketReservationStatusQueryService implements TicketReservationStatusQueryUseCase {

    private final TicketReservationRepository ticketReservationRepository;

    @Override
    @Transactional(readOnly = true)
    public TicketReservationHoldStatusResult getHoldStatus(Long sessionId) {
        return ticketReservationRepository.findBySessionId(sessionId)
                .map(reservation -> new TicketReservationHoldStatusResult(
                        reservation.getStatus() == TicketReservationStatus.HELD,
                        reservation.getHeldAt()
                ))
                .orElseGet(() -> new TicketReservationHoldStatusResult(false, null));
    }
}
