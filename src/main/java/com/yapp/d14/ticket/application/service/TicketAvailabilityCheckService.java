package com.yapp.d14.ticket.application.service;

import com.yapp.d14.interview.application.port.in.InterviewSessionAbandonIfInProgressUseCase;
import com.yapp.d14.interview.domain.AbandonCause;
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

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
class TicketAvailabilityCheckService implements TicketAvailabilityCheckUseCase {

    private final UserTicketRepository userTicketRepository;
    private final TicketReservationRepository ticketReservationRepository;
    private final InterviewSessionAbandonIfInProgressUseCase interviewSessionAbandonIfInProgressUseCase;

    @Override
    @Transactional
    public void checkAvailable(UUID userId) {
        cleanupHeldFromPreviousSessions(userId);

        UserTicket userTicket = userTicketRepository.findByUserId(userId)
                .orElseGet(() -> userTicketRepository.save(UserTicket.create(userId)));

        if (!userTicket.hasRemaining()) {
            throw new TicketException(TicketErrorCode.NO_REMAINING_TICKET);
        }
    }

    // 새 세션 생성을 시도하는 시점에, 그 유저의 이전 세션이 아직 IN_PROGRESS + HELD로 남아있으면
    // 경과 시간과 무관하게 정리한다(이용권 사이클 정리 문서 3장) — 재개 플로우의 20분 HOLD_TTL 판정과는 별개.
    private void cleanupHeldFromPreviousSessions(UUID userId) {
        List<TicketReservation> heldReservations = ticketReservationRepository.findHeldByUserId(userId);

        for (TicketReservation reservation : heldReservations) {
            int released = ticketReservationRepository.releaseIfHeld(reservation.getId(), AbandonCause.SESSION_SUPERSEDED.name());
            if (released == 1) {
                userTicketRepository.increment(userId);
                interviewSessionAbandonIfInProgressUseCase.abandon(reservation.getSessionId(), AbandonCause.SESSION_SUPERSEDED);
            }
        }
    }
}
