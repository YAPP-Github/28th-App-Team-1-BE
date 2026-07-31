package com.yapp.d14.consent.application.service;

import com.yapp.d14.consent.application.port.out.ConsentRecordRepository;
import com.yapp.d14.consent.domain.ConsentItem;
import com.yapp.d14.consent.domain.ConsentRecord;
import com.yapp.d14.consent.domain.RequiredConsentStatus;
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
class RequiredConsentStatusQueryServiceTest {

    @Mock
    private ConsentRecordRepository consentRecordRepository;

    @InjectMocks
    private RequiredConsentStatusQueryService service;

    private final UUID userId = UUID.randomUUID();

    @Test
    void 동의_이력이_없으면_NOT_SUBMITTED다() {
        given(consentRecordRepository.findAllByUserId(userId)).willReturn(List.of());

        assertThat(service.getStatus(userId)).isEqualTo(RequiredConsentStatus.NOT_SUBMITTED);
    }

    @Test
    void 필수_항목이_전부_현행_버전으로_존재하면_UP_TO_DATE다() {
        List<ConsentRecord> records = ConsentItem.requiredItems().stream()
                .map(item -> ConsentRecord.create(userId, item, true))
                .toList();
        given(consentRecordRepository.findAllByUserId(userId)).willReturn(records);

        assertThat(service.getStatus(userId)).isEqualTo(RequiredConsentStatus.UP_TO_DATE);
    }

    @Test
    void 필수_항목_중_하나라도_구버전이면_STALE이다() {
        List<ConsentRecord> records = ConsentItem.requiredItems().stream()
                .map(item -> ConsentRecord.create(userId, item, true))
                .collect(java.util.stream.Collectors.toCollection(java.util.ArrayList::new));
        records.set(0, ConsentRecord.of(userId, records.get(0).getItem(), 0, true, LocalDateTime.now()));
        given(consentRecordRepository.findAllByUserId(userId)).willReturn(records);

        assertThat(service.getStatus(userId)).isEqualTo(RequiredConsentStatus.STALE);
    }
}
