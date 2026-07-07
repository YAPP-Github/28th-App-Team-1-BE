package com.yapp.d14.jd.adapter.out.integration.crawling;

import com.yapp.d14.jd.application.port.out.JdContentFetcher;
import com.yapp.d14.jd.exception.JdCrawlingFailedException;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;

@Component
class JsoupJdContentFetcherAdapter implements JdContentFetcher {

    private static final int TIMEOUT_MS = 5000;

    private static final int MAX_REDIRECTS = 5;

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

        Document document = fetchFollowingRedirects(jdUrl);

        return extractText(document);
    }

    // 리다이렉트를 자동으로 따라가면 최초 URL만 검증한 SSRF 방어가 우회될 수 있어
    // 매 홉마다 Location을 직접 검증한 뒤 다음 요청을 보낸다.
    private Document fetchFollowingRedirects(String url) {
        String currentUrl = url;

        for (int hop = 0; hop <= MAX_REDIRECTS; hop++) {
            Connection.Response response;
            try {
                response = Jsoup.connect(currentUrl)
                        .userAgent(USER_AGENT)
                        .timeout(TIMEOUT_MS)
                        .followRedirects(false)
                        .execute();
            } catch (IOException e) {
                throw new JdCrawlingFailedException("해당 URL에 접근할 수 없습니다.", e);
            }

            if (!isRedirect(response.statusCode())) {
                try {
                    return response.parse();
                } catch (IOException e) {
                    throw new JdCrawlingFailedException("해당 URL에 접근할 수 없습니다.", e);
                }
            }

            currentUrl = resolveRedirectLocation(currentUrl, response);
            JdUrlGuard.assertSafe(currentUrl);
        }

        throw new JdCrawlingFailedException("리다이렉트 횟수가 너무 많습니다.");
    }

    private boolean isRedirect(int statusCode) {
        return statusCode == 301 || statusCode == 302 || statusCode == 303
                || statusCode == 307 || statusCode == 308;
    }

    private String resolveRedirectLocation(String currentUrl, Connection.Response response) {
        String location = response.header("Location");
        if (location == null || location.isBlank()) {
            throw new JdCrawlingFailedException("리다이렉트 대상을 확인할 수 없습니다.");
        }

        try {
            return URI.create(currentUrl).resolve(location).toString();
        } catch (IllegalArgumentException e) {
            throw new JdCrawlingFailedException("올바르지 않은 리다이렉트 대상입니다.");
        }
    }

    private String extractText(Document document) {
        for (String tag : REMOVE_TAGS) {
            document.select(tag).remove();
        }
        String text = document.body().text();
        return text.replaceAll("\\s{2,}", " ").trim();
    }
}
