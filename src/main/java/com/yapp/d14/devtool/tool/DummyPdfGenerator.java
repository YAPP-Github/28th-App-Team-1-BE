package com.yapp.d14.devtool.tool;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;

/** 포트폴리오 등록 파이프라인(페이지 수 검증·텍스트 추출·임베딩)을 실제로 태우기 위한 1페이지짜리 더미 이력서 PDF. */
final class DummyPdfGenerator {

    private static final List<String> RESUME_LINES = List.of(
            "Backend Engineer Resume",
            "",
            "Summary",
            "3 years of experience building Spring Boot backend services for a B2C platform.",
            "Focused on API design, database schema evolution, and production reliability.",
            "",
            "Experience",
            "- Designed and implemented REST APIs for a portfolio management feature serving 50k users.",
            "- Migrated a monolithic scheduler into an event-driven pipeline, cutting p95 latency by 40%.",
            "- Introduced automated integration tests, reducing regression bugs in production by half.",
            "",
            "Skills",
            "Java, Spring Boot, PostgreSQL, Redis, AWS S3, Docker",
            "",
            "Projects",
            "- Built a recommendation service using vector similarity search over user activity logs.",
            "- Led a team of three engineers to redesign the checkout flow, improving conversion by 12%."
    );

    private DummyPdfGenerator() {
    }

    static byte[] generate() {
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);

            PDType1Font font = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
            float fontSize = 11f;
            float leading = 16f;
            float startY = PDRectangle.A4.getHeight() - 60f;

            try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
                contentStream.beginText();
                contentStream.setFont(font, fontSize);
                contentStream.newLineAtOffset(50f, startY);
                for (String line : RESUME_LINES) {
                    contentStream.showText(line);
                    contentStream.newLineAtOffset(0f, -leading);
                }
                contentStream.endText();
            }

            ByteArrayOutputStream output = new ByteArrayOutputStream();
            document.save(output);
            return output.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException("더미 포트폴리오 PDF 생성에 실패했습니다.", e);
        }
    }
}
