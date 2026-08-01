package com.yapp.d14.consent.application.port.out;

import com.yapp.d14.consent.domain.ConsentDocument;
import com.yapp.d14.consent.domain.ConsentItem;

import java.util.Optional;

public interface ConsentDocumentRepository {

    Optional<ConsentDocument> findByItemAndVersion(ConsentItem item, int version);

    boolean existsByItemAndVersion(ConsentItem item, int version);
}
