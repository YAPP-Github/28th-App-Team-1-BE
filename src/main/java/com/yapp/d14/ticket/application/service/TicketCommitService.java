package com.yapp.d14.ticket.application.service;

import com.yapp.d14.ticket.application.port.in.TicketCommitUseCase;
import com.yapp.d14.ticket.application.port.out.TicketReservationRepository;
import com.yapp.d14.ticket.domain.TicketReservation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
class TicketCommitService implements TicketCommitUseCase {

    private final TicketReservationRepository ticketReservationRepository;

    @Override
    @Transactional
    public void commit(Long sessionId, String outcomeReason) {
        TicketReservation reservation = ticketReservationRepository.findBySessionId(sessionId).orElse(null);
        if (reservation == null) {
            log.warn("[TICKET COMMIT] 해당 세션의 예약을 찾을 수 없어요: sessionId={}", sessionId);
            return;
        }

        int committed = ticketReservationRepository.commitIfHeld(reservation.getId(), outcomeReason);
        if (committed == 0) {
            log.info("[TICKET COMMIT] 이미 처리된 예약이라 건너뜁니다: sessionId={}", sessionId);
        }
    }
}
