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
import java.util.Optional;
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

    // PgVectorPortfolioEmbeddingStoreAdapter.RETRIEVAL_SIMILARITY_THRESHOLD 와 반드시 같은 값이어야 한다.
    private static final double RETRIEVAL_SIMILARITY_THRESHOLD = 0.4;

    // 이 커밋 시점(#75)에 fake-portfolio-golden-set.json으로 실측한 값
    // (meanRecall@3 = meanRecall@10 = MRR ≈ 0.4545)에 약간의 여유를 둔 회귀 방지 하한선.
    // RETRIEVAL_SIMILARITY_THRESHOLD나 청킹 로직 변경으로 이 값 아래로 떨어지면 실패한다.
    private static final double MIN_MEAN_RECALL_AT_STRICT_K = 0.4;
    private static final double MIN_MEAN_RECALL_AT_FULL_K = 0.4;
    private static final double MIN_MRR = 0.4;

    @Autowired
    private PortfolioEmbeddingStore portfolioEmbeddingStore;

    @Test
    void 가짜_포트폴리오_골든셋으로_검색_품질을_측정한다() {
        PortfolioRagGoldenSet goldenSet = PortfolioRagGoldenSet.fromClasspath(FAKE_GOLDEN_SET_RESOURCE);
        // 대표 골든셋(커밋되어 CI에서도 재현 가능)이므로 실측 기반 회귀 하한선을 적용한다.
        runEval(goldenSet, MIN_MEAN_RECALL_AT_STRICT_K, MIN_MEAN_RECALL_AT_FULL_K, MIN_MRR);
    }

    @Test
    void 실제_포트폴리오_골든셋으로_검색_품질을_측정한다() {
        Assumptions.assumeTrue(Files.exists(REAL_GOLDEN_SET_PATH),
                "로컬 전용 골든셋이 없어 스킵합니다: " + REAL_GOLDEN_SET_PATH);

        PortfolioRagGoldenSet goldenSet = PortfolioRagGoldenSet.fromFile(REAL_GOLDEN_SET_PATH);
        // 로컬 전용 데이터라 실측 기반 하한선이 없으므로 0~1 범위만 확인한다.
        runEval(goldenSet, 0.0, 0.0, 0.0);
    }

    @Test
    void 임계값_미만_상위_유사도_쿼리는_topK_여유가_있어도_결과가_비어있다() {
        PortfolioRagGoldenSet goldenSet = PortfolioRagGoldenSet.fromClasspath(FAKE_GOLDEN_SET_RESOURCE);
        UUID portfolioId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        try {
            portfolioEmbeddingStore.save(portfolioId, userId, "eval-resume.pdf", goldenSet.joinedPortfolioText());

            int cutOffByThresholdCount = 0;
            for (PortfolioRagGoldenSet.Query query : goldenSet.queries()) {
                Optional<Double> topScore = portfolioEmbeddingStore.findTopSimilarityScore(portfolioId, query.text());
                List<String> retrievedChunks = portfolioEmbeddingStore.findTopChunks(portfolioId, query.text(), TOP_K);

                assertThat(topScore).isPresent();
                if (topScore.get() < RETRIEVAL_SIMILARITY_THRESHOLD) {
                    // 코퍼스 전체를 통틀어 가장 유사한 청크조차 임계값 미만이라면, topK 자리가 남아도 결과가 비어야 한다.
                    assertThat(retrievedChunks)
                            .as("query=\"%s\" topScore=%s", query.text(), topScore.get())
                            .isEmpty();
                    cutOffByThresholdCount++;
                } else {
                    assertThat(retrievedChunks)
                            .as("query=\"%s\" topScore=%s", query.text(), topScore.get())
                            .isNotEmpty();
                }
            }
            log.info("[RAG EVAL][threshold-boundary] 임계값({})에 걸려 결과가 비워진 쿼리: {}/{}건",
                    RETRIEVAL_SIMILARITY_THRESHOLD, cutOffByThresholdCount, goldenSet.queries().size());
        } finally {
            portfolioEmbeddingStore.deleteByPortfolioId(portfolioId);
        }
    }

    private void runEval(PortfolioRagGoldenSet goldenSet, double minMeanRecallAtStrictK,
                          double minMeanRecallAtFullK, double minMrr) {
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

            assertThat(summary.meanRecallAtStrictK()).isBetween(minMeanRecallAtStrictK, 1.0);
            assertThat(summary.meanRecallAtFullK()).isBetween(minMeanRecallAtFullK, 1.0);
            assertThat(summary.mrr()).isBetween(minMrr, 1.0);
        } finally {
            portfolioEmbeddingStore.deleteByPortfolioId(portfolioId);
        }
    }
}
