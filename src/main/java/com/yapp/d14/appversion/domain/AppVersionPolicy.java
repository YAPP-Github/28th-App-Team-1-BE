package com.yapp.d14.appversion.domain;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 플랫폼별 앱 버전 정책. 최소 지원 버전·최신 버전을 기준으로 클라이언트 버전의 업데이트 유형을 판정한다.
 */
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

    /**
     * 판정 규칙:
     * <ul>
     *     <li>현재 버전 &lt; minSupportedVersion → {@link UpdateType#FORCE}</li>
     *     <li>minSupportedVersion ≤ 현재 버전 &lt; latestVersion → {@link UpdateType#OPTIONAL}</li>
     *     <li>현재 버전 ≥ latestVersion → {@link UpdateType#NONE}</li>
     * </ul>
     */
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
