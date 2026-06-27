package com.yapp.d14.common.response;

public record ErrorResponse(
        boolean success,
        String code,
        String message
) {

    public static ErrorResponse of(String code, String message) {
        return new ErrorResponse(false, code, message);
    }
}
