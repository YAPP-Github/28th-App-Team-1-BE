package com.yapp.d14.auth.application.port.in.result;

import com.yapp.d14.consent.domain.RequiredConsentStatus;
import com.yapp.d14.user.application.port.in.result.UserProfileResult;

public record AuthToken(
        String accessToken,
        String refreshToken,
        boolean newUser,
        RequiredConsentStatus consentStatus,
        UserProfileResult profile
) {
}
