package com.yapp.d14.consent.application.port.in;

import com.yapp.d14.consent.domain.ConsentItem;

import java.util.List;
import java.util.UUID;

public interface PendingConsentItemsQueryUseCase {

    List<ConsentItem> getPendingItems(UUID userId);
}
