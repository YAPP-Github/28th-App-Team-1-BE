package com.yapp.d14.jd.adapter.out.integration.crawling;

import com.yapp.d14.jd.application.port.out.JdContentFetcher;
import com.yapp.d14.jd.exception.JdCrawlingFailedException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
class JsoupJdContentFetcherAdapter implements JdContentFetcher {

    private static final int TIMEOUT_MS = 5000;

    private static final String USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36";

    private static final String[] REMOVE_TAGS = {
            "script", "style", "nav", "footer", "header",
            "meta", "link", "noscript", "iframe", "button",
            "form", "input", "select", "textarea", "img"
    };

    @Override
    public String fetch(String jdUrl) {
        JdUrlGuard.assertSafe(jdUrl);

        Document document;
        try {
            document = Jsoup.connect(jdUrl)
                    .userAgent(USER_AGENT)
                    .timeout(TIMEOUT_MS)
                    .get();
        } catch (IOException e) {
            throw new JdCrawlingFailedException("해당 URL에 접근할 수 없습니다.", e);
        }

        return extractText(document);
    }

    private String extractText(Document document) {
        for (String tag : REMOVE_TAGS) {
            document.select(tag).remove();
        }
        String text = document.body().text();
        return text.replaceAll("\\s{2,}", " ").trim();
    }
}
