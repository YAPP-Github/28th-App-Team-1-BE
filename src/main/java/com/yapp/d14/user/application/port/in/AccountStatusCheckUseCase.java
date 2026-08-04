package com.yapp.d14.user.application.port.in;

import java.util.UUID;

public interface AccountStatusCheckUseCase {

    void checkNotSuspended(UUID userId);
}
