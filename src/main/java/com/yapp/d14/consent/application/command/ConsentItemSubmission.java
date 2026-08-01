package com.yapp.d14.consent.application.command;

import com.yapp.d14.consent.domain.ConsentItem;

public record ConsentItemSubmission(ConsentItem item, int version, boolean agreed) {
}
