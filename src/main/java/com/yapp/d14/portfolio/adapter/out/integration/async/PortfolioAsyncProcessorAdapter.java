package com.yapp.d14.portfolio.adapter.out.integration.async;

import com.yapp.d14.portfolio.application.port.out.PortfolioProcessor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Slf4j
@Component
class PortfolioAsyncProcessorAdapter implements PortfolioProcessor {

    @Override
    @Async("portfolioTaskExecutor")
    public void process(UUID portfolioId) {
        // S3 업로드 → Tika 파싱 → 임베딩 파이프라인은 별도 이슈에서 구현 예정
        log.info("portfolio async processing triggered: portfolioId={}", portfolioId);
    }
}
