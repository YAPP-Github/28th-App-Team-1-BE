package com.yapp.d14.jd.application.command;

import com.yapp.d14.jd.exception.JdErrorCode;
import com.yapp.d14.jd.exception.JdException;

import java.util.UUID;

public record JdValidateCommand(String jdUrl, UUID userId) {

    public JdValidateCommand {
        if (jdUrl == null || jdUrl.isBlank()) {
            throw new JdException(JdErrorCode.INVALID_JD_URL);
        }
    }
}
