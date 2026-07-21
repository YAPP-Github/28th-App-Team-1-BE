package com.yapp.d14.portfolio.adapter.out.persistence;

import com.yapp.d14.portfolio.application.port.out.PortfolioRepository;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;

// acquireRegistrationLock의 pg_advisory_xact_lock 네이티브 쿼리가 실제 Postgres 드라이버·캐스팅에서
// 문법 오류 없이 동작하는지 검증한다(네이티브 쿼리는 Hibernate 부트스트랩 시점에 검증되지 않는다).
@Tag("integration")
@SpringBootTest
class PortfolioPersistenceAdapterIntegrationTest {

    @Autowired
    private PortfolioRepository portfolioRepository;

    @Test
    void 등록_락을_정상적으로_획득한다() {
        assertThatCode(() -> portfolioRepository.acquireRegistrationLock(UUID.randomUUID()))
                .doesNotThrowAnyException();
    }
}
