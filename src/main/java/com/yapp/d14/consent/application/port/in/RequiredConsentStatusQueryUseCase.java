package com.yapp.d14.consent.application.port.in;

import com.yapp.d14.consent.domain.RequiredConsentStatus;

import java.util.UUID;

public interface RequiredConsentStatusQueryUseCase {

    RequiredConsentStatus getStatus(UUID userId);
}
