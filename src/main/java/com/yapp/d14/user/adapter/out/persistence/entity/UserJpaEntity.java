package com.yapp.d14.user.adapter.out.persistence.entity;

import com.yapp.d14.job.domain.Job;
import com.yapp.d14.user.domain.Provider;
import com.yapp.d14.user.domain.User;
import jakarta.persistence.Column;
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
                @UniqueConstraint(columnNames = {"provider", "provider_id"}),
                @UniqueConstraint(columnNames = {"name"})
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
    private boolean nameRegistered;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Provider provider;

    @Column(nullable = false)
    private String providerId;

    @Enumerated(EnumType.STRING)
    private Job jobRole;

    private Integer careerYears;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    public static UserJpaEntity from(User user) {
        UserJpaEntity entity = new UserJpaEntity();
        entity.id = user.getId();
        entity.email = user.getEmail();
        entity.name = user.getName();
        entity.nameRegistered = user.isNameRegistered();
        entity.provider = user.getProvider();
        entity.providerId = user.getProviderId();
        entity.jobRole = user.getJobRole();
        entity.careerYears = user.getCareerYears();
        entity.createdAt = user.getCreatedAt();
        entity.updatedAt = user.getUpdatedAt();
        return entity;
    }

    public User toDomain() {
        return User.of(id, email, name, nameRegistered, provider, providerId, jobRole, careerYears, createdAt, updatedAt);
    }
}
