package com.yapp.d14.portfolio.adapter.out.integration.vector;

import com.yapp.d14.portfolio.application.port.out.PortfolioEmbeddingStore;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 포트폴리오 RAG 검색 품질(Recall@K, MRR)을 실제 임베딩·pgvector로 측정하는 하네스.
 * 로컬 Postgres(docker compose)와 OPENAI_API_KEY가 필요하며 실제 임베딩 API 호출로 비용이 발생한다.
 * ./gradlew ragEvalTest 로만 실행되고 기본 test/CI에는 포함되지 않는다.
 */
@Tag("rag-eval")
@SpringBootTest
class PortfolioRagEvalTest {

    private static final Logger log = LoggerFactory.getLogger(PortfolioRagEvalTest.class);

    // InterviewSessionPreloadService.TOP_K 와 동일하게 맞춰 실제 서비스 조회 조건을 재현한다.
    private static final int TOP_K = 10;
    // 코퍼스가 TOP_K보다 작으면 recall@TOP_K가 항상 1.0이 되어 변별력이 없으므로, 더 엄격한 컷오프도 함께 본다.
    private static final int STRICT_K = 3;

    private static final String FAKE_GOLDEN_SET_RESOURCE = "rag-eval/fake-portfolio-golden-set.json";
    private static final Path REAL_GOLDEN_SET_PATH = Path.of("src/test/resources/rag-eval/local/real-portfolio-golden-set.json");

    @Autowired
    private PortfolioEmbeddingStore portfolioEmbeddingStore;

    @Test
    void 가짜_포트폴리오_골든셋으로_검색_품질을_측정한다() {
        PortfolioRagGoldenSet goldenSet = PortfolioRagGoldenSet.fromClasspath(FAKE_GOLDEN_SET_RESOURCE);
        runEval(goldenSet);
    }

    @Test
    void 실제_포트폴리오_골든셋으로_검색_품질을_측정한다() {
        Assumptions.assumeTrue(Files.exists(REAL_GOLDEN_SET_PATH),
                "로컬 전용 골든셋이 없어 스킵합니다: " + REAL_GOLDEN_SET_PATH);

        PortfolioRagGoldenSet goldenSet = PortfolioRagGoldenSet.fromFile(REAL_GOLDEN_SET_PATH);
        runEval(goldenSet);
    }

    private void runEval(PortfolioRagGoldenSet goldenSet) {
        UUID portfolioId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Map<String, String> passageTextsById = goldenSet.passageTextsById();

        try {
            portfolioEmbeddingStore.save(portfolioId, userId, "eval-resume.pdf", goldenSet.joinedPortfolioText());

            List<PortfolioRagEvalMetrics.QueryResult> results = goldenSet.queries().stream()
                    .map(query -> {
                        List<String> relevantTexts = query.relevantPassageIds().stream()
                                .map(passageTextsById::get)
                                .toList();
                        List<String> retrievedChunks = portfolioEmbeddingStore.findTopChunks(portfolioId, query.text(), TOP_K);

                        PortfolioRagEvalMetrics.QueryResult result =
                                PortfolioRagEvalMetrics.evaluate(query.text(), relevantTexts, retrievedChunks, STRICT_K);
                        log.info("[RAG EVAL][{}] query=\"{}\" recall@{}={} recall@{}={} reciprocalRank={}",
                                goldenSet.label(), query.text(), STRICT_K, result.recallAtStrictK(),
                                TOP_K, result.recallAtFullK(), result.reciprocalRank());
                        return result;
                    })
                    .toList();

            PortfolioRagEvalMetrics.Summary summary = PortfolioRagEvalMetrics.summarize(results);
            log.info("[RAG EVAL][{}] ===== meanRecall@{}={} meanRecall@{}={} MRR={} (쿼리 {}건) =====",
                    goldenSet.label(), STRICT_K, summary.meanRecallAtStrictK(),
                    TOP_K, summary.meanRecallAtFullK(), summary.mrr(), results.size());

            assertThat(summary.meanRecallAtStrictK()).isBetween(0.0, 1.0);
            assertThat(summary.meanRecallAtFullK()).isBetween(0.0, 1.0);
            assertThat(summary.mrr()).isBetween(0.0, 1.0);
        } finally {
            portfolioEmbeddingStore.deleteByPortfolioId(portfolioId);
        }
    }
}
