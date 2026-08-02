package com.yapp.d14.ticket.application.port.in.result;

import java.time.LocalDateTime;

public record TicketReservationHoldStatusResult(boolean held, LocalDateTime heldAt) {
}
