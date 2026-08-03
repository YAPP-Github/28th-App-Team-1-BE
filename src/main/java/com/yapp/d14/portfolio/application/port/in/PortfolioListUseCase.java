package com.yapp.d14.portfolio.application.port.in;

import com.yapp.d14.portfolio.application.port.in.result.PortfolioListResult;

import java.util.UUID;

public interface PortfolioListUseCase {

    PortfolioListResult getList(UUID userId);
}
