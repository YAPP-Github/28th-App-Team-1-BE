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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class TicketRemainingQueryServiceTest {

    @Mock
    private UserTicketRepository userTicketRepository;

    @InjectMocks
    private TicketRemainingQueryService service;

    private final UUID userId = UUID.randomUUID();

    @Test
    void 기존_유저면_저장된_remaining을_반환한다() {
        given(userTicketRepository.findByUserId(userId))
                .willReturn(Optional.of(UserTicket.of(userId, 2, LocalDateTime.now())));

        int remaining = service.getRemaining(userId);

        assertThat(remaining).isEqualTo(2);
    }

    @Test
    void 신규_유저면_remaining_3으로_생성한_뒤_반환한다() {
        given(userTicketRepository.findByUserId(userId)).willReturn(Optional.empty());
        given(userTicketRepository.save(any())).willAnswer(invocation -> invocation.getArgument(0));

        int remaining = service.getRemaining(userId);

        assertThat(remaining).isEqualTo(3);
        ArgumentCaptor<UserTicket> captor = ArgumentCaptor.forClass(UserTicket.class);
        verify(userTicketRepository).save(captor.capture());
        assertThat(captor.getValue().getUserId()).isEqualTo(userId);
    }
}
