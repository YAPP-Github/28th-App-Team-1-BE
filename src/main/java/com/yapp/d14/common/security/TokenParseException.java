package com.yapp.d14.common.security;

import lombok.Getter;

@Getter
public class TokenParseException extends RuntimeException {

    private final int status;
    private final String code;
    private final String message;

    public TokenParseException(int status, String code, String message) {
        super(message);
        this.status = status;
        this.code = code;
        this.message = message;
    }
}
