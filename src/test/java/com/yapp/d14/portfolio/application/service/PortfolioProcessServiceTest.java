package com.yapp.d14.portfolio.application.service;

import com.yapp.d14.portfolio.application.port.out.PdfTextExtractor;
import com.yapp.d14.portfolio.application.port.out.PortfolioEmbeddingStore;
import com.yapp.d14.portfolio.application.port.out.PortfolioFileUploader;
import com.yapp.d14.portfolio.application.port.out.PortfolioRepository;
import com.yapp.d14.portfolio.domain.Portfolio;
import com.yapp.d14.portfolio.domain.PortfolioStatus;
import com.yapp.d14.portfolio.exception.PortfolioErrorCode;
import com.yapp.d14.portfolio.exception.PortfolioException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PortfolioProcessServiceTest {

    @Mock
    private PortfolioRepository portfolioRepository;

    @Mock
    private PortfolioFileUploader portfolioFileUploader;

    @Mock
    private PdfTextExtractor pdfTextExtractor;

    @Mock
    private PortfolioEmbeddingStore portfolioEmbeddingStore;

    @InjectMocks
    private PortfolioProcessService portfolioProcessService;

    private UUID userId;
    private Portfolio portfolio;
    private byte[] fileContent;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        fileContent = "fake-pdf-bytes".getBytes();
        portfolio = Portfolio.create(
                UUID.randomUUID(), userId, "resume.pdf", fileContent.length, 5,
                "users/%s/portfolios/%s/test.pdf".formatted(userId, UUID.randomUUID()), false
        );
    }

    @Test
    void 소유자가_아니면_예외를_던지고_후속처리를_하지_않는다() {
        given(portfolioRepository.findById(portfolio.getId())).willReturn(Optional.of(portfolio));
        UUID otherUserId = UUID.randomUUID();

        assertThatThrownBy(() -> portfolioProcessService.process(otherUserId, portfolio.getId(), fileContent))
                .isInstanceOf(PortfolioException.class);

        verify(portfolioFileUploader, never()).upload(any(), any(), any());
    }

    @Test
    void S3_업로드에_실패하면_FAILED_SYSTEM으로_전환하고_이후_단계를_진행하지_않는다() {
        given(portfolioRepository.findById(portfolio.getId())).willReturn(Optional.of(portfolio));
        doThrow(new RuntimeException("S3 장애")).when(portfolioFileUploader).upload(any(), any(), any());

        portfolioProcessService.process(userId, portfolio.getId(), fileContent);

        assertThat(portfolio.getStatus()).isEqualTo(PortfolioStatus.FAILED_SYSTEM);
        verify(portfolioRepository).save(portfolio);
        verify(pdfTextExtractor, never()).extractText(any());
        verify(portfolioFileUploader, never()).delete(any());
    }

    @Test
    void 텍스트_추출에_실패하면_FAILED_FILE로_전환하고_S3_파일을_롤백_삭제한다() {
        given(portfolioRepository.findById(portfolio.getId())).willReturn(Optional.of(portfolio));
        given(pdfTextExtractor.extractText(fileContent))
                .willThrow(new PortfolioException(PortfolioErrorCode.INVALID_PDF_FILE));

        portfolioProcessService.process(userId, portfolio.getId(), fileContent);

        assertThat(portfolio.getStatus()).isEqualTo(PortfolioStatus.FAILED_FILE);
        verify(portfolioRepository).save(portfolio);
        verify(portfolioFileUploader).delete(portfolio.getS3Key());
    }

    @Test
    void 텍스트_추출_중_PortfolioException이_아닌_예외가_발생해도_FAILED_FILE로_전환하고_S3_파일을_롤백_삭제한다() {
        given(portfolioRepository.findById(portfolio.getId())).willReturn(Optional.of(portfolio));
        given(pdfTextExtractor.extractText(fileContent))
                .willThrow(new RuntimeException("Tika 파서 내부 오류"));

        portfolioProcessService.process(userId, portfolio.getId(), fileContent);

        assertThat(portfolio.getStatus()).isEqualTo(PortfolioStatus.FAILED_FILE);
        verify(portfolioRepository).save(portfolio);
        verify(portfolioFileUploader).delete(portfolio.getS3Key());
        verify(portfolioEmbeddingStore, never()).save(any(), any(), any(), any());
    }

    @Test
    void 추출된_텍스트가_30자_미만이면_FAILED_FILE로_전환하고_S3_파일을_롤백_삭제한다() {
        given(portfolioRepository.findById(portfolio.getId())).willReturn(Optional.of(portfolio));
        given(pdfTextExtractor.extractText(fileContent)).willReturn("너무 짧은 텍스트");

        portfolioProcessService.process(userId, portfolio.getId(), fileContent);

        assertThat(portfolio.getStatus()).isEqualTo(PortfolioStatus.FAILED_FILE);
        verify(portfolioRepository).save(portfolio);
        verify(portfolioFileUploader).delete(portfolio.getS3Key());
    }

    @Test
    void 임베딩에_실패하면_FAILED_SYSTEM으로_전환하고_벡터와_S3_파일을_롤백_삭제한다() {
        String extractedText = "이 정도 길이면 30자를 충분히 넘기는 추출된 포트폴리오 텍스트입니다.";
        given(portfolioRepository.findById(portfolio.getId())).willReturn(Optional.of(portfolio));
        given(pdfTextExtractor.extractText(fileContent)).willReturn(extractedText);
        doThrow(new RuntimeException("임베딩 API 장애"))
                .when(portfolioEmbeddingStore)
                .save(portfolio.getId(), userId, portfolio.getFileName(), extractedText);

        portfolioProcessService.process(userId, portfolio.getId(), fileContent);

        assertThat(portfolio.getStatus()).isEqualTo(PortfolioStatus.FAILED_SYSTEM);
        verify(portfolioEmbeddingStore).deleteByPortfolioId(portfolio.getId());
        verify(portfolioFileUploader).delete(portfolio.getS3Key());
    }

    @Test
    void 임베딩까지_성공하면_READY로_전환한다() {
        String extractedText = "이 정도 길이면 30자를 충분히 넘기는 추출된 포트폴리오 텍스트입니다.";
        given(portfolioRepository.findById(portfolio.getId())).willReturn(Optional.of(portfolio));
        given(pdfTextExtractor.extractText(fileContent)).willReturn(extractedText);

        portfolioProcessService.process(userId, portfolio.getId(), fileContent);

        assertThat(portfolio.getStatus()).isEqualTo(PortfolioStatus.READY);
        verify(portfolioEmbeddingStore).save(portfolio.getId(), userId, portfolio.getFileName(), extractedText);
        verify(portfolioRepository).save(portfolio);
        verify(portfolioFileUploader, never()).delete(any());
        verify(portfolioEmbeddingStore, never()).deleteByPortfolioId(any());
    }
}
