package com.yapp.d14.portfolio.application.port.in;

import java.util.List;
import java.util.UUID;

public interface PortfolioListUseCase {

    List<PortfolioSummary> getList(UUID userId);
}
