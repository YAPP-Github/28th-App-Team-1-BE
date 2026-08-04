package com.yapp.d14.user.adapter.out.persistence.entity;

import com.yapp.d14.common.crypto.EncryptedStringConverter;
import com.yapp.d14.user.domain.AccountStatus;
import com.yapp.d14.user.domain.JobRole;
import com.yapp.d14.user.domain.Provider;
import com.yapp.d14.user.domain.SuspensionReason;
import com.yapp.d14.user.domain.User;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "users",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"provider", "provider_id"})
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserJpaEntity {

    @Id
    @Column(columnDefinition = "uuid")
    private UUID id;

    private String email;

    private String name;

    @Column(nullable = false, columnDefinition = "boolean default false")
    private boolean profileRegistered;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Provider provider;

    @Column(nullable = false)
    private String providerId;

    @Enumerated(EnumType.STRING)
    private JobRole jobRole;

    private Integer careerYears;

    @Convert(converter = EncryptedStringConverter.class)
    @Column(columnDefinition = "text")
    private String appleRefreshToken;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "varchar(20) default 'ACTIVE'")
    private AccountStatus accountStatus;

    @Enumerated(EnumType.STRING)
    private SuspensionReason suspensionReason;

    private LocalDateTime suspendedAt;

    private LocalDateTime scheduledReleaseAt;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    public static UserJpaEntity from(User user) {
        UserJpaEntity entity = new UserJpaEntity();
        entity.id = user.getId();
        entity.email = user.getEmail();
        entity.name = user.getName();
        entity.profileRegistered = user.isProfileRegistered();
        entity.provider = user.getProvider();
        entity.providerId = user.getProviderId();
        entity.jobRole = user.getJobRole();
        entity.careerYears = user.getCareerYears();
        entity.appleRefreshToken = user.getAppleRefreshToken();
        entity.accountStatus = user.getAccountStatus();
        entity.suspensionReason = user.getSuspensionReason();
        entity.suspendedAt = user.getSuspendedAt();
        entity.scheduledReleaseAt = user.getScheduledReleaseAt();
        entity.createdAt = user.getCreatedAt();
        entity.updatedAt = user.getUpdatedAt();
        return entity;
    }

    public User toDomain() {
        return User.of(
                id, email, name, profileRegistered, provider, providerId, jobRole, careerYears,
                appleRefreshToken, accountStatus, suspensionReason, suspendedAt, scheduledReleaseAt,
                createdAt, updatedAt
        );
    }
}
