package com.yapp.d14.portfolio.adapter.out.integration.vector;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PgVectorPortfolioEmbeddingStoreAdapterTest {

    @Mock
    private VectorStore vectorStore;

    @Mock
    private EmbeddingModel embeddingModel;

    @InjectMocks
    private PgVectorPortfolioEmbeddingStoreAdapter adapter;

    @Captor
    private ArgumentCaptor<List<Document>> documentsCaptor;

    @Captor
    private ArgumentCaptor<List<String>> sentencesCaptor;

    private static float[] vector(float... values) {
        return values;
    }

    private static String sentenceOfLength(char filler, int length) {
        return String.valueOf(filler).repeat(length - 1) + ".";
    }

    @Test
    void 유사한_문장들은_하나의_섹션으로_합쳐진다() {
        UUID portfolioId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        String s1 = "AWS Lambda로 이미지 리사이징을 자동화했습니다.";
        String s2 = "S3 업로드 이벤트를 트리거로 사용했습니다.";
        String s3 = "서버 비용을 크게 절감했습니다.";
        String text = s1 + " " + s2 + " " + s3;

        when(embeddingModel.embed(anyList())).thenReturn(List.of(
                vector(1f, 0f, 0f),
                vector(0.99f, 0.05f, 0f),
                vector(0.98f, 0.1f, 0f)
        ));

        adapter.save(portfolioId, userId, "resume.pdf", text);

        verify(vectorStore).add(documentsCaptor.capture());
        List<Document> chunks = documentsCaptor.getValue();
        assertThat(chunks).hasSize(1);
        assertThat(chunks.get(0).getText()).contains(s1, s2, s3);
    }

    @Test
    void 유사도가_임계값보다_낮으면_새로운_섹션으로_나뉜다() {
        UUID portfolioId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        String s1 = sentenceOfLength('가', 91);
        String s2 = sentenceOfLength('나', 91);
        String s3 = sentenceOfLength('다', 91);
        String s4 = sentenceOfLength('라', 91);
        String shift = sentenceOfLength('마', 91);
        String text = String.join(" ", s1, s2, s3, s4, shift);

        when(embeddingModel.embed(anyList())).thenReturn(List.of(
                vector(1f, 0f, 0f),
                vector(1f, 0f, 0f),
                vector(1f, 0f, 0f),
                vector(1f, 0f, 0f),
                vector(0f, 1f, 0f)
        ));

        adapter.save(portfolioId, userId, "resume.pdf", text);

        verify(vectorStore).add(documentsCaptor.capture());
        List<Document> chunks = documentsCaptor.getValue();
        assertThat(chunks).hasSize(2);
        assertThat(chunks.get(0).getText()).contains(s1, s2, s3, s4);
        assertThat(chunks.get(1).getText()).isEqualTo(shift);
    }

    @Test
    void 유사도가_높아도_최대_길이를_넘기면_섹션이_분할된다() {
        UUID portfolioId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        String s1 = sentenceOfLength('가', 500);
        String s2 = sentenceOfLength('나', 500);
        String s3 = sentenceOfLength('다', 500);
        String s4 = sentenceOfLength('라', 500);
        String s5 = sentenceOfLength('마', 500);
        String text = String.join(" ", s1, s2, s3, s4, s5);

        when(embeddingModel.embed(anyList())).thenReturn(List.of(
                vector(1f, 0f, 0f),
                vector(1f, 0f, 0f),
                vector(1f, 0f, 0f),
                vector(1f, 0f, 0f),
                vector(1f, 0f, 0f)
        ));

        adapter.save(portfolioId, userId, "resume.pdf", text);

        verify(vectorStore).add(documentsCaptor.capture());
        List<Document> chunks = documentsCaptor.getValue();
        assertThat(chunks).hasSize(2);
    }

    @Test
    void 각_청크에_portfolioId_userId_fileName_chunkIndex_메타데이터가_채워진다() {
        UUID portfolioId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        String s1 = sentenceOfLength('가', 1200);
        String s2 = sentenceOfLength('나', 1200);
        String text = s1 + " " + s2;

        when(embeddingModel.embed(anyList())).thenReturn(List.of(
                vector(1f, 0f, 0f),
                vector(1f, 0f, 0f)
        ));

        adapter.save(portfolioId, userId, "resume.pdf", text);

        verify(vectorStore).add(documentsCaptor.capture());
        List<Document> chunks = documentsCaptor.getValue();

        assertThat(chunks).hasSize(2);
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
        String hugeSentenceWithNoPunctuation = "이것은 매우 긴 포트폴리오 설명 문장입니다 ".repeat(300);

        adapter.save(portfolioId, userId, "resume.pdf", hugeSentenceWithNoPunctuation);

        verify(vectorStore).add(documentsCaptor.capture());
        List<Document> chunks = documentsCaptor.getValue();
        assertThat(chunks.size()).isGreaterThan(1);
        verify(embeddingModel, never()).embed(anyList());
    }

    @Test
    void 줄바꿈으로_구분된_문장부호_없는_제목_불릿_텍스트도_문장_경계로_인식한다() {
        UUID portfolioId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        String line1 = "프로젝트 개요";
        String line2 = "담당 업무 요약";
        String text = line1 + "\n" + line2;

        when(embeddingModel.embed(sentencesCaptor.capture())).thenReturn(List.of(
                vector(1f, 0f, 0f),
                vector(0f, 1f, 0f)
        ));

        adapter.save(portfolioId, userId, "resume.pdf", text);

        assertThat(sentencesCaptor.getValue()).containsExactly(line1, line2);
    }

    @Test
    void 불릿_기호_앞도_문장_경계로_인식한다() {
        UUID portfolioId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        String text = "핵심 역량•백엔드 설계•성능 최적화";

        when(embeddingModel.embed(sentencesCaptor.capture())).thenReturn(List.of(
                vector(1f, 0f, 0f),
                vector(0f, 1f, 0f),
                vector(0f, 0f, 1f)
        ));

        adapter.save(portfolioId, userId, "resume.pdf", text);

        assertThat(sentencesCaptor.getValue()).containsExactly("핵심 역량", "•백엔드 설계", "•성능 최적화");
    }

    @Test
    void 문장_부호가_없는_텍스트는_임베딩_호출_없이_한_섹션이_된다() {
        UUID portfolioId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        String text = "프로젝트 개요 및 담당 업무 요약";

        adapter.save(portfolioId, userId, "resume.pdf", text);

        verify(vectorStore).add(documentsCaptor.capture());
        List<Document> chunks = documentsCaptor.getValue();
        assertThat(chunks).hasSize(1);
        assertThat(chunks.get(0).getText()).isEqualTo(text);
        verify(embeddingModel, never()).embed(anyList());
    }

    @Test
    void 빈_텍스트는_섹션이_생성되지_않는다() {
        UUID portfolioId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        adapter.save(portfolioId, userId, "resume.pdf", "   ");

        verify(vectorStore).add(documentsCaptor.capture());
        assertThat(documentsCaptor.getValue()).isEmpty();
        verify(embeddingModel, never()).embed(anyList());
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
