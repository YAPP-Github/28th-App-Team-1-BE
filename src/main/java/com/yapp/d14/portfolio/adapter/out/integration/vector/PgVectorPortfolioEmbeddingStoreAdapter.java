package com.yapp.d14.portfolio.adapter.out.integration.vector;

import com.yapp.d14.portfolio.application.port.out.PortfolioEmbeddingStore;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.document.Document;
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

    // 빈 줄로 문단을 구분해 1차로 내용 단위(섹션)를 나눈다.
    private static final Pattern BLANK_LINE = Pattern.compile("\\n\\s*\\n+");
    // 너무 짧은 문단(소제목 한 줄 등)이 각자 임베딩되지 않도록 이 길이가 될 때까지 다음 문단과 합친다.
    private static final int MIN_SECTION_LENGTH = 350;
    // 섹션이 이보다 길면 임베딩 입력 한도를 위해 토큰 단위로 추가 분할한다.
    private static final int MAX_SECTION_LENGTH = 2000;

    private final VectorStore vectorStore;
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
        List<String> paragraphs = Arrays.stream(BLANK_LINE.split(text.trim()))
                .map(String::trim)
                .filter(paragraph -> !paragraph.isEmpty())
                .toList();

        List<String> sections = new ArrayList<>();
        StringBuilder buffer = new StringBuilder();
        for (String paragraph : paragraphs) {
            if (!buffer.isEmpty()) {
                buffer.append("\n\n");
            }
            buffer.append(paragraph);
            if (buffer.length() >= MIN_SECTION_LENGTH) {
                sections.add(buffer.toString());
                buffer.setLength(0);
            }
        }
        if (!buffer.isEmpty()) {
            sections.add(buffer.toString());
        }
        return sections;
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
}
