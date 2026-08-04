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
    private boolean profileRegistered;
    private final Provider provider;
    private final String providerId;
    private JobRole jobRole;
    private Integer careerYears;
    private String appleRefreshToken;
    private AccountStatus accountStatus;
    private SuspensionReason suspensionReason;
    private LocalDateTime suspendedAt;
    private LocalDateTime scheduledReleaseAt;
    private final LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Builder(access = AccessLevel.PRIVATE)
    private User(
            UUID id,
            String email,
            String name,
            boolean profileRegistered,
            Provider provider,
            String providerId,
            JobRole jobRole,
            Integer careerYears,
            String appleRefreshToken,
            AccountStatus accountStatus,
            SuspensionReason suspensionReason,
            LocalDateTime suspendedAt,
            LocalDateTime scheduledReleaseAt,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
        this.id = id;
        this.email = email;
        this.name = name;
        this.profileRegistered = profileRegistered;
        this.provider = provider;
        this.providerId = providerId;
        this.jobRole = jobRole;
        this.careerYears = careerYears;
        this.appleRefreshToken = appleRefreshToken;
        this.accountStatus = accountStatus;
        this.suspensionReason = suspensionReason;
        this.suspendedAt = suspendedAt;
        this.scheduledReleaseAt = scheduledReleaseAt;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static User create(String email, Provider provider, String providerId) {
        LocalDateTime now = LocalDateTime.now();
        return User.builder()
                .id(UUID.randomUUID())
                .email(email)
                .name(null)
                .profileRegistered(false)
                .provider(provider)
                .providerId(providerId)
                .jobRole(null)
                .careerYears(null)
                .appleRefreshToken(null)
                .accountStatus(AccountStatus.ACTIVE)
                .suspensionReason(null)
                .suspendedAt(null)
                .scheduledReleaseAt(null)
                .createdAt(now)
                .updatedAt(now)
                .build();
    }

    public static User of(
            UUID id,
            String email,
            String name,
            boolean profileRegistered,
            Provider provider,
            String providerId,
            JobRole jobRole,
            Integer careerYears,
            String appleRefreshToken,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
        return of(id, email, name, profileRegistered, provider, providerId, jobRole, careerYears, appleRefreshToken,
                AccountStatus.ACTIVE, null, null, null, createdAt, updatedAt);
    }

    public static User of(
            UUID id,
            String email,
            String name,
            boolean profileRegistered,
            Provider provider,
            String providerId,
            JobRole jobRole,
            Integer careerYears,
            String appleRefreshToken,
            AccountStatus accountStatus,
            SuspensionReason suspensionReason,
            LocalDateTime suspendedAt,
            LocalDateTime scheduledReleaseAt,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
        return User.builder()
                .id(id)
                .email(email)
                .name(name)
                .profileRegistered(profileRegistered)
                .provider(provider)
                .providerId(providerId)
                .jobRole(jobRole)
                .careerYears(careerYears)
                .appleRefreshToken(appleRefreshToken)
                // ddl-auto: update로 컬럼이 추가되면 기존 행은 null이 되므로 도메인 조립 시점에 ACTIVE로 메운다.
                .accountStatus(accountStatus == null ? AccountStatus.ACTIVE : accountStatus)
                .suspensionReason(suspensionReason)
                .suspendedAt(suspendedAt)
                .scheduledReleaseAt(scheduledReleaseAt)
                .createdAt(createdAt)
                .updatedAt(updatedAt)
                .build();
    }

    public void registerName(String name) {
        this.name = name;
        refreshProfileRegistered();
        this.updatedAt = LocalDateTime.now();
    }

    public void updateProfile(JobRole jobRole, Integer careerYears) {
        this.jobRole = jobRole;
        this.careerYears = careerYears;
        refreshProfileRegistered();
        this.updatedAt = LocalDateTime.now();
    }

    public void updateAppleRefreshToken(String appleRefreshToken) {
        this.appleRefreshToken = appleRefreshToken;
        this.updatedAt = LocalDateTime.now();
    }

    // 정지 전환·해제는 운영자가 DB에서 직접 처리한다(이슈 #92 결정). 여기서는 판정만 한다.
    // scheduledReleaseAt이 지나도 자동 해제되지 않는다 — PRD Part7 "해제도 수동만".
    public boolean isSuspended() {
        return accountStatus == AccountStatus.SUSPENDED;
    }

    private void refreshProfileRegistered() {
        this.profileRegistered = name != null && jobRole != null && careerYears != null;
    }
}
