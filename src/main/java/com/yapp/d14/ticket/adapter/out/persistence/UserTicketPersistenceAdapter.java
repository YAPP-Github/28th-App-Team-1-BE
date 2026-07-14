package com.yapp.d14.ticket.adapter.out.persistence;

import com.yapp.d14.ticket.adapter.out.persistence.entity.UserTicketJpaEntity;
import com.yapp.d14.ticket.application.port.out.UserTicketRepository;
import com.yapp.d14.ticket.domain.UserTicket;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
class UserTicketPersistenceAdapter implements UserTicketRepository {

    private final UserTicketJpaRepository userTicketJpaRepository;

    @Override
    public UserTicket save(UserTicket userTicket) {
        return userTicketJpaRepository.save(UserTicketJpaEntity.from(userTicket)).toDomain();
    }

    @Override
    public Optional<UserTicket> findByUserId(UUID userId) {
        return userTicketJpaRepository.findById(userId).map(UserTicketJpaEntity::toDomain);
    }

    @Override
    public int decrementIfAvailable(UUID userId) {
        return userTicketJpaRepository.decrementIfAvailable(userId);
    }

    @Override
    public void increment(UUID userId) {
        userTicketJpaRepository.increment(userId);
    }
}
