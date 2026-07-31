package com.yapp.d14.appversion.domain;

import com.yapp.d14.appversion.exception.AppVersionErrorCode;
import com.yapp.d14.appversion.exception.AppVersionException;

public enum Platform {

    IOS,
    ANDROID;

    public static Platform from(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new AppVersionException(AppVersionErrorCode.INVALID_PLATFORM);
        }
        try {
            return Platform.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new AppVersionException(AppVersionErrorCode.INVALID_PLATFORM);
        }
    }
}
