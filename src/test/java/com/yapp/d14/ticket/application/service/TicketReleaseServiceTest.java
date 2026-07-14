package com.yapp.d14.ticket.application.service;

import com.yapp.d14.ticket.application.port.out.TicketReservationRepository;
import com.yapp.d14.ticket.application.port.out.UserTicketRepository;
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
class TicketReleaseServiceTest {

    @Mock
    private TicketReservationRepository ticketReservationRepository;

    @Mock
    private UserTicketRepository userTicketRepository;

    @InjectMocks
    private TicketReleaseService service;

    private final UUID userId = UUID.randomUUID();

    private TicketReservation heldReservation() {
        return TicketReservation.of(
                10L, userId, 1L, TicketReservationStatus.HELD, null, LocalDateTime.now(), null
        );
    }

    @Test
    void 예약이_있으면_원자적으로_release하고_잔여를_되돌린다() {
        given(ticketReservationRepository.findBySessionId(1L)).willReturn(Optional.of(heldReservation()));
        given(ticketReservationRepository.releaseIfHeld(10L, "PRELOAD_FAILED")).willReturn(1);

        service.release(1L, "PRELOAD_FAILED");

        verify(ticketReservationRepository).releaseIfHeld(10L, "PRELOAD_FAILED");
        verify(userTicketRepository).increment(userId);
    }

    @Test
    void 이미_다른_트랜잭션이_release했으면_increment하지_않는다() {
        given(ticketReservationRepository.findBySessionId(1L)).willReturn(Optional.of(heldReservation()));
        given(ticketReservationRepository.releaseIfHeld(10L, "PRELOAD_FAILED")).willReturn(0);

        service.release(1L, "PRELOAD_FAILED");

        verify(userTicketRepository, never()).increment(any());
    }

    @Test
    void 예약이_없으면_아무것도_하지_않는다() {
        given(ticketReservationRepository.findBySessionId(1L)).willReturn(Optional.empty());

        service.release(1L, "PRELOAD_FAILED");

        verify(ticketReservationRepository, never()).releaseIfHeld(any(), any());
        verify(userTicketRepository, never()).increment(any());
    }
}
