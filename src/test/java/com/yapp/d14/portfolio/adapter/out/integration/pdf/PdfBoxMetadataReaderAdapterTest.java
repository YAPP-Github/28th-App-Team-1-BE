package com.yapp.d14.portfolio.adapter.out.integration.pdf;

import com.yapp.d14.portfolio.exception.PortfolioException;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PdfBoxMetadataReaderAdapterTest {

    private final PdfBoxMetadataReaderAdapter adapter = new PdfBoxMetadataReaderAdapter();

    @Test
    void 정상적인_PDF의_페이지_수를_반환한다() throws IOException {
        byte[] pdf = createPdf(3);

        int pageCount = adapter.countPages(pdf);

        assertThat(pageCount).isEqualTo(3);
    }

    @Test
    void 손상된_파일이면_예외를_던진다() {
        byte[] garbage = "not a pdf".getBytes();

        assertThatThrownBy(() -> adapter.countPages(garbage))
                .isInstanceOf(PortfolioException.class);
    }

    @Test
    void 동시_요청이_상한_이내면_모두_정상적으로_처리된다() throws Exception {
        byte[] pdf = createPdf(1);
        int concurrentRequests = 4;
        ExecutorService executor = Executors.newFixedThreadPool(concurrentRequests);
        try {
            List<Callable<Integer>> tasks = List.of(
                    () -> adapter.countPages(pdf),
                    () -> adapter.countPages(pdf),
                    () -> adapter.countPages(pdf),
                    () -> adapter.countPages(pdf)
            );

            List<Future<Integer>> results = executor.invokeAll(tasks);
            for (Future<Integer> result : results) {
                assertThat(result.get()).isEqualTo(1);
            }
        } finally {
            executor.shutdown();
        }
    }

    private byte[] createPdf(int pageCount) throws IOException {
        try (PDDocument document = new PDDocument()) {
            for (int i = 0; i < pageCount; i++) {
                document.addPage(new PDPage());
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            document.save(out);
            return out.toByteArray();
        }
    }
}
