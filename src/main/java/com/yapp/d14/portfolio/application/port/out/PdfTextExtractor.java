package com.yapp.d14.portfolio.application.port.out;

public interface PdfTextExtractor {

    String extractText(byte[] fileContent);
}
