package com.yapp.d14.user.application.port.in;

import java.util.UUID;

public interface UserWithdrawUseCase {

    void withdraw(UUID userId);
}
