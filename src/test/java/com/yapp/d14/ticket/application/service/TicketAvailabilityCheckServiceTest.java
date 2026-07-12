package com.yapp.d14.ticket.application.service;

import com.yapp.d14.ticket.application.port.out.TicketReservationRepository;
import com.yapp.d14.ticket.application.port.out.UserTicketRepository;
import com.yapp.d14.ticket.domain.TicketReservation;
import com.yapp.d14.ticket.domain.TicketReservationStatus;
import com.yapp.d14.ticket.domain.UserTicket;
import com.yapp.d14.ticket.exception.TicketErrorCode;
import com.yapp.d14.ticket.exception.TicketException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class TicketAvailabilityCheckServiceTest {

    @Mock
    private UserTicketRepository userTicketRepository;

    @Mock
    private TicketReservationRepository ticketReservationRepository;

    @InjectMocks
    private TicketAvailabilityCheckService service;

    private final UUID userId = UUID.randomUUID();

    @Test
    void 신규_유저면_remaining_3으로_생성한_뒤_통과한다() {
        given(ticketReservationRepository.findExpiredHeld(any(), any())).willReturn(List.of());
        given(userTicketRepository.findByUserId(userId)).willReturn(Optional.empty());
        given(userTicketRepository.save(any())).willAnswer(invocation -> invocation.getArgument(0));

        assertThatCode(() -> service.checkAvailable(userId)).doesNotThrowAnyException();

        ArgumentCaptor<UserTicket> captor = ArgumentCaptor.forClass(UserTicket.class);
        verify(userTicketRepository).save(captor.capture());
        assertThat(captor.getValue().getRemaining()).isEqualTo(3);
    }

    @Test
    void 기존_유저이고_remaining이_양수면_통과한다() {
        given(ticketReservationRepository.findExpiredHeld(any(), any())).willReturn(List.of());
        given(userTicketRepository.findByUserId(userId))
                .willReturn(Optional.of(UserTicket.of(userId, 2, LocalDateTime.now())));

        assertThatCode(() -> service.checkAvailable(userId)).doesNotThrowAnyException();
        verify(userTicketRepository, never()).save(any());
    }

    @Test
    void remaining이_0이면_NO_REMAINING_TICKET() {
        given(ticketReservationRepository.findExpiredHeld(any(), any())).willReturn(List.of());
        given(userTicketRepository.findByUserId(userId))
                .willReturn(Optional.of(UserTicket.of(userId, 0, LocalDateTime.now())));

        assertThatThrownBy(() -> service.checkAvailable(userId))
                .isInstanceOf(TicketException.class)
                .extracting(e -> ((TicketException) e).getErrorCode())
                .isEqualTo(TicketErrorCode.NO_REMAINING_TICKET);
    }

    @Test
    void 만료된_HELD_예약이_있으면_RELEASED로_전환하고_remaining을_복구한다() {
        TicketReservation expired = TicketReservation.hold(userId, 1L);
        given(ticketReservationRepository.findExpiredHeld(any(), any())).willReturn(List.of(expired));
        given(userTicketRepository.findByUserId(userId))
                .willReturn(Optional.of(UserTicket.of(userId, 1, LocalDateTime.now())));

        service.checkAvailable(userId);

        ArgumentCaptor<TicketReservation> captor = ArgumentCaptor.forClass(TicketReservation.class);
        verify(ticketReservationRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(TicketReservationStatus.RELEASED);
        verify(userTicketRepository, times(1)).increment(userId);
    }

    @Test
    void 만료된_예약이_없으면_release나_increment가_일어나지_않는다() {
        given(ticketReservationRepository.findExpiredHeld(any(), any())).willReturn(List.of());
        given(userTicketRepository.findByUserId(userId))
                .willReturn(Optional.of(UserTicket.of(userId, 1, LocalDateTime.now())));

        service.checkAvailable(userId);

        verify(ticketReservationRepository, never()).save(any());
        verify(userTicketRepository, never()).increment(any());
    }
}
