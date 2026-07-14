package com.yapp.d14.ticket.domain;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
public class UserTicket {

    private static final int DEFAULT_REMAINING = 3;

    private final UUID userId;
    private final int remaining;
    private final LocalDateTime updatedAt;

    @Builder(access = AccessLevel.PRIVATE)
    private UserTicket(UUID userId, int remaining, LocalDateTime updatedAt) {
        this.userId = userId;
        this.remaining = remaining;
        this.updatedAt = updatedAt;
    }

    public static UserTicket create(UUID userId) {
        return UserTicket.builder()
                .userId(userId)
                .remaining(DEFAULT_REMAINING)
                .updatedAt(LocalDateTime.now())
                .build();
    }

    public static UserTicket of(UUID userId, int remaining, LocalDateTime updatedAt) {
        return UserTicket.builder()
                .userId(userId)
                .remaining(remaining)
                .updatedAt(updatedAt)
                .build();
    }

    public boolean hasRemaining() {
        return remaining > 0;
    }
}
