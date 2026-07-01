package com.yapp.d14.auth.application.port.out;

import com.yapp.d14.user.domain.Provider;

public interface SocialAuthClient {

    SocialUserInfo getUserInfo(Provider provider, String credential);
}
