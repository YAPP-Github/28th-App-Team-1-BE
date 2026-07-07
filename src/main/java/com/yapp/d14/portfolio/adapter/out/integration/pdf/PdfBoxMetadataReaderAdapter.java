package com.yapp.d14.portfolio.adapter.out.integration.pdf;

import com.yapp.d14.portfolio.application.port.out.PdfMetadataReader;
import com.yapp.d14.portfolio.exception.PortfolioErrorCode;
import com.yapp.d14.portfolio.exception.PortfolioException;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
class PdfBoxMetadataReaderAdapter implements PdfMetadataReader {

    @Override
    public int countPages(byte[] fileContent) {
        try (PDDocument document = Loader.loadPDF(fileContent)) {
            return document.getNumberOfPages();
        } catch (IOException e) {
            throw new PortfolioException(PortfolioErrorCode.INVALID_PDF_FILE);
        }
    }
}
