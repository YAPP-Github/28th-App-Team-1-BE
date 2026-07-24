package com.yapp.d14.portfolio.application.service;

import com.yapp.d14.portfolio.application.port.in.PortfolioProcessUseCase;
import com.yapp.d14.portfolio.application.port.out.PdfTextExtractor;
import com.yapp.d14.portfolio.application.port.out.PortfolioEmbeddingStore;
import com.yapp.d14.portfolio.application.port.out.PortfolioFileUploader;
import com.yapp.d14.portfolio.application.port.out.PortfolioRepository;
import com.yapp.d14.portfolio.domain.Portfolio;
import com.yapp.d14.portfolio.domain.PortfolioStatus;
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
    private static final int MAX_PROCESS_RETRIES = 2;

    private final PortfolioRepository portfolioRepository;
    private final PortfolioFileUploader portfolioFileUploader;
    private final PdfTextExtractor pdfTextExtractor;
    private final PortfolioEmbeddingStore portfolioEmbeddingStore;

    @Override
    @Async("portfolioTaskExecutor")
    public void process(UUID userId, UUID portfolioId, byte[] fileContent) {
        log.info("portfolio async processing triggered: portfolioId={}", portfolioId);

        Portfolio portfolio = PortfolioAccessSupport.requireOwned(portfolioRepository, portfolioId, userId);

        if (!uploadToS3(portfolio, fileContent)) {
            return;
        }

        String extractedText = extractText(portfolio, fileContent);
        if (extractedText == null) {
            return;
        }

        if (!embed(portfolio, extractedText)) {
            return;
        }

        if (!isStillProcessing(portfolio.getId())) {
            log.warn("[PORTFOLIO PROCESS] 처리 시간 초과로 이미 종료됨, 완료 처리를 생략하고 리소스를 정리함: portfolioId={}", portfolio.getId());
            portfolioEmbeddingStore.deleteByPortfolioId(portfolio.getId());
            portfolioFileUploader.delete(portfolio.getS3Key());
            return;
        }
        portfolio.ready();
        portfolioRepository.save(portfolio);
    }

    private boolean uploadToS3(Portfolio portfolio, byte[] fileContent) {
        for (int attempt = 1; attempt <= MAX_PROCESS_RETRIES + 1; attempt++) {
            if (attempt > 1 && !isStillProcessing(portfolio.getId())) {
                log.warn("[PORTFOLIO PROCESS] 처리 시간 초과로 이미 종료됨, S3 업로드 재시도를 중단함: portfolioId={}", portfolio.getId());
                break;
            }
            try {
                portfolioFileUploader.upload(portfolio.getS3Key(), fileContent, CONTENT_TYPE);
                return true;
            } catch (Exception e) {
                log.warn("[PORTFOLIO PROCESS] S3 업로드 실패 ({}/{}): portfolioId={}",
                        attempt, MAX_PROCESS_RETRIES + 1, portfolio.getId(), e);
            }
        }

        log.error("[PORTFOLIO PROCESS] S3 업로드 처리 실패: portfolioId={}", portfolio.getId());
        if (isStillProcessing(portfolio.getId())) {
            portfolio.failSystem("파일 업로드에 실패했어요. 잠시 후 다시 시도해 주세요.");
            portfolioRepository.save(portfolio);
        }
        return false;
    }

    private String extractText(Portfolio portfolio, byte[] fileContent) {
        String extractedText;
        try {
            extractedText = pdfTextExtractor.extractText(fileContent);
        } catch (Exception e) {
            log.error("[PORTFOLIO PROCESS] 텍스트 추출 실패: portfolioId={}", portfolio.getId(), e);
            failFileAndCleanupS3(portfolio, "파일이 손상되었거나 암호로 보호되어 있어요. 다시 업로드해 주세요.");
            return null;
        }

        if (!portfolio.hasEnoughExtractedText(extractedText)) {
            log.warn("[PORTFOLIO PROCESS] 추출된 텍스트가 너무 짧음: portfolioId={}, length={}",
                    portfolio.getId(), extractedText.trim().length());
            failFileAndCleanupS3(portfolio, "텍스트를 인식할 수 없어요. 스캔본이 아닌 PDF 파일로 다시 업로드해 주세요.");
            return null;
        }

        return extractedText;
    }

    private boolean embed(Portfolio portfolio, String extractedText) {
        for (int attempt = 1; attempt <= MAX_PROCESS_RETRIES + 1; attempt++) {
            if (attempt > 1 && !isStillProcessing(portfolio.getId())) {
                log.warn("[PORTFOLIO PROCESS] 처리 시간 초과로 이미 종료됨, 임베딩 재시도를 중단함: portfolioId={}", portfolio.getId());
                break;
            }
            try {
                portfolioEmbeddingStore.save(portfolio.getId(), portfolio.getUserId(), portfolio.getFileName(), extractedText);
                return true;
            } catch (Exception e) {
                log.warn("[PORTFOLIO PROCESS] 임베딩 실패 ({}/{}): portfolioId={}",
                        attempt, MAX_PROCESS_RETRIES + 1, portfolio.getId(), e);
            }
        }

        log.error("[PORTFOLIO PROCESS] 임베딩 처리 실패: portfolioId={}", portfolio.getId());
        if (isStillProcessing(portfolio.getId())) {
            portfolio.failSystem("포트폴리오 분석에 실패했어요. 잠시 후 다시 시도해 주세요.");
            portfolioRepository.save(portfolio);
        }
        portfolioEmbeddingStore.deleteByPortfolioId(portfolio.getId());
        portfolioFileUploader.delete(portfolio.getS3Key());
        return false;
    }

    private void failFileAndCleanupS3(Portfolio portfolio, String message) {
        if (isStillProcessing(portfolio.getId())) {
            portfolio.failFile(message);
            portfolioRepository.save(portfolio);
        }
        portfolioFileUploader.delete(portfolio.getS3Key());
    }

    private boolean isStillProcessing(UUID portfolioId) {
        return portfolioRepository.findById(portfolioId)
                .map(p -> p.getStatus() == PortfolioStatus.PROCESSING)
                .orElse(false);
    }
}
