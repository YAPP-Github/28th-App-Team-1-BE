package com.yapp.d14.ticket.application.service;

import com.yapp.d14.ticket.application.port.in.result.TicketReservationHoldStatusResult;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class TicketReservationStatusQueryServiceTest {

    @Mock
    private TicketReservationRepository ticketReservationRepository;

    @InjectMocks
    private TicketReservationStatusQueryService service;

    private final Long sessionId = 1L;

    @Test
    void 예약이_HELD면_held_true와_heldAt을_반환한다() {
        LocalDateTime heldAt = LocalDateTime.now();
        TicketReservation reservation = TicketReservation.of(
                10L, UUID.randomUUID(), sessionId, TicketReservationStatus.HELD, null, heldAt, null
        );
        given(ticketReservationRepository.findBySessionId(sessionId)).willReturn(Optional.of(reservation));

        TicketReservationHoldStatusResult result = service.getHoldStatus(sessionId);

        assertThat(result.held()).isTrue();
        assertThat(result.heldAt()).isEqualTo(heldAt);
    }

    @Test
    void 예약이_HELD가_아니면_held_false를_반환한다() {
        TicketReservation reservation = TicketReservation.of(
                10L, UUID.randomUUID(), sessionId, TicketReservationStatus.RELEASED, "USER_EXIT", LocalDateTime.now(), LocalDateTime.now()
        );
        given(ticketReservationRepository.findBySessionId(sessionId)).willReturn(Optional.of(reservation));

        TicketReservationHoldStatusResult result = service.getHoldStatus(sessionId);

        assertThat(result.held()).isFalse();
    }

    @Test
    void 예약_자체가_없으면_held_false를_반환한다() {
        given(ticketReservationRepository.findBySessionId(sessionId)).willReturn(Optional.empty());

        TicketReservationHoldStatusResult result = service.getHoldStatus(sessionId);

        assertThat(result.held()).isFalse();
        assertThat(result.heldAt()).isNull();
    }
}
