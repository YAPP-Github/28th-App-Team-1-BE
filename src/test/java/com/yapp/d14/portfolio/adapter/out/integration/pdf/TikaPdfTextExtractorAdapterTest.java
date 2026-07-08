package com.yapp.d14.portfolio.adapter.out.integration.pdf;

import com.yapp.d14.portfolio.exception.PortfolioException;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TikaPdfTextExtractorAdapterTest {

    private final TikaPdfTextExtractorAdapter adapter = new TikaPdfTextExtractorAdapter();

    @Test
    void 정상적인_PDF에서_텍스트를_추출한다() throws IOException {
        byte[] pdf = createPdfWithText("This is a portfolio test sentence for text extraction.");

        String extracted = adapter.extractText(pdf);

        assertThat(extracted).contains("portfolio test sentence");
    }

    @Test
    void 손상된_PDF면_예외를_던진다() throws IOException {
        byte[] validPdf = createPdfWithText("valid content");
        byte[] truncated = Arrays.copyOf(validPdf, 20);

        assertThatThrownBy(() -> adapter.extractText(truncated))
                .isInstanceOf(PortfolioException.class);
    }

    private byte[] createPdfWithText(String text) throws IOException {
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage();
            document.addPage(page);
            try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
                contentStream.beginText();
                contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                contentStream.newLineAtOffset(50, 700);
                contentStream.showText(text);
                contentStream.endText();
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            document.save(out);
            return out.toByteArray();
        }
    }
}
