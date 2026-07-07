package com.yapp.d14.portfolio.application.port.in;

import com.yapp.d14.portfolio.application.port.in.result.PortfolioSummary;

import java.util.List;
import java.util.UUID;

public interface PortfolioListUseCase {

    List<PortfolioSummary> getList(UUID userId);
}
