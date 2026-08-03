package com.yapp.d14.portfolio.application.port.in.result;

import java.time.LocalDateTime;
import java.util.List;

public record PortfolioListResult(
        boolean replaceAvailable,
        LocalDateTime nextAvailableAt,
        boolean deleteAvailable,
        LocalDateTime nextDeleteAvailableAt,
        List<PortfolioSummary> portfolios
) {
}
