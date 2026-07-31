package com.yapp.d14.consent.domain;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ConsentRecord {

    private final UUID userId;
    private final ConsentItem item;
    private final int version;
    private final boolean agreed;
    private final LocalDateTime respondedAt;

    public static ConsentRecord create(UUID userId, ConsentItem item, boolean agreed) {
        return ConsentRecord.builder()
                .userId(userId)
                .item(item)
                .version(item.getCurrentVersion())
                .agreed(agreed)
                .respondedAt(LocalDateTime.now())
                .build();
    }

    public static ConsentRecord of(UUID userId, ConsentItem item, int version, boolean agreed, LocalDateTime respondedAt) {
        return ConsentRecord.builder()
                .userId(userId)
                .item(item)
                .version(version)
                .agreed(agreed)
                .respondedAt(respondedAt)
                .build();
    }

    public boolean isStale() {
        return version < item.getCurrentVersion();
    }

    public static Map<ConsentItem, ConsentRecord> latestByItem(List<ConsentRecord> records) {
        return records.stream()
                .collect(Collectors.toMap(
                        ConsentRecord::getItem,
                        record -> record,
                        (a, b) -> a.getRespondedAt().isAfter(b.getRespondedAt()) ? a : b
                ));
    }
}
