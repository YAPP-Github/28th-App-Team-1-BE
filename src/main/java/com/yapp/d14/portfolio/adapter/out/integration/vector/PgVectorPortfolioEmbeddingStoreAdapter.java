package com.yapp.d14.portfolio.adapter.out.integration.vector;

import com.yapp.d14.portfolio.application.port.out.PortfolioEmbeddingStore;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
class PgVectorPortfolioEmbeddingStoreAdapter implements PortfolioEmbeddingStore {

    private static final String METADATA_PORTFOLIO_ID = "portfolioId";
    private static final String METADATA_USER_ID = "userId";
    private static final String METADATA_FILE_NAME = "fileName";
    private static final String METADATA_CHUNK_INDEX = "chunkIndex";

    private final VectorStore vectorStore;
    private final TokenTextSplitter textSplitter = new TokenTextSplitter();

    @Override
    public void save(UUID portfolioId, UUID userId, String fileName, String text) {
        Document sourceDocument = new Document(text, Map.of(
                METADATA_PORTFOLIO_ID, portfolioId.toString(),
                METADATA_USER_ID, userId.toString(),
                METADATA_FILE_NAME, fileName
        ));

        List<Document> chunks = textSplitter.apply(List.of(sourceDocument));
        for (int i = 0; i < chunks.size(); i++) {
            chunks.get(i).getMetadata().put(METADATA_CHUNK_INDEX, i);
        }

        vectorStore.add(chunks);
    }

    @Override
    public void deleteByPortfolioId(UUID portfolioId) {
        FilterExpressionBuilder filterBuilder = new FilterExpressionBuilder();
        vectorStore.delete(filterBuilder.eq(METADATA_PORTFOLIO_ID, portfolioId.toString()).build());
    }
}
