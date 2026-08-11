package com.yapp.d14.interview.adapter.in.web;

import com.yapp.d14.common.web.CurrentUser;
import com.yapp.d14.interview.application.port.in.AudioStreamUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.io.OutputStream;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@RestController
@RequestMapping("/api/v1/interview/sessions")
@RequiredArgsConstructor
class AudioStreamController implements AudioStreamControllerDocs {

    private final AudioStreamUseCase audioStreamUseCase;

    @Override
    @GetMapping(value = "/{sessionId}/questions/{questionId}/audio/stream", produces = "audio/mpeg")
    public ResponseEntity<StreamingResponseBody> streamAudio(
            @CurrentUser UUID userId,
            @PathVariable Long sessionId,
            @PathVariable Long questionId
    ) {
        Flux<byte[]> audioChunks = audioStreamUseCase.stream(userId, sessionId, questionId);
        StreamingResponseBody body = outputStream -> {
            // 연결이 끊겨도 쓰기만 포기하고 구독은 유지한다. 여기서 예외를 던지면 업스트림이 취소돼
            // doOnComplete가 발화하지 않아 질문 음성이 S3에 저장되지 않는다(TTS 비용은 이미 지불됐다).
            AtomicBoolean disconnected = new AtomicBoolean();
            audioChunks.doOnNext(chunk -> {
                if (disconnected.get()) {
                    return;
                }
                if (!writeChunk(outputStream, chunk)) {
                    disconnected.set(true);
                    log.debug("클라이언트가 오디오 스트림 도중 연결을 끊었습니다, 남은 TTS는 계속 수신합니다: sessionId={}, questionId={}",
                            sessionId, questionId);
                }
            }).blockLast();
        };
        return ResponseEntity.ok().contentType(MediaType.valueOf("audio/mpeg")).body(body);
    }

    private boolean writeChunk(OutputStream outputStream, byte[] chunk) {
        try {
            outputStream.write(chunk);
            outputStream.flush();
            return true;
        } catch (IOException e) {
            return false;
        }
    }
}
