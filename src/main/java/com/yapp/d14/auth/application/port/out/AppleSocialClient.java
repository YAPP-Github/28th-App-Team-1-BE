package com.yapp.d14.auth.application.port.out;

public interface AppleSocialClient {

    SocialUserInfo getUserInfo(String authorizationCode);
}
