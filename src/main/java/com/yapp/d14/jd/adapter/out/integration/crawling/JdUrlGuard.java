package com.yapp.d14.jd.adapter.out.integration.crawling;

import com.yapp.d14.jd.exception.JdCrawlingFailedException;

import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;

final class JdUrlGuard {

    private JdUrlGuard() {
    }

    static void assertSafe(String url) {
        URI uri;
        try {
            uri = new URI(url);
        } catch (URISyntaxException e) {
            throw new JdCrawlingFailedException("올바르지 않은 URL입니다.");
        }

        String scheme = uri.getScheme();
        if (scheme == null || !(scheme.equalsIgnoreCase("http") || scheme.equalsIgnoreCase("https"))) {
            throw new JdCrawlingFailedException("지원하지 않는 URL 스킴입니다.");
        }

        String host = uri.getHost();
        if (host == null) {
            throw new JdCrawlingFailedException("올바르지 않은 URL입니다.");
        }

        InetAddress address;
        try {
            address = InetAddress.getByName(host);
        } catch (UnknownHostException e) {
            throw new JdCrawlingFailedException("호스트를 확인할 수 없습니다.");
        }

        if (address.isLoopbackAddress()
                || address.isLinkLocalAddress()
                || address.isSiteLocalAddress()
                || address.isAnyLocalAddress()
                || address.isMulticastAddress()) {
            throw new JdCrawlingFailedException("접근할 수 없는 주소입니다.");
        }
    }
}
