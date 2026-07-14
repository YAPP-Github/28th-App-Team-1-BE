package com.yapp.d14.jd.application.command;

import com.yapp.d14.jd.exception.JdErrorCode;
import com.yapp.d14.jd.exception.JdException;

public record JdValidateCommand(String jdUrl) {

    public JdValidateCommand {
        if (jdUrl == null || jdUrl.isBlank()) {
            throw new JdException(JdErrorCode.INVALID_JD_URL);
        }
    }
}
