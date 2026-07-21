package com.yapp.d14.ticket.application.service;

import com.yapp.d14.ticket.application.port.in.TicketInitializeUseCase;
import com.yapp.d14.ticket.application.port.out.UserTicketRepository;
import com.yapp.d14.ticket.domain.UserTicket;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
class TicketInitializeService implements TicketInitializeUseCase {

    private final UserTicketRepository userTicketRepository;

    @Override
    @Transactional
    public void initialize(UUID userId) {
        if (userTicketRepository.findByUserId(userId).isPresent()) {
            return;
        }

        userTicketRepository.save(UserTicket.create(userId));
    }
}
