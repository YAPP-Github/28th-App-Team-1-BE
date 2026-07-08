package com.yapp.d14.portfolio.adapter.out.integration.vector;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PgVectorPortfolioEmbeddingStoreAdapterTest {

    @Mock
    private VectorStore vectorStore;

    @InjectMocks
    private PgVectorPortfolioEmbeddingStoreAdapter adapter;

    @Captor
    private ArgumentCaptor<List<Document>> documentsCaptor;

    @Test
    void 빈_줄로_구분된_짧은_문단들은_최소_길이가_될_때까지_하나의_섹션으로_합쳐진다() {
        UUID portfolioId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        String shortParagraph1 = "프로젝트 A 개요";
        String shortParagraph2 = "담당 업무 요약";
        String text = shortParagraph1 + "\n\n" + shortParagraph2;

        adapter.save(portfolioId, userId, "resume.pdf", text);

        verify(vectorStore).add(documentsCaptor.capture());
        List<Document> chunks = documentsCaptor.getValue();
        assertThat(chunks).hasSize(1);
        assertThat(chunks.get(0).getText()).contains(shortParagraph1, shortParagraph2);
    }

    @Test
    void 최소_길이를_넘는_문단은_별도_섹션으로_분리된다() {
        UUID portfolioId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        String longParagraph = "가".repeat(400);
        String anotherParagraph = "나".repeat(400);
        String text = longParagraph + "\n\n" + anotherParagraph;

        adapter.save(portfolioId, userId, "resume.pdf", text);

        verify(vectorStore).add(documentsCaptor.capture());
        List<Document> chunks = documentsCaptor.getValue();
        assertThat(chunks).hasSize(2);
        assertThat(chunks.get(0).getText()).isEqualTo(longParagraph);
        assertThat(chunks.get(1).getText()).isEqualTo(anotherParagraph);
    }

    @Test
    void 각_청크에_portfolioId_userId_fileName_chunkIndex_메타데이터가_채워진다() {
        UUID portfolioId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        String longParagraph = "가".repeat(400);
        String anotherParagraph = "나".repeat(400);
        String text = longParagraph + "\n\n" + anotherParagraph;

        adapter.save(portfolioId, userId, "resume.pdf", text);

        verify(vectorStore).add(documentsCaptor.capture());
        List<Document> chunks = documentsCaptor.getValue();

        assertThat(chunks.get(0).getMetadata())
                .containsEntry("portfolioId", portfolioId.toString())
                .containsEntry("userId", userId.toString())
                .containsEntry("fileName", "resume.pdf")
                .containsEntry("chunkIndex", 0);
        assertThat(chunks.get(1).getMetadata()).containsEntry("chunkIndex", 1);
    }

    @Test
    void 섹션이_최대_길이를_넘으면_토큰_단위로_추가_분할된다() {
        UUID portfolioId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        String hugeParagraph = "이것은 매우 긴 포트폴리오 설명 문장입니다. ".repeat(300);

        adapter.save(portfolioId, userId, "resume.pdf", hugeParagraph);

        verify(vectorStore).add(documentsCaptor.capture());
        List<Document> chunks = documentsCaptor.getValue();
        assertThat(chunks.size()).isGreaterThan(1);
    }

    @Test
    void portfolioId_기준으로_삭제_필터를_적용한다() {
        UUID portfolioId = UUID.randomUUID();

        adapter.deleteByPortfolioId(portfolioId);

        ArgumentCaptor<Filter.Expression> filterCaptor = ArgumentCaptor.forClass(Filter.Expression.class);
        verify(vectorStore).delete(filterCaptor.capture());
        assertThat(filterCaptor.getValue()).isNotNull();
    }
}
