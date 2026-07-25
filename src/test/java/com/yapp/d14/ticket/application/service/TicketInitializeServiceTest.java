package com.yapp.d14.ticket.application.service;

import com.yapp.d14.ticket.application.port.out.UserTicketRepository;
import com.yapp.d14.ticket.domain.UserTicket;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class TicketInitializeServiceTest {

    @Mock
    private UserTicketRepository userTicketRepository;

    @InjectMocks
    private TicketInitializeService service;

    private final UUID userId = UUID.randomUUID();

    @Test
    void 아직_이용권이_없으면_remaining_3으로_생성한다() {
        given(userTicketRepository.findByUserId(userId)).willReturn(Optional.empty());

        service.initialize(userId);

        ArgumentCaptor<UserTicket> captor = ArgumentCaptor.forClass(UserTicket.class);
        verify(userTicketRepository).save(captor.capture());
        assertThat(captor.getValue().getUserId()).isEqualTo(userId);
        assertThat(captor.getValue().getRemaining()).isEqualTo(3);
    }

    @Test
    void 이미_이용권이_있으면_아무것도_하지_않는다() {
        given(userTicketRepository.findByUserId(userId))
                .willReturn(Optional.of(UserTicket.of(userId, 1, LocalDateTime.now())));

        service.initialize(userId);

        verify(userTicketRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }
}
