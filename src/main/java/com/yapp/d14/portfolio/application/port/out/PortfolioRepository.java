package com.yapp.d14.portfolio.application.port.out;

import com.yapp.d14.portfolio.domain.Portfolio;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PortfolioRepository {

    Portfolio save(Portfolio portfolio);

    boolean existsByUserId(UUID userId);

    Optional<Portfolio> findById(UUID id);

    List<Portfolio> findAllByUserId(UUID userId);

    void deleteById(UUID id);
}
