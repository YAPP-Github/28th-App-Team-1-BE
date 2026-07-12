package com.yapp.d14.ticket.application.service;

import com.yapp.d14.ticket.application.port.out.TicketReservationRepository;
import com.yapp.d14.ticket.application.port.out.UserTicketRepository;
import com.yapp.d14.ticket.domain.TicketReservation;
import com.yapp.d14.ticket.domain.TicketReservationStatus;
import com.yapp.d14.ticket.exception.TicketErrorCode;
import com.yapp.d14.ticket.exception.TicketException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class TicketHoldServiceTest {

    @Mock
    private UserTicketRepository userTicketRepository;

    @Mock
    private TicketReservationRepository ticketReservationRepository;

    @InjectMocks
    private TicketHoldService service;

    private final UUID userId = UUID.randomUUID();
    private final Long sessionId = 1L;

    @Test
    void decrement이_성공하면_예약을_HELD로_저장한다() {
        given(userTicketRepository.decrementIfAvailable(userId)).willReturn(1);

        service.hold(userId, sessionId);

        ArgumentCaptor<TicketReservation> captor = ArgumentCaptor.forClass(TicketReservation.class);
        verify(ticketReservationRepository).save(captor.capture());
        assertThat(captor.getValue().getUserId()).isEqualTo(userId);
        assertThat(captor.getValue().getSessionId()).isEqualTo(sessionId);
        assertThat(captor.getValue().getStatus()).isEqualTo(TicketReservationStatus.HELD);
    }

    @Test
    void decrement이_실패하면_NO_REMAINING_TICKET_예약을_저장하지_않는다() {
        given(userTicketRepository.decrementIfAvailable(userId)).willReturn(0);

        assertThatThrownBy(() -> service.hold(userId, sessionId))
                .isInstanceOf(TicketException.class)
                .extracting(e -> ((TicketException) e).getErrorCode())
                .isEqualTo(TicketErrorCode.NO_REMAINING_TICKET);

        verify(ticketReservationRepository, never()).save(any());
    }
}
