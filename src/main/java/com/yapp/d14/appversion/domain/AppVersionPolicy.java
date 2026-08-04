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
    private final String forceTitle;
    private final String forceBody;
    private final String optionalTitle;
    private final String optionalBody;
    private final LocalDateTime updatedAt;

    @Builder(access = AccessLevel.PRIVATE)
    private AppVersionPolicy(Platform platform, AppVersion minSupportedVersion, AppVersion latestVersion,
                            String storeUrl, String forceTitle, String forceBody,
                            String optionalTitle, String optionalBody, LocalDateTime updatedAt) {
        this.platform = platform;
        this.minSupportedVersion = minSupportedVersion;
        this.latestVersion = latestVersion;
        this.storeUrl = storeUrl;
        this.forceTitle = forceTitle;
        this.forceBody = forceBody;
        this.optionalTitle = optionalTitle;
        this.optionalBody = optionalBody;
        this.updatedAt = updatedAt;
    }

    public static AppVersionPolicy of(Platform platform, AppVersion minSupportedVersion, AppVersion latestVersion,
                                      String storeUrl, String forceTitle, String forceBody,
                                      String optionalTitle, String optionalBody, LocalDateTime updatedAt) {
        return AppVersionPolicy.builder()
                .platform(platform)
                .minSupportedVersion(minSupportedVersion)
                .latestVersion(latestVersion)
                .storeUrl(storeUrl)
                .forceTitle(forceTitle)
                .forceBody(forceBody)
                .optionalTitle(optionalTitle)
                .optionalBody(optionalBody)
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

    public String resolveTitle(UpdateType updateType) {
        return switch (updateType) {
            case FORCE -> forceTitle;
            case OPTIONAL -> optionalTitle;
            case NONE -> null;
        };
    }

    public String resolveBody(UpdateType updateType) {
        return switch (updateType) {
            case FORCE -> forceBody;
            case OPTIONAL -> optionalBody;
            case NONE -> null;
        };
    }
}
