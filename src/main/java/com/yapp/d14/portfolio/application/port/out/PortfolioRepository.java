package com.yapp.d14.portfolio.application.port.out;

import com.yapp.d14.portfolio.domain.Portfolio;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PortfolioRepository {

    Portfolio save(Portfolio portfolio);

    boolean existsActiveByUserId(UUID userId);

    boolean existsAnyByUserId(UUID userId);

    boolean existsReplacementSince(UUID userId, LocalDateTime since);

    Optional<Portfolio> findById(UUID id);

    List<Portfolio> findAllActiveByUserId(UUID userId);
}
