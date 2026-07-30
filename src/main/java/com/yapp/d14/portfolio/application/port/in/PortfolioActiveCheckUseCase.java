package com.yapp.d14.portfolio.application.port.in;

import java.util.UUID;

public interface PortfolioActiveCheckUseCase {

    /** 존재하고 삭제되지 않았으면 true. 존재하지 않아도(id가 null이거나 조회 실패) false. */
    boolean isActive(UUID portfolioId);
}
