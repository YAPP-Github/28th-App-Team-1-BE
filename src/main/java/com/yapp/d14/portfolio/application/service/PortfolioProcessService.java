package com.yapp.d14.portfolio.application.service;

import com.yapp.d14.portfolio.application.port.in.PortfolioProcessUseCase;
import com.yapp.d14.portfolio.application.port.out.PdfTextExtractor;
import com.yapp.d14.portfolio.application.port.out.PortfolioEmbeddingStore;
import com.yapp.d14.portfolio.application.port.out.PortfolioFileUploader;
import com.yapp.d14.portfolio.application.port.out.PortfolioRepository;
import com.yapp.d14.portfolio.domain.Portfolio;
import com.yapp.d14.portfolio.exception.PortfolioException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
class PortfolioProcessService implements PortfolioProcessUseCase {

    private static final String CONTENT_TYPE = "application/pdf";

    private final PortfolioRepository portfolioRepository;
    private final PortfolioFileUploader portfolioFileUploader;
    private final PdfTextExtractor pdfTextExtractor;
    private final PortfolioEmbeddingStore portfolioEmbeddingStore;

    @Override
    @Async("portfolioTaskExecutor")
    public void process(UUID userId, UUID portfolioId, byte[] fileContent) {
        log.info("portfolio async processing triggered: portfolioId={}", portfolioId);

        Portfolio portfolio = PortfolioAccessSupport.requireOwned(portfolioRepository, portfolioId, userId);

        try {
            portfolioFileUploader.upload(portfolio.getS3Key(), fileContent, CONTENT_TYPE);
        } catch (Exception e) {
            log.error("[PORTFOLIO PROCESS] S3 업로드 실패: portfolioId={}", portfolioId, e);
            portfolio.failSystem("파일 업로드에 실패했어요. 잠시 후 다시 시도해 주세요.");
            portfolioRepository.save(portfolio);
            return;
        }

        String extractedText;
        try {
            extractedText = pdfTextExtractor.extractText(fileContent);
        } catch (PortfolioException e) {
            log.error("[PORTFOLIO PROCESS] 텍스트 추출 실패: portfolioId={}", portfolioId, e);
            portfolio.failFile("파일이 손상되었거나 암호로 보호되어 있어요. 다시 업로드해 주세요.");
            portfolioRepository.save(portfolio);
            portfolioFileUploader.delete(portfolio.getS3Key());
            return;
        }

        if (!portfolio.hasEnoughExtractedText(extractedText)) {
            log.warn("[PORTFOLIO PROCESS] 추출된 텍스트가 너무 짧음: portfolioId={}, length={}",
                    portfolioId, extractedText.trim().length());
            portfolio.failFile("텍스트를 인식할 수 없어요. 스캔본이 아닌 PDF 파일로 다시 업로드해 주세요.");
            portfolioRepository.save(portfolio);
            portfolioFileUploader.delete(portfolio.getS3Key());
            return;
        }

        try {
            portfolioEmbeddingStore.save(portfolio.getId(), portfolio.getUserId(), portfolio.getFileName(), extractedText);
        } catch (Exception e) {
            log.error("[PORTFOLIO PROCESS] 임베딩 실패: portfolioId={}", portfolioId, e);
            portfolio.failSystem("포트폴리오 분석에 실패했어요. 잠시 후 다시 시도해 주세요.");
            portfolioRepository.save(portfolio);
            portfolioEmbeddingStore.deleteByPortfolioId(portfolio.getId());
            portfolioFileUploader.delete(portfolio.getS3Key());
            return;
        }

        portfolio.ready();
        portfolioRepository.save(portfolio);
    }
}
