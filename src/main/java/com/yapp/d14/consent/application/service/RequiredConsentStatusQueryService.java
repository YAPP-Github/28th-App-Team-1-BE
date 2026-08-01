package com.yapp.d14.consent.application.service;

import com.yapp.d14.consent.application.port.in.RequiredConsentStatusQueryUseCase;
import com.yapp.d14.consent.application.port.out.ConsentRecordRepository;
import com.yapp.d14.consent.domain.ConsentItem;
import com.yapp.d14.consent.domain.ConsentRecord;
import com.yapp.d14.consent.domain.RequiredConsentStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
class RequiredConsentStatusQueryService implements RequiredConsentStatusQueryUseCase {

    private final ConsentRecordRepository consentRecordRepository;

    @Override
    public RequiredConsentStatus getStatus(UUID userId) {
        List<ConsentRecord> records = consentRecordRepository.findAllByUserId(userId);
        Map<ConsentItem, ConsentRecord> latestByItem = ConsentRecord.latestByItem(records);
        return RequiredConsentStatus.evaluate(latestByItem);
    }
}
