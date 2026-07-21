package com.yapp.d14.user.application.port.in;

import java.util.UUID;

public interface UserNameDuplicateCheckUseCase {

    boolean isAvailable(UUID userId, String name);
}
