package com.yapp.d14.interview.adapter.in.web;

import com.yapp.d14.common.web.CurrentUser;
import com.yapp.d14.interview.application.port.in.AudioStreamUseCase;
import lombok.RequiredArgsConstructor;
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
import java.io.UncheckedIOException;
import java.util.UUID;

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
        StreamingResponseBody body = outputStream -> audioChunks
                .doOnNext(chunk -> writeChunk(outputStream, chunk))
                .blockLast();
        return ResponseEntity.ok().contentType(MediaType.valueOf("audio/mpeg")).body(body);
    }

    private void writeChunk(OutputStream outputStream, byte[] chunk) {
        try {
            outputStream.write(chunk);
            outputStream.flush();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
