package com.yapp.d14.ticket.application.service;

import com.yapp.d14.ticket.application.port.in.TicketReleaseUseCase;
import com.yapp.d14.ticket.application.port.out.TicketReservationRepository;
import com.yapp.d14.ticket.application.port.out.UserTicketRepository;
import com.yapp.d14.ticket.domain.TicketReservation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
class TicketReleaseService implements TicketReleaseUseCase {

    private final TicketReservationRepository ticketReservationRepository;
    private final UserTicketRepository userTicketRepository;

    @Override
    @Transactional
    public void release(Long sessionId, String outcomeReason) {
        TicketReservation reservation = ticketReservationRepository.findBySessionId(sessionId).orElse(null);
        if (reservation == null) {
            log.warn("[TICKET RELEASE] 해당 세션의 예약을 찾을 수 없어요: sessionId={}", sessionId);
            return;
        }

        reservation.release(outcomeReason);
        ticketReservationRepository.save(reservation);
        userTicketRepository.increment(reservation.getUserId());
    }
}
