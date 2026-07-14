package com.yapp.d14.ticket.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum TicketReservationStatus {

    HELD("보류"),
    COMMITTED("확정"),
    RELEASED("반환");

    private final String label;
}
