package com.yapp.d14.ticket.application.port.in;

import java.util.UUID;

public interface TicketRemainingQueryUseCase {

    int getRemaining(UUID userId);
}
