package com.yapp.d14.user.application.port.in;

import java.util.UUID;

public interface UserNameRegisterUseCase {

    void register(UUID userId, String name);
}
