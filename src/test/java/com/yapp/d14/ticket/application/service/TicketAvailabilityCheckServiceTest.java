package com.yapp.d14.ticket.application.service;

import com.yapp.d14.interview.application.port.in.InterviewSessionAbandonIfInProgressUseCase;
import com.yapp.d14.interview.domain.AbandonCause;
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

    @Mock
    private InterviewSessionAbandonIfInProgressUseCase interviewSessionAbandonIfInProgressUseCase;

    @InjectMocks
    private TicketAvailabilityCheckService service;

    private final UUID userId = UUID.randomUUID();

    @Test
    void 신규_유저면_remaining_3으로_생성한_뒤_통과한다() {
        given(ticketReservationRepository.findHeldByUserId(userId)).willReturn(List.of());
        given(userTicketRepository.findByUserId(userId)).willReturn(Optional.empty());
        given(userTicketRepository.save(any())).willAnswer(invocation -> invocation.getArgument(0));

        assertThatCode(() -> service.checkAvailable(userId)).doesNotThrowAnyException();

        ArgumentCaptor<UserTicket> captor = ArgumentCaptor.forClass(UserTicket.class);
        verify(userTicketRepository).save(captor.capture());
        assertThat(captor.getValue().getRemaining()).isEqualTo(3);
    }

    @Test
    void 기존_유저이고_remaining이_양수면_통과한다() {
        given(ticketReservationRepository.findHeldByUserId(userId)).willReturn(List.of());
        given(userTicketRepository.findByUserId(userId))
                .willReturn(Optional.of(UserTicket.of(userId, 2, LocalDateTime.now())));

        assertThatCode(() -> service.checkAvailable(userId)).doesNotThrowAnyException();
        verify(userTicketRepository, never()).save(any());
    }

    @Test
    void remaining이_0이면_NO_REMAINING_TICKET() {
        given(ticketReservationRepository.findHeldByUserId(userId)).willReturn(List.of());
        given(userTicketRepository.findByUserId(userId))
                .willReturn(Optional.of(UserTicket.of(userId, 0, LocalDateTime.now())));

        assertThatThrownBy(() -> service.checkAvailable(userId))
                .isInstanceOf(TicketException.class)
                .extracting(e -> ((TicketException) e).getErrorCode())
                .isEqualTo(TicketErrorCode.NO_REMAINING_TICKET);
    }

    @Test
    void HELD된_이전_세션이_있으면_경과_시간과_무관하게_원자적으로_RELEASED로_전환하고_remaining을_복구한다() {
        TicketReservation held = TicketReservation.of(
                10L, userId, 1L, TicketReservationStatus.HELD, null, LocalDateTime.now(), null
        );
        given(ticketReservationRepository.findHeldByUserId(userId)).willReturn(List.of(held));
        given(userTicketRepository.findByUserId(userId))
                .willReturn(Optional.of(UserTicket.of(userId, 1, LocalDateTime.now())));
        given(ticketReservationRepository.releaseIfHeld(10L, "SESSION_SUPERSEDED")).willReturn(1);

        service.checkAvailable(userId);

        verify(ticketReservationRepository).releaseIfHeld(10L, "SESSION_SUPERSEDED");
        verify(userTicketRepository, times(1)).increment(userId);
        verify(interviewSessionAbandonIfInProgressUseCase).abandon(1L, AbandonCause.SESSION_SUPERSEDED);
    }

    @Test
    void 다른_트랜잭션이_이미_HELD_예약을_처리했으면_increment도_세션_정리도_하지_않는다() {
        TicketReservation held = TicketReservation.of(
                10L, userId, 1L, TicketReservationStatus.HELD, null, LocalDateTime.now(), null
        );
        given(ticketReservationRepository.findHeldByUserId(userId)).willReturn(List.of(held));
        given(userTicketRepository.findByUserId(userId))
                .willReturn(Optional.of(UserTicket.of(userId, 1, LocalDateTime.now())));
        given(ticketReservationRepository.releaseIfHeld(10L, "SESSION_SUPERSEDED")).willReturn(0);

        service.checkAvailable(userId);

        verify(userTicketRepository, never()).increment(any());
        verify(interviewSessionAbandonIfInProgressUseCase, never()).abandon(any(), any());
    }

    @Test
    void HELD된_이전_세션이_없으면_release나_increment가_일어나지_않는다() {
        given(ticketReservationRepository.findHeldByUserId(userId)).willReturn(List.of());
        given(userTicketRepository.findByUserId(userId))
                .willReturn(Optional.of(UserTicket.of(userId, 1, LocalDateTime.now())));

        service.checkAvailable(userId);

        verify(ticketReservationRepository, never()).releaseIfHeld(any(), any());
        verify(userTicketRepository, never()).increment(any());
    }
}
