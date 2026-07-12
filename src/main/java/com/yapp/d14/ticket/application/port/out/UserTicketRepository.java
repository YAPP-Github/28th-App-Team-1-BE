package com.yapp.d14.ticket.application.port.out;

import com.yapp.d14.ticket.domain.UserTicket;

import java.util.Optional;
import java.util.UUID;

public interface UserTicketRepository {

    UserTicket save(UserTicket userTicket);

    Optional<UserTicket> findByUserId(UUID userId);

    int decrementIfAvailable(UUID userId);

    void increment(UUID userId);
}
