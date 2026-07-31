package com.yapp.d14.appversion.application.command;

import com.yapp.d14.appversion.domain.Platform;

/**
 * 앱 버전 정책 조회 커맨드. 클라이언트가 전달한 platform·version 문자열을 방어적으로 파싱해 생성한다.
 *
 * <p>version은 원문(마케팅 버전 문자열)을 그대로 담고, SemVer 파싱은 도메인({@code AppVersion})에서 수행한다.
 */
public record AppVersionCheckCommand(
        Platform platform,
        String version
) {

    public static AppVersionCheckCommand of(String platform, String version) {
        return new AppVersionCheckCommand(Platform.from(platform), version);
    }
}
