package com.yapp.d14.portfolio.adapter.out.persistence;

import com.yapp.d14.portfolio.application.port.out.PortfolioRepository;
import com.yapp.d14.portfolio.domain.Portfolio;
import com.yapp.d14.portfolio.domain.PortfolioStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

// acquireRegistrationLock의 pg_advisory_xact_lock 네이티브 쿼리가 실제 Postgres 드라이버·캐스팅에서
// 문법 오류 없이 동작하는지 검증한다(네이티브 쿼리는 Hibernate 부트스트랩 시점에 검증되지 않는다).
@Tag("integration")
@SpringBootTest
class PortfolioPersistenceAdapterIntegrationTest {

    @Autowired
    private PortfolioRepository portfolioRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private UUID userId;

    @AfterEach
    void cleanUp() {
        if (userId == null) {
            return;
        }
        jdbcTemplate.update("DELETE FROM portfolios WHERE user_id = ?", userId);
    }

    @Test
    void 등록_락을_정상적으로_획득한다() {
        assertThatCode(() -> portfolioRepository.acquireRegistrationLock(UUID.randomUUID()))
                .doesNotThrowAnyException();
    }

    @Test
    void 등록은_월_경계_이전이고_완료가_월_경계_이후인_교체건도_해당_달_이력으로_잡힌다() {
        userId = UUID.randomUUID();
        LocalDateTime monthStart = LocalDateTime.of(2024, 2, 1, 0, 0);
        Portfolio monthBoundaryReplacement = Portfolio.of(
                UUID.randomUUID(), userId, "resume.pdf", 100L, 5, "s3-key",
                PortfolioStatus.READY, "완료",
                monthStart.minusHours(1), monthStart.plusMinutes(30),
                true, false, null
        );
        portfolioRepository.save(monthBoundaryReplacement);

        boolean result = portfolioRepository.existsReplacementSince(userId, monthStart);

        assertThat(result).isTrue();
    }
}
