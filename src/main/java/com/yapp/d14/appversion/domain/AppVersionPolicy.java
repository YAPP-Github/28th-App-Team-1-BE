package com.yapp.d14.appversion.domain;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class AppVersionPolicy {

    private final Platform platform;
    private final AppVersion minSupportedVersion;
    private final AppVersion latestVersion;
    private final String storeUrl;
    private final LocalDateTime updatedAt;

    @Builder(access = AccessLevel.PRIVATE)
    private AppVersionPolicy(Platform platform, AppVersion minSupportedVersion, AppVersion latestVersion,
                            String storeUrl, LocalDateTime updatedAt) {
        this.platform = platform;
        this.minSupportedVersion = minSupportedVersion;
        this.latestVersion = latestVersion;
        this.storeUrl = storeUrl;
        this.updatedAt = updatedAt;
    }

    public static AppVersionPolicy of(Platform platform, AppVersion minSupportedVersion, AppVersion latestVersion,
                                      String storeUrl, LocalDateTime updatedAt) {
        return AppVersionPolicy.builder()
                .platform(platform)
                .minSupportedVersion(minSupportedVersion)
                .latestVersion(latestVersion)
                .storeUrl(storeUrl)
                .updatedAt(updatedAt)
                .build();
    }

    public UpdateType determineUpdateType(AppVersion current) {
        if (current.isLowerThan(minSupportedVersion)) {
            return UpdateType.FORCE;
        }
        if (current.isLowerThan(latestVersion)) {
            return UpdateType.OPTIONAL;
        }
        return UpdateType.NONE;
    }
}
