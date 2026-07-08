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

    private final Tika tika = new Tika();

    @Override
    public String extractText(byte[] fileContent) {
        try (ByteArrayInputStream input = new ByteArrayInputStream(fileContent)) {
            return tika.parseToString(input);
        } catch (IOException | TikaException e) {
            throw new PortfolioException(PortfolioErrorCode.INVALID_PDF_FILE);
        }
    }
}
