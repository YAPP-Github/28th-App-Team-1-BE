package com.yapp.d14.auth.application.command;

import com.yapp.d14.user.domain.Provider;

public record SocialUnlinkCommand(Provider provider, String providerId, String appleRefreshToken) {
}
