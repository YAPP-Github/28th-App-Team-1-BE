package com.yapp.d14.portfolio.application.port.in;

import com.yapp.d14.portfolio.application.command.PortfolioRegisterCommand;

public interface PortfolioRegisterUseCase {

    PortfolioRegisterResult register(PortfolioRegisterCommand command);
}
