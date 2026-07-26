package com.yapp.d14.portfolio.adapter.out.integration.vector;

import java.util.List;

final class PortfolioRagEvalMetrics {

    private PortfolioRagEvalMetrics() {
    }

    record QueryResult(String queryText, double recallAtStrictK, double recallAtFullK, double reciprocalRank) {
    }

    record Summary(double meanRecallAtStrictK, double meanRecallAtFullK, double mrr) {
    }

    // 정답 passage 원문과 검색된 청크 원문이 서로를 포함하면(병합/분할 경계 차이를 흡수) 매칭된 것으로 본다.
    // strictK는 코퍼스가 topK(=fullK)보다 작아 recall@fullK가 항상 1.0이 되는 상황을 피하기 위한 더 엄격한 컷오프다.
    static QueryResult evaluate(String queryText, List<String> relevantPassageTexts,
                                 List<String> retrievedChunkTexts, int strictK) {
        if (relevantPassageTexts.isEmpty()) {
            return new QueryResult(queryText, 0.0, 0.0, 0.0);
        }

        List<String> strictTopChunks = retrievedChunkTexts.subList(0, Math.min(strictK, retrievedChunkTexts.size()));
        double recallAtStrictK = recall(relevantPassageTexts, strictTopChunks);
        double recallAtFullK = recall(relevantPassageTexts, retrievedChunkTexts);

        double reciprocalRank = 0.0;
        for (int rank = 0; rank < retrievedChunkTexts.size(); rank++) {
            String chunk = retrievedChunkTexts.get(rank);
            if (relevantPassageTexts.stream().anyMatch(passage -> hits(passage, chunk))) {
                reciprocalRank = 1.0 / (rank + 1);
                break;
            }
        }

        return new QueryResult(queryText, recallAtStrictK, recallAtFullK, reciprocalRank);
    }

    static Summary summarize(List<QueryResult> results) {
        double meanRecallAtStrictK = results.stream().mapToDouble(QueryResult::recallAtStrictK).average().orElse(0.0);
        double meanRecallAtFullK = results.stream().mapToDouble(QueryResult::recallAtFullK).average().orElse(0.0);
        double mrr = results.stream().mapToDouble(QueryResult::reciprocalRank).average().orElse(0.0);
        return new Summary(meanRecallAtStrictK, meanRecallAtFullK, mrr);
    }

    private static double recall(List<String> relevantPassageTexts, List<String> retrievedChunkTexts) {
        long foundCount = relevantPassageTexts.stream()
                .filter(passage -> retrievedChunkTexts.stream().anyMatch(chunk -> hits(passage, chunk)))
                .count();
        return (double) foundCount / relevantPassageTexts.size();
    }

    private static boolean hits(String passageText, String chunkText) {
        return chunkText.contains(passageText) || passageText.contains(chunkText);
    }
}
