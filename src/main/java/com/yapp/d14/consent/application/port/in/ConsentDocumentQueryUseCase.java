package com.yapp.d14.consent.application.port.in;

import com.yapp.d14.consent.domain.ConsentDocument;
import com.yapp.d14.consent.domain.ConsentItem;

public interface ConsentDocumentQueryUseCase {

    ConsentDocument getDocument(ConsentItem item, int version);

    boolean hasDocument(ConsentItem item, int version);
}
