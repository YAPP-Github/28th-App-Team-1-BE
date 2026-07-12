package com.yapp.d14.ticket.domain;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
public class TicketReservation {

    private final Long id;
    private final UUID userId;
    private final Long sessionId;
    private TicketReservationStatus status;
    private String outcomeReason;
    private final LocalDateTime heldAt;
    private LocalDateTime resolvedAt;

    @Builder(access = AccessLevel.PRIVATE)
    private TicketReservation(
            Long id,
            UUID userId,
            Long sessionId,
            TicketReservationStatus status,
            String outcomeReason,
            LocalDateTime heldAt,
            LocalDateTime resolvedAt
    ) {
        this.id = id;
        this.userId = userId;
        this.sessionId = sessionId;
        this.status = status;
        this.outcomeReason = outcomeReason;
        this.heldAt = heldAt;
        this.resolvedAt = resolvedAt;
    }

    public static TicketReservation hold(UUID userId, Long sessionId) {
        return TicketReservation.builder()
                .userId(userId)
                .sessionId(sessionId)
                .status(TicketReservationStatus.HELD)
                .heldAt(LocalDateTime.now())
                .build();
    }

    public static TicketReservation of(
            Long id,
            UUID userId,
            Long sessionId,
            TicketReservationStatus status,
            String outcomeReason,
            LocalDateTime heldAt,
            LocalDateTime resolvedAt
    ) {
        return TicketReservation.builder()
                .id(id)
                .userId(userId)
                .sessionId(sessionId)
                .status(status)
                .outcomeReason(outcomeReason)
                .heldAt(heldAt)
                .resolvedAt(resolvedAt)
                .build();
    }

    public void release(String outcomeReason) {
        this.status = TicketReservationStatus.RELEASED;
        this.outcomeReason = outcomeReason;
        this.resolvedAt = LocalDateTime.now();
    }
}
