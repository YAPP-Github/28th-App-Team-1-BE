package com.yapp.d14.ticket.adapter.out.persistence.entity;

import com.yapp.d14.ticket.domain.UserTicket;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "user_ticket")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserTicketJpaEntity {

    @Id
    @Column(name = "user_id", columnDefinition = "uuid")
    private UUID userId;

    @Column(nullable = false)
    private int remaining;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public static UserTicketJpaEntity from(UserTicket userTicket) {
        UserTicketJpaEntity entity = new UserTicketJpaEntity();
        entity.userId = userTicket.getUserId();
        entity.remaining = userTicket.getRemaining();
        entity.updatedAt = userTicket.getUpdatedAt();
        return entity;
    }

    public UserTicket toDomain() {
        return UserTicket.of(userId, remaining, updatedAt);
    }
}
