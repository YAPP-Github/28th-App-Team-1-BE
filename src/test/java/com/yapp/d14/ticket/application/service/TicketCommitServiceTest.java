package com.yapp.d14.ticket.application.service;

import com.yapp.d14.ticket.application.port.out.TicketReservationRepository;
import com.yapp.d14.ticket.domain.TicketReservation;
import com.yapp.d14.ticket.domain.TicketReservationStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class TicketCommitServiceTest {

    @Mock
    private TicketReservationRepository ticketReservationRepository;

    @InjectMocks
    private TicketCommitService service;

    private final UUID userId = UUID.randomUUID();

    private TicketReservation heldReservation() {
        return TicketReservation.of(
                10L, userId, 1L, TicketReservationStatus.HELD, null, LocalDateTime.now(), null
        );
    }

    @Test
    void 예약이_있으면_원자적으로_commit한다() {
        given(ticketReservationRepository.findBySessionId(1L)).willReturn(Optional.of(heldReservation()));
        given(ticketReservationRepository.commitIfHeld(10L, "COMPLETED")).willReturn(1);

        service.commit(1L, "COMPLETED");

        verify(ticketReservationRepository).commitIfHeld(10L, "COMPLETED");
    }

    @Test
    void 이미_다른_트랜잭션이_처리했으면_그대로_넘어간다() {
        given(ticketReservationRepository.findBySessionId(1L)).willReturn(Optional.of(heldReservation()));
        given(ticketReservationRepository.commitIfHeld(10L, "COMPLETED")).willReturn(0);

        service.commit(1L, "COMPLETED");

        verify(ticketReservationRepository).commitIfHeld(10L, "COMPLETED");
    }

    @Test
    void 예약이_없으면_아무것도_하지_않는다() {
        given(ticketReservationRepository.findBySessionId(1L)).willReturn(Optional.empty());

        service.commit(1L, "COMPLETED");

        verify(ticketReservationRepository, never()).commitIfHeld(any(), any());
    }
}
