package com.yapp.d14.ticket.application.service;

import com.yapp.d14.ticket.application.port.in.TicketRemainingQueryUseCase;
import com.yapp.d14.ticket.application.port.out.UserTicketRepository;
import com.yapp.d14.ticket.domain.UserTicket;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
class TicketRemainingQueryService implements TicketRemainingQueryUseCase {

    private final UserTicketRepository userTicketRepository;

    @Override
    @Transactional
    public int getRemaining(UUID userId) {
        return userTicketRepository.findByUserId(userId)
                .orElseGet(() -> userTicketRepository.save(UserTicket.create(userId)))
                .getRemaining();
    }
}
