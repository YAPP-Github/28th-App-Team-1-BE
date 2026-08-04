package com.yapp.d14.consent.application.port.in;

import java.util.UUID;

public interface ConsentUpToDateCheckUseCase {

    void checkUpToDate(UUID userId);
}
