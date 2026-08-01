package com.yapp.d14.consent.application.service;

import com.yapp.d14.consent.application.port.in.ConsentDocumentQueryUseCase;
import com.yapp.d14.consent.application.port.out.ConsentDocumentRepository;
import com.yapp.d14.consent.domain.ConsentDocument;
import com.yapp.d14.consent.domain.ConsentItem;
import com.yapp.d14.consent.exception.ConsentErrorCode;
import com.yapp.d14.consent.exception.ConsentException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
class ConsentDocumentQueryService implements ConsentDocumentQueryUseCase {

    private final ConsentDocumentRepository consentDocumentRepository;

    @Override
    public ConsentDocument getDocument(ConsentItem item, int version) {
        return consentDocumentRepository.findByItemAndVersion(item, version)
                .orElseThrow(() -> new ConsentException(ConsentErrorCode.DOCUMENT_NOT_FOUND));
    }

    @Override
    public boolean hasDocument(ConsentItem item, int version) {
        return consentDocumentRepository.existsByItemAndVersion(item, version);
    }
}
