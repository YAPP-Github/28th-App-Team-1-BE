package com.yapp.d14.ticket.application.service;

import com.yapp.d14.ticket.application.port.in.TicketHoldUseCase;
import com.yapp.d14.ticket.application.port.out.TicketReservationRepository;
import com.yapp.d14.ticket.application.port.out.UserTicketRepository;
import com.yapp.d14.ticket.domain.TicketReservation;
import com.yapp.d14.ticket.exception.TicketErrorCode;
import com.yapp.d14.ticket.exception.TicketException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
class TicketHoldService implements TicketHoldUseCase {

    private final UserTicketRepository userTicketRepository;
    private final TicketReservationRepository ticketReservationRepository;

    @Override
    @Transactional
    public void hold(UUID userId, Long sessionId) {
        int updatedRows = userTicketRepository.decrementIfAvailable(userId);
        if (updatedRows == 0) {
            throw new TicketException(TicketErrorCode.NO_REMAINING_TICKET);
        }

        ticketReservationRepository.save(TicketReservation.hold(userId, sessionId));
    }
}
