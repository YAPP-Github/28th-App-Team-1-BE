package com.yapp.d14.user.application.port.in;

import java.util.UUID;

public interface UserProfileInitializeUseCase {

    void initializeIfAbsent(UUID userId, String rawJobRole, Integer careerYears);
}
