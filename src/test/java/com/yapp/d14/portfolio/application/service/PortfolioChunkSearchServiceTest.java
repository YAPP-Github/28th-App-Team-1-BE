package com.yapp.d14.portfolio.application.service;

import com.yapp.d14.portfolio.application.port.in.result.PortfolioChunkResult;
import com.yapp.d14.portfolio.application.port.out.PortfolioEmbeddingStore;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class PortfolioChunkSearchServiceTest {

    @Mock
    private PortfolioEmbeddingStore portfolioEmbeddingStore;

    @InjectMocks
    private PortfolioChunkSearchService service;

    private final UUID portfolioId = UUID.randomUUID();

    @Test
    void 조회된_청크를_결과_리스트로_변환한다() {
        given(portfolioEmbeddingStore.findTopChunks(portfolioId, "쿼리", 20))
                .willReturn(List.of("청크1", "청크2"));

        List<PortfolioChunkResult> results = service.searchChunks(portfolioId, "쿼리", 20);

        assertThat(results).extracting(PortfolioChunkResult::text).containsExactly("청크1", "청크2");
    }
}
