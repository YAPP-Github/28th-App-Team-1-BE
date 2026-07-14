package com.yapp.d14.ticket.adapter.out.persistence;

import com.yapp.d14.ticket.adapter.out.persistence.entity.UserTicketJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

interface UserTicketJpaRepository extends JpaRepository<UserTicketJpaEntity, UUID> {

    @Modifying(clearAutomatically = true)
    @Query("UPDATE UserTicketJpaEntity t SET t.remaining = t.remaining - 1, t.updatedAt = CURRENT_TIMESTAMP " +
            "WHERE t.userId = :userId AND t.remaining > 0")
    int decrementIfAvailable(@Param("userId") UUID userId);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE UserTicketJpaEntity t SET t.remaining = t.remaining + 1, t.updatedAt = CURRENT_TIMESTAMP " +
            "WHERE t.userId = :userId")
    void increment(@Param("userId") UUID userId);
}
