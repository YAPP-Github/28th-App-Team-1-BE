package com.yapp.d14.ticket.adapter.out.persistence.entity;

import com.yapp.d14.ticket.domain.TicketReservation;
import com.yapp.d14.ticket.domain.TicketReservationStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "ticket_reservation", uniqueConstraints = @UniqueConstraint(columnNames = "session_id"))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TicketReservationJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", columnDefinition = "uuid", nullable = false)
    private UUID userId;

    @Column(name = "session_id", nullable = false)
    private Long sessionId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TicketReservationStatus status;

    @Column(name = "outcome_reason")
    private String outcomeReason;

    @Column(name = "held_at", nullable = false)
    private LocalDateTime heldAt;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    public static TicketReservationJpaEntity from(TicketReservation ticketReservation) {
        TicketReservationJpaEntity entity = new TicketReservationJpaEntity();
        entity.id = ticketReservation.getId();
        entity.userId = ticketReservation.getUserId();
        entity.sessionId = ticketReservation.getSessionId();
        entity.status = ticketReservation.getStatus();
        entity.outcomeReason = ticketReservation.getOutcomeReason();
        entity.heldAt = ticketReservation.getHeldAt();
        entity.resolvedAt = ticketReservation.getResolvedAt();
        return entity;
    }

    public TicketReservation toDomain() {
        return TicketReservation.of(id, userId, sessionId, status, outcomeReason, heldAt, resolvedAt);
    }
}
