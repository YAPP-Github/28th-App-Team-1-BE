package com.yapp.d14.portfolio.adapter.out.integration.pdf;

import com.yapp.d14.portfolio.application.port.out.PdfTextExtractor;
import com.yapp.d14.portfolio.exception.PortfolioErrorCode;
import com.yapp.d14.portfolio.exception.PortfolioException;
import org.apache.tika.Tika;
import org.apache.tika.exception.TikaException;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.IOException;

@Component
class TikaPdfTextExtractorAdapter implements PdfTextExtractor {

    // Tika 기본값(100,000자)을 넘으면 예외 없이 조용히 잘라서 반환하므로 무제한으로 해제한다.
    // 파일 크기(20MB)·페이지 수(30p)는 이미 상위 검증에서 제한되어 있어 무제한으로 둬도 안전하다.
    private static final int UNLIMITED_STRING_LENGTH = -1;

    private final Tika tika = new Tika();

    TikaPdfTextExtractorAdapter() {
        tika.setMaxStringLength(UNLIMITED_STRING_LENGTH);
    }

    @Override
    public String extractText(byte[] fileContent) {
        try (ByteArrayInputStream input = new ByteArrayInputStream(fileContent)) {
            return tika.parseToString(input);
        } catch (IOException | TikaException e) {
            throw new PortfolioException(PortfolioErrorCode.INVALID_PDF_FILE);
        }
    }
}
