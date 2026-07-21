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
    private String name;
    private boolean nameRegistered;
    private final Provider provider;
    private final String providerId;
    private JobRole jobRole;
    private Integer careerYears;
    private final LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Builder(access = AccessLevel.PRIVATE)
    private User(
            UUID id,
            String email,
            String name,
            boolean nameRegistered,
            Provider provider,
            String providerId,
            JobRole jobRole,
            Integer careerYears,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
        this.id = id;
        this.email = email;
        this.name = name;
        this.nameRegistered = nameRegistered;
        this.provider = provider;
        this.providerId = providerId;
        this.jobRole = jobRole;
        this.careerYears = careerYears;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static User create(String email, Provider provider, String providerId) {
        LocalDateTime now = LocalDateTime.now();
        return User.builder()
                .id(UUID.randomUUID())
                .email(email)
                .name(null)
                .nameRegistered(false)
                .provider(provider)
                .providerId(providerId)
                .jobRole(null)
                .careerYears(null)
                .createdAt(now)
                .updatedAt(now)
                .build();
    }

    public static User of(
            UUID id,
            String email,
            String name,
            boolean nameRegistered,
            Provider provider,
            String providerId,
            JobRole jobRole,
            Integer careerYears,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
        return User.builder()
                .id(id)
                .email(email)
                .name(name)
                .nameRegistered(nameRegistered)
                .provider(provider)
                .providerId(providerId)
                .jobRole(jobRole)
                .careerYears(careerYears)
                .createdAt(createdAt)
                .updatedAt(updatedAt)
                .build();
    }

    public void registerName(String name) {
        this.name = name;
        this.nameRegistered = true;
        this.updatedAt = LocalDateTime.now();
    }

    public void updateProfile(JobRole jobRole, Integer careerYears) {
        this.jobRole = jobRole;
        this.careerYears = careerYears;
        this.updatedAt = LocalDateTime.now();
    }
}
