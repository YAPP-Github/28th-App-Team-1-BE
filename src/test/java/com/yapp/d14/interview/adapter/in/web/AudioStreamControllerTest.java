package com.yapp.d14.interview.adapter.in.web;

import com.yapp.d14.interview.application.port.in.AudioStreamUseCase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.context.request.async.AsyncRequestNotUsableException;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
import reactor.core.publisher.Flux;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class AudioStreamControllerTest {

    private static final UUID USER_ID = UUID.randomUUID();
    private static final long SESSION_ID = 1L;
    private static final long QUESTION_ID = 2L;

    @Mock
    private AudioStreamUseCase audioStreamUseCase;

    @Test
    void 오디오_청크를_순서대로_응답에_기록한다() throws IOException {
        given(audioStreamUseCase.stream(any(), any(), any()))
                .willReturn(Flux.just("abc".getBytes(), "de".getBytes()));
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        streamBody().writeTo(outputStream);

        assertThat(outputStream.toByteArray()).isEqualTo("abcde".getBytes());
    }

    @Test
    void 클라이언트가_연결을_끊으면_예외를_전파하지_않는다() {
        given(audioStreamUseCase.stream(any(), any(), any()))
                .willReturn(Flux.just("abc".getBytes()));

        assertThatCode(() -> streamBody().writeTo(brokenPipeOutputStream()))
                .doesNotThrowAnyException();
    }

    @Test
    void 클라이언트가_연결을_끊어도_업스트림_스트림을_취소하지_않는다() throws IOException {
        AtomicBoolean cancelled = new AtomicBoolean(false);
        given(audioStreamUseCase.stream(any(), any(), any()))
                .willReturn(Flux.just("abc".getBytes(), "de".getBytes()).doOnCancel(() -> cancelled.set(true)));

        streamBody().writeTo(brokenPipeOutputStream());

        assertThat(cancelled).isFalse();
    }

    @Test
    void 클라이언트가_연결을_끊어도_남은_청크까지_모두_소진한다() throws IOException {
        List<byte[]> consumed = new ArrayList<>();
        given(audioStreamUseCase.stream(any(), any(), any()))
                .willReturn(Flux.just("abc".getBytes(), "de".getBytes()).doOnNext(consumed::add));

        streamBody().writeTo(brokenPipeOutputStream());

        assertThat(consumed).hasSize(2);
    }

    @Test
    void 클라이언트가_연결을_끊어도_S3_업로드_트리거인_완료_신호가_발화한다() throws IOException {
        AtomicBoolean completed = new AtomicBoolean(false);
        given(audioStreamUseCase.stream(any(), any(), any()))
                .willReturn(Flux.just("abc".getBytes()).doOnComplete(() -> completed.set(true)));

        streamBody().writeTo(brokenPipeOutputStream());

        assertThat(completed).isTrue();
    }

    @Test
    void 첫_청크_쓰기가_실패하면_이후_청크는_쓰기를_시도하지_않는다() throws IOException {
        AtomicInteger writeAttempts = new AtomicInteger();
        given(audioStreamUseCase.stream(any(), any(), any()))
                .willReturn(Flux.just("abc".getBytes(), "de".getBytes(), "fgh".getBytes()));

        streamBody().writeTo(countingBrokenPipeOutputStream(writeAttempts));

        assertThat(writeAttempts).hasValue(1);
    }

    @Test
    void 오디오_생성_실패는_그대로_전파한다() {
        given(audioStreamUseCase.stream(any(), any(), any()))
                .willReturn(Flux.error(new IllegalStateException("TTS 실패")));
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        assertThatThrownBy(() -> streamBody().writeTo(outputStream))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("TTS 실패");
    }

    private StreamingResponseBody streamBody() {
        return new AudioStreamController(audioStreamUseCase)
                .streamAudio(USER_ID, SESSION_ID, QUESTION_ID)
                .getBody();
    }

    private OutputStream brokenPipeOutputStream() {
        return new OutputStream() {
            @Override
            public void write(int b) throws IOException {
                throw disconnected();
            }

            @Override
            public void write(byte[] b, int off, int len) throws IOException {
                throw disconnected();
            }

            private IOException disconnected() {
                return new AsyncRequestNotUsableException(
                        "ServletOutputStream failed to write", new IOException("Broken pipe"));
            }
        };
    }

    private OutputStream countingBrokenPipeOutputStream(AtomicInteger writeAttempts) {
        return new OutputStream() {
            @Override
            public void write(int b) throws IOException {
                writeAttempts.incrementAndGet();
                throw new IOException("Broken pipe");
            }

            @Override
            public void write(byte[] b, int off, int len) throws IOException {
                writeAttempts.incrementAndGet();
                throw new IOException("Broken pipe");
            }
        };
    }
}
