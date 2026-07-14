package com.yapp.d14.ticket.exception;

import com.yapp.d14.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@RequiredArgsConstructor
public enum TicketErrorCode implements ErrorCode {

    NO_REMAINING_TICKET(HttpStatus.FORBIDDEN, "NO_REMAINING_TICKET", "남은 이용권이 없어요.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;

    @Override
    public HttpStatus getHttpStatus() { return httpStatus; }

    @Override
    public String getCode() { return code; }

    @Override
    public String getMessage() { return message; }
}
