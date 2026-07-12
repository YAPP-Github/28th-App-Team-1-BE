package com.yapp.d14.portfolio.adapter.out.integration.vector;

import com.yapp.d14.portfolio.application.port.out.PortfolioEmbeddingStore;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

@Component
@RequiredArgsConstructor
class PgVectorPortfolioEmbeddingStoreAdapter implements PortfolioEmbeddingStore {

    private static final String METADATA_PORTFOLIO_ID = "portfolioId";
    private static final String METADATA_USER_ID = "userId";
    private static final String METADATA_FILE_NAME = "fileName";
    private static final String METADATA_CHUNK_INDEX = "chunkIndex";

    // 너무 짧은 문장 묶음(소제목 한 줄 등)이 각자 임베딩되지 않도록 이 길이가 될 때까지 다음 문장과 합친다.
    private static final int MIN_SECTION_LENGTH = 350;
    // 섹션이 이보다 길면 유사도와 무관하게 강제로 분할한다(임베딩 입력 한도 대비 1차 방어선).
    private static final int MAX_SECTION_LENGTH = 2000;
    // 인접 문장 임베딩 간 코사인 유사도가 이 값 미만이면 주제가 바뀐 것으로 보고 섹션을 나눈다.
    // 실제 포트폴리오 데이터로 검증된 값이 아닌 초기 추정치 — 추후 조정이 필요할 수 있다.
    private static final double SIMILARITY_THRESHOLD = 0.75;

    // PDF 추출 과정에서 뒤섞인 줄바꿈을 되돌리기 위해 공백을 하나로 정규화한다.
    private static final Pattern WHITESPACE = Pattern.compile("\\s+");
    // 정규화된 텍스트를 문장 단위(.!? 뒤 공백)로 분리한다. 완벽한 문장 경계 인식기는 아니다.
    private static final Pattern SENTENCE_BOUNDARY = Pattern.compile("(?<=[.!?])\\s+");

    private final VectorStore vectorStore;
    private final EmbeddingModel embeddingModel;
    private final TokenTextSplitter tokenTextSplitter = new TokenTextSplitter();

    @Override
    public void save(UUID portfolioId, UUID userId, String fileName, String text) {
        Map<String, Object> baseMetadata = Map.of(
                METADATA_PORTFOLIO_ID, portfolioId.toString(),
                METADATA_USER_ID, userId.toString(),
                METADATA_FILE_NAME, fileName
        );

        List<Document> chunks = splitIntoSections(text).stream()
                .flatMap(section -> splitIfTooLong(section).stream())
                .map(section -> new Document(section, new HashMap<>(baseMetadata)))
                .toList();

        for (int i = 0; i < chunks.size(); i++) {
            chunks.get(i).getMetadata().put(METADATA_CHUNK_INDEX, i);
        }

        vectorStore.add(chunks);
    }

    private List<String> splitIntoSections(String text) {
        String normalized = WHITESPACE.matcher(text.trim()).replaceAll(" ").trim();
        if (normalized.isEmpty()) {
            return List.of();
        }

        List<String> sentences = splitIntoSentences(normalized);
        if (sentences.size() <= 1) {
            return sentences;
        }

        List<float[]> embeddings = embeddingModel.embed(sentences);

        List<String> sections = new ArrayList<>();
        StringBuilder buffer = new StringBuilder(sentences.get(0));
        for (int i = 1; i < sentences.size(); i++) {
            String sentence = sentences.get(i);
            double similarity = cosineSimilarity(embeddings.get(i - 1), embeddings.get(i));
            int prospectiveLength = buffer.length() + 1 + sentence.length();

            boolean topicShift = buffer.length() >= MIN_SECTION_LENGTH && similarity < SIMILARITY_THRESHOLD;
            boolean hardCapExceeded = prospectiveLength > MAX_SECTION_LENGTH;

            if (topicShift || hardCapExceeded) {
                sections.add(buffer.toString());
                buffer.setLength(0);
                buffer.append(sentence);
            } else {
                buffer.append(' ').append(sentence);
            }
        }
        if (!buffer.isEmpty()) {
            sections.add(buffer.toString());
        }
        return sections;
    }

    private List<String> splitIntoSentences(String normalizedText) {
        return Arrays.stream(SENTENCE_BOUNDARY.split(normalizedText))
                .map(String::trim)
                .filter(sentence -> !sentence.isEmpty())
                .toList();
    }

    private double cosineSimilarity(float[] a, float[] b) {
        double dot = 0;
        double normA = 0;
        double normB = 0;
        for (int i = 0; i < a.length; i++) {
            dot += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }
        if (normA == 0 || normB == 0) {
            return 0.0;
        }
        return dot / (Math.sqrt(normA) * Math.sqrt(normB));
    }

    private List<String> splitIfTooLong(String section) {
        if (section.length() <= MAX_SECTION_LENGTH) {
            return List.of(section);
        }
        return tokenTextSplitter.apply(List.of(new Document(section))).stream()
                .map(Document::getText)
                .toList();
    }

    @Override
    public void deleteByPortfolioId(UUID portfolioId) {
        FilterExpressionBuilder filterBuilder = new FilterExpressionBuilder();
        vectorStore.delete(filterBuilder.eq(METADATA_PORTFOLIO_ID, portfolioId.toString()).build());
    }

    @Override
    public Optional<Double> findTopSimilarityScore(UUID portfolioId, String queryText) {
        FilterExpressionBuilder filterBuilder = new FilterExpressionBuilder();
        SearchRequest searchRequest = SearchRequest.builder()
                .query(queryText)
                .topK(1)
                .filterExpression(filterBuilder.eq(METADATA_PORTFOLIO_ID, portfolioId.toString()).build())
                .build();

        List<Document> results = vectorStore.similaritySearch(searchRequest);
        return results.isEmpty() ? Optional.empty() : Optional.ofNullable(results.get(0).getScore());
    }

    @Override
    public List<String> findTopChunks(UUID portfolioId, String queryText, int topK) {
        FilterExpressionBuilder filterBuilder = new FilterExpressionBuilder();
        SearchRequest searchRequest = SearchRequest.builder()
                .query(queryText)
                .topK(topK)
                .filterExpression(filterBuilder.eq(METADATA_PORTFOLIO_ID, portfolioId.toString()).build())
                .build();

        return vectorStore.similaritySearch(searchRequest).stream()
                .map(Document::getText)
                .toList();
    }
}
