package com.yapp.d14.appversion.application.port.in.result;

import com.yapp.d14.appversion.domain.UpdateType;

public record AppVersionCheckResult(
        UpdateType updateType,
        String latestVersion,
        String minSupportedVersion,
        String storeUrl,
        String title,
        String body
) {
}
