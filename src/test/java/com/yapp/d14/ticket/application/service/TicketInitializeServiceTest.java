package com.yapp.d14.ticket.application.service;

import com.yapp.d14.ticket.application.port.out.UserTicketRepository;
import com.yapp.d14.ticket.domain.UserTicket;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class TicketInitializeServiceTest {

    @Mock
    private UserTicketRepository userTicketRepository;

    @InjectMocks
    private TicketInitializeService service;

    private final UUID userId = UUID.randomUUID();

    @Test
    void remaining_3인_이용권_생성을_DB에_위임하고_이미_있으면_충돌없이_무시된다() {
        service.initialize(userId);

        ArgumentCaptor<UserTicket> captor = ArgumentCaptor.forClass(UserTicket.class);
        verify(userTicketRepository).insertIfAbsent(captor.capture());
        assertThat(captor.getValue().getUserId()).isEqualTo(userId);
        assertThat(captor.getValue().getRemaining()).isEqualTo(3);
    }
}
