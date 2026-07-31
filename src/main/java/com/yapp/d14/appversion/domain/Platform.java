package com.yapp.d14.appversion.domain;

import com.yapp.d14.appversion.exception.AppVersionErrorCode;
import com.yapp.d14.appversion.exception.AppVersionException;

public enum Platform {

    IOS,
    ANDROID;

    /**
     * 클라이언트가 전달한 platform 문자열을 방어적으로 파싱한다.
     * 대소문자를 무시하며, 알 수 없는 값이면 {@link AppVersionErrorCode#INVALID_PLATFORM} 예외를 던진다.
     */
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
