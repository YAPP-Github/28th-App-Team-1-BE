package com.yapp.d14.portfolio.application.port.in.result;

import java.time.LocalDateTime;
import java.util.UUID;

public record PortfolioDeleteResult(
        UUID portfolioId,
        LocalDateTime deletedAt
) {
}
