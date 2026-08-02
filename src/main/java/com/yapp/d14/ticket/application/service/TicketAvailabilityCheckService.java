package com.yapp.d14.ticket.application.service;

import com.yapp.d14.interview.application.port.in.InterviewSessionAbandonOnHoldExpiryUseCase;
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
    private final InterviewSessionAbandonOnHoldExpiryUseCase interviewSessionAbandonOnHoldExpiryUseCase;

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
            int released = ticketReservationRepository.releaseIfHeld(reservation.getId(), OUTCOME_HOLD_EXPIRED);
            if (released == 1) {
                userTicketRepository.increment(userId);
                // 이용권 정리는 여기서 끝났지만, interview_session.status는 그대로 IN_PROGRESS로 남는 정합성 문제가 있었다 — 함께 정리한다.
                interviewSessionAbandonOnHoldExpiryUseCase.abandonForHoldExpiry(reservation.getSessionId());
            }
        }
    }
}
