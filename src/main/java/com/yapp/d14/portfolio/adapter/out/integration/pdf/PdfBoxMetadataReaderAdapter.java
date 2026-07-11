package com.yapp.d14.portfolio.adapter.out.integration.pdf;

import com.yapp.d14.portfolio.application.port.out.PdfMetadataReader;
import com.yapp.d14.portfolio.exception.PortfolioErrorCode;
import com.yapp.d14.portfolio.exception.PortfolioException;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

@Component
class PdfBoxMetadataReaderAdapter implements PdfMetadataReader {

    // 등록 요청 스레드(Tomcat)에서 PDFBox 전체 파싱이 동시에 무제한으로 몰리는 것을 막기 위한 상한.
    private static final int MAX_CONCURRENT_PARSES = 4;
    private static final long ACQUIRE_TIMEOUT_SECONDS = 5;

    private final Semaphore parseSemaphore = new Semaphore(MAX_CONCURRENT_PARSES);

    @Override
    public int countPages(byte[] fileContent) {
        boolean acquired;
        try {
            acquired = parseSemaphore.tryAcquire(ACQUIRE_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new PortfolioException(PortfolioErrorCode.PDF_PARSING_BUSY);
        }
        if (!acquired) {
            throw new PortfolioException(PortfolioErrorCode.PDF_PARSING_BUSY);
        }

        try (PDDocument document = Loader.loadPDF(fileContent)) {
            return document.getNumberOfPages();
        } catch (IOException e) {
            throw new PortfolioException(PortfolioErrorCode.INVALID_PDF_FILE);
        } finally {
            parseSemaphore.release();
        }
    }
}
