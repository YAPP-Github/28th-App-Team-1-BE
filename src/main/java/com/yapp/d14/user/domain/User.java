package com.yapp.d14.user.domain;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
public class User {

    private final UUID id;
    private final String email;
    private final String name;
    private final Provider provider;
    private final String providerId;
    private final LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Builder(access = AccessLevel.PRIVATE)
    private User(
            UUID id,
            String email,
            String name,
            Provider provider,
            String providerId,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
        this.id = id;
        this.email = email;
        this.name = name;
        this.provider = provider;
        this.providerId = providerId;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static User create(String email, String name, Provider provider, String providerId) {
        LocalDateTime now = LocalDateTime.now();
        return User.builder()
                .id(UUID.randomUUID())
                .email(email)
                .name(name)
                .provider(provider)
                .providerId(providerId)
                .createdAt(now)
                .updatedAt(now)
                .build();
    }

    public static User of(
            UUID id,
            String email,
            String name,
            Provider provider,
            String providerId,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
        return User.builder()
                .id(id)
                .email(email)
                .name(name)
                .provider(provider)
                .providerId(providerId)
                .createdAt(createdAt)
                .updatedAt(updatedAt)
                .build();
    }
}
