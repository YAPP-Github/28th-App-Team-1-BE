package com.yapp.d14.consent.application.service;

import com.yapp.d14.consent.application.port.in.PendingConsentItemsQueryUseCase;
import com.yapp.d14.consent.application.port.out.ConsentRecordRepository;
import com.yapp.d14.consent.domain.ConsentItem;
import com.yapp.d14.consent.domain.ConsentRecord;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
class PendingConsentItemsQueryService implements PendingConsentItemsQueryUseCase {

    private final ConsentRecordRepository consentRecordRepository;

    @Override
    public List<ConsentItem> getPendingItems(UUID userId) {
        List<ConsentRecord> records = consentRecordRepository.findAllByUserId(userId);
        Map<ConsentItem, ConsentRecord> latestByItem = ConsentRecord.latestByItem(records);

        return Arrays.stream(ConsentItem.values())
                .filter(item -> isMissingRequired(item, latestByItem) || isAgreedButStale(item, latestByItem))
                .toList();
    }

    private boolean isMissingRequired(ConsentItem item, Map<ConsentItem, ConsentRecord> latestByItem) {
        return item.isRequired() && !latestByItem.containsKey(item);
    }

    private boolean isAgreedButStale(ConsentItem item, Map<ConsentItem, ConsentRecord> latestByItem) {
        ConsentRecord record = latestByItem.get(item);
        return record != null && record.isAgreed() && record.isStale();
    }
}
