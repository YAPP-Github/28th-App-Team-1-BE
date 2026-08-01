package com.yapp.d14.consent.adapter.in.web;

import com.yapp.d14.consent.domain.ConsentItem;
import com.yapp.d14.consent.exception.ConsentErrorCode;
import com.yapp.d14.consent.exception.ConsentException;

public final class ConsentItemParser {

    private ConsentItemParser() {
    }

    public static ConsentItem parse(String rawItem) {
        try {
            return ConsentItem.valueOf(rawItem);
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new ConsentException(ConsentErrorCode.INVALID_CONSENT_ITEM);
        }
    }
}
