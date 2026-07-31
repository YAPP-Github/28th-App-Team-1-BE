package com.yapp.d14.appversion.adapter.out.persistence.entity;

import com.yapp.d14.appversion.domain.AppVersion;
import com.yapp.d14.appversion.domain.AppVersionPolicy;
import com.yapp.d14.appversion.domain.Platform;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "app_version_policy")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AppVersionPolicyJpaEntity {

    @Id
    @Enumerated(EnumType.STRING)
    @Column(name = "platform", nullable = false, length = 20)
    private Platform platform;

    @Column(name = "min_supported_version", nullable = false, length = 30)
    private String minSupportedVersion;

    @Column(name = "latest_version", nullable = false, length = 30)
    private String latestVersion;

    @Column(name = "store_url", nullable = false)
    private String storeUrl;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public AppVersionPolicy toDomain() {
        return AppVersionPolicy.of(
                platform,
                AppVersion.parse(minSupportedVersion),
                AppVersion.parse(latestVersion),
                storeUrl,
                updatedAt
        );
    }
}
