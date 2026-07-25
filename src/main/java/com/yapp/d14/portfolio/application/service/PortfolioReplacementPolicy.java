package com.yapp.d14.portfolio.application.service;

import lombok.experimental.UtilityClass;

import java.time.LocalDate;
import java.time.LocalDateTime;

@UtilityClass
class PortfolioReplacementPolicy {

    LocalDateTime currentMonthStart() {
        return LocalDate.now().withDayOfMonth(1).atStartOfDay();
    }

    LocalDateTime nextMonthStart() {
        return currentMonthStart().plusMonths(1);
    }
}
