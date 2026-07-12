package com.yapp.d14.ticket.application.port.in;

public interface TicketReleaseUseCase {

    void release(Long sessionId, String outcomeReason);
}
