package com.yapp.d14.appversion.application.command;

import com.yapp.d14.appversion.domain.Platform;
import com.yapp.d14.appversion.exception.AppVersionErrorCode;
import com.yapp.d14.appversion.exception.AppVersionException;

public record AppVersionCheckCommand(
        Platform platform,
        String version
) {

    public static AppVersionCheckCommand of(String platform, String version) {
        if (version == null || version.isBlank()) {
            throw new AppVersionException(AppVersionErrorCode.INVALID_VERSION_FORMAT);
        }
        return new AppVersionCheckCommand(Platform.from(platform), version);
    }
}
