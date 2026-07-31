package com.yapp.d14.consent.application.service;

import com.yapp.d14.consent.application.port.out.ConsentRecordRepository;
import com.yapp.d14.consent.domain.ConsentItem;
import com.yapp.d14.consent.domain.ConsentRecord;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class PendingConsentItemsQueryServiceTest {

    @Mock
    private ConsentRecordRepository consentRecordRepository;

    @InjectMocks
    private PendingConsentItemsQueryService service;

    private final UUID userId = UUID.randomUUID();

    @Test
    void 동의_이력이_없으면_필수_항목_전체가_반환된다() {
        given(consentRecordRepository.findAllByUserId(userId)).willReturn(List.of());

        List<ConsentItem> pending = service.getPendingItems(userId);

        assertThat(pending).containsExactlyInAnyOrderElementsOf(ConsentItem.requiredItems());
    }

    @Test
    void 필수_항목이_전부_현행_버전이면_빈_목록이다() {
        List<ConsentRecord> records = ConsentItem.requiredItems().stream()
                .map(item -> ConsentRecord.create(userId, item, true))
                .toList();
        given(consentRecordRepository.findAllByUserId(userId)).willReturn(records);

        assertThat(service.getPendingItems(userId)).isEmpty();
    }

    @Test
    void 구버전으로_동의한_항목만_반환된다() {
        ConsentRecord stale = ConsentRecord.of(userId, ConsentItem.TERMS_OF_SERVICE, 0, true, LocalDateTime.now());
        List<ConsentRecord> records = ConsentItem.requiredItems().stream()
                .filter(item -> item != ConsentItem.TERMS_OF_SERVICE)
                .map(item -> ConsentRecord.create(userId, item, true))
                .collect(java.util.stream.Collectors.toCollection(java.util.ArrayList::new));
        records.add(stale);
        given(consentRecordRepository.findAllByUserId(userId)).willReturn(records);

        assertThat(service.getPendingItems(userId)).containsExactly(ConsentItem.TERMS_OF_SERVICE);
    }
}
