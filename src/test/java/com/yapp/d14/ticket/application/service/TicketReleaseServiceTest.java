package com.yapp.d14.ticket.application.service;

import com.yapp.d14.ticket.application.port.out.TicketReservationRepository;
import com.yapp.d14.ticket.application.port.out.UserTicketRepository;
import com.yapp.d14.ticket.domain.TicketReservation;
import com.yapp.d14.ticket.domain.TicketReservationStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
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

    @Test
    void 예약이_있으면_release하고_잔여를_되돌린다() {
        TicketReservation reservation = TicketReservation.hold(userId, 1L);
        given(ticketReservationRepository.findBySessionId(1L)).willReturn(Optional.of(reservation));
        given(ticketReservationRepository.save(any())).willAnswer(invocation -> invocation.getArgument(0));

        service.release(1L, "PRELOAD_FAILED");

        ArgumentCaptor<TicketReservation> captor = ArgumentCaptor.forClass(TicketReservation.class);
        verify(ticketReservationRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(TicketReservationStatus.RELEASED);
        assertThat(captor.getValue().getOutcomeReason()).isEqualTo("PRELOAD_FAILED");
        verify(userTicketRepository).increment(userId);
    }

    @Test
    void 예약이_없으면_아무것도_하지_않는다() {
        given(ticketReservationRepository.findBySessionId(1L)).willReturn(Optional.empty());

        service.release(1L, "PRELOAD_FAILED");

        verify(ticketReservationRepository, never()).save(any());
        verify(userTicketRepository, never()).increment(any());
    }
}
