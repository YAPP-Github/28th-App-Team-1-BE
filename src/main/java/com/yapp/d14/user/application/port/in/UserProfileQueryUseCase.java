package com.yapp.d14.user.application.port.in;

import com.yapp.d14.user.application.port.in.result.UserProfileResult;

import java.util.UUID;

public interface UserProfileQueryUseCase {

    UserProfileResult getProfile(UUID userId);
}
