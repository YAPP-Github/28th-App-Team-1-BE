package com.yapp.d14.ticket.application.port.in;

import com.yapp.d14.ticket.application.port.in.result.TicketReservationHoldStatusResult;

public interface TicketReservationStatusQueryUseCase {

    TicketReservationHoldStatusResult getHoldStatus(Long sessionId);
}
