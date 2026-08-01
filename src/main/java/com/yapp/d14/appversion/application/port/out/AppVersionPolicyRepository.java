package com.yapp.d14.appversion.application.port.out;

import com.yapp.d14.appversion.domain.AppVersionPolicy;
import com.yapp.d14.appversion.domain.Platform;

import java.util.Optional;

public interface AppVersionPolicyRepository {

    Optional<AppVersionPolicy> findByPlatform(Platform platform);
}
