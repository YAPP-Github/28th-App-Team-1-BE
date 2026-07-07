package com.yapp.d14.portfolio.application.port.out;

import java.util.UUID;

public interface PortfolioProcessor {

    void process(UUID portfolioId);
}
