package com.yapp.d14.portfolio.application.port.in;

import com.yapp.d14.portfolio.application.command.PortfolioRegisterCommand;
import com.yapp.d14.portfolio.application.port.in.result.PortfolioRegisterResult;

public interface PortfolioRegisterUseCase {

    PortfolioRegisterResult register(PortfolioRegisterCommand command);
}
