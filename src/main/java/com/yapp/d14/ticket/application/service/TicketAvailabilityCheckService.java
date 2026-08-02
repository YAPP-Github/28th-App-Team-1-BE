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
    //
    // 예약이 HELD인 것만으로는 정리 대상인지 알 수 없다 — 이미 COMPLETED/ABANDONED(USER_EXIT)된 세션도
    // 보고서 생성 결과를 기다리며 의도적으로 HELD 상태를 유지한다(B안). 그런 예약을 여기서 먼저 release해버리면
    // 나중에 보고서 생성이 끝나도 commitIfHeld/releaseIfHeld가 HELD 조건에 걸려 조용히 무시된다.
    // 그래서 abandon()이 세션을 IN_PROGRESS -> ABANDONED로 실제로 전환했을 때만 release한다.
    private void cleanupHeldFromPreviousSessions(UUID userId) {
        List<TicketReservation> heldReservations = ticketReservationRepository.findHeldByUserId(userId);

        for (TicketReservation reservation : heldReservations) {
            boolean abandoned = interviewSessionAbandonIfInProgressUseCase.abandon(reservation.getSessionId(), AbandonCause.SESSION_SUPERSEDED);
            if (!abandoned) {
                continue;
            }
            int released = ticketReservationRepository.releaseIfHeld(reservation.getId(), AbandonCause.SESSION_SUPERSEDED.name());
            if (released == 1) {
                userTicketRepository.increment(userId);
            }
        }
    }
}
