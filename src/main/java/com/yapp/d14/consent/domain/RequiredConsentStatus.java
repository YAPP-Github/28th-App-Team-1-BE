package com.yapp.d14.consent.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Map;

@Getter
@RequiredArgsConstructor
public enum RequiredConsentStatus {

    NOT_SUBMITTED("필수 동의 미제출"),
    STALE("일부 항목 재동의 필요"),
    UP_TO_DATE("필수 동의 최신 상태");

    private final String label;

    public static RequiredConsentStatus evaluate(Map<ConsentItem, ConsentRecord> latestByItem) {
        List<ConsentItem> requiredItems = ConsentItem.requiredItems();

        boolean anyMissing = requiredItems.stream()
                .anyMatch(item -> !latestByItem.containsKey(item));
        if (anyMissing) {
            return NOT_SUBMITTED;
        }

        boolean anyStale = requiredItems.stream()
                .anyMatch(item -> latestByItem.get(item).isStale());
        return anyStale ? STALE : UP_TO_DATE;
    }
}
