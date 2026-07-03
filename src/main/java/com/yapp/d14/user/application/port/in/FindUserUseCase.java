package com.yapp.d14.user.application.port.in;

import com.yapp.d14.user.domain.User;

import java.util.UUID;

public interface FindUserUseCase {

    User findById(UUID userId);
}
