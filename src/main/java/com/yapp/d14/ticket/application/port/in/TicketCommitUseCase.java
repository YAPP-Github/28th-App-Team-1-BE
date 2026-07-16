package com.yapp.d14.ticket.application.port.in;

public interface TicketCommitUseCase {

    void commit(Long sessionId, String outcomeReason);
}
