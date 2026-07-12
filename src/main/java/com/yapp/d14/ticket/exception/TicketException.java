package com.yapp.d14.ticket.exception;

import com.yapp.d14.common.exception.BusinessException;

public class TicketException extends BusinessException {

    public TicketException(TicketErrorCode errorCode) {
        super(errorCode);
    }
}
