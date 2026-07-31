package com.yapp.d14.consent.application.port.out;

import com.yapp.d14.consent.domain.ConsentRecord;

import java.util.List;
import java.util.UUID;

public interface ConsentRecordRepository {

    ConsentRecord save(ConsentRecord consentRecord);

    List<ConsentRecord> findAllByUserId(UUID userId);
}
