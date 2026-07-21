package com.yapp.d14.jd.application.port.out;

import java.util.UUID;

public interface JdValidationRateLimiter {

    int getTodayCount(UUID userId);

    void increment(UUID userId);
}
