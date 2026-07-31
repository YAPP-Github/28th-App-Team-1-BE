package com.yapp.d14.common.web;

import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * 요청 body를 생성 시점에 통째로 읽어 보관하고, {@link #getInputStream()}/{@link #getReader()}가
 * 매번 새 스트림을 돌려줘 body를 여러 번 읽을 수 있게 하는 래퍼.
 *
 * <p>스프링 기본 {@code ContentCachingRequestWrapper}는 downstream이 body를 "읽을 때만" 캐싱한다.
 * 그런데 {@code JwtAuthenticationFilter}는 인증 실패(401) 시 Authorization 헤더만 보고 체인을 태우지
 * 않아 body가 소비되지 않으므로, 그 요청의 body는 캐시가 비어 로깅되지 않는다. 이 래퍼는 body를 미리
 * 읽어 두기 때문에 인증 거부 요청의 body까지 남길 수 있다.
 *
 * <p>body 전체를 메모리에 올리므로, 대용량 멀티파트 업로드처럼 캐싱하면 안 되는 요청은 이 래퍼로
 * 감싸기 전에 {@code RequestResponseLoggingFilter.shouldNotFilter}에서 먼저 걸러야 한다.
 */
public class CachedBodyHttpServletRequest extends HttpServletRequestWrapper {

    private final byte[] cachedBody;

    public CachedBodyHttpServletRequest(HttpServletRequest request) throws IOException {
        super(request);
        this.cachedBody = request.getInputStream().readAllBytes();
    }

    public byte[] getCachedBody() {
        return cachedBody;
    }

    @Override
    public ServletInputStream getInputStream() {
        return new CachedBodyServletInputStream(cachedBody);
    }

    @Override
    public BufferedReader getReader() {
        Charset charset = getCharacterEncoding() == null
                ? StandardCharsets.UTF_8
                : Charset.forName(getCharacterEncoding());
        return new BufferedReader(new InputStreamReader(new ByteArrayInputStream(cachedBody), charset));
    }

    private static final class CachedBodyServletInputStream extends ServletInputStream {

        private final ByteArrayInputStream buffer;

        private CachedBodyServletInputStream(byte[] body) {
            this.buffer = new ByteArrayInputStream(body);
        }

        @Override
        public boolean isFinished() {
            return buffer.available() == 0;
        }

        @Override
        public boolean isReady() {
            return true;
        }

        @Override
        public void setReadListener(ReadListener readListener) {
            throw new UnsupportedOperationException();
        }

        @Override
        public int read() {
            return buffer.read();
        }
    }
}
