package com.yapp.d14.interview.adapter.in.web;

import com.yapp.d14.common.web.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.util.UUID;

@Tag(name = "Interview", description = "면접 세션 API")
public interface AudioStreamControllerDocs {

    @Operation(
            summary = "질문 음성 스트리밍",
            description = "질문 텍스트를 온디맨드로 TTS 스트리밍합니다. " +
                    "POST /answers, GET /status 응답에 담긴 questionId로 호출하세요.\n\n" +
                    "**인증**: Access Token 필요 (Authorization: Bearer {accessToken})\n\n" +
                    "**응답 형식**: `Content-Type: audio/mpeg`, `Transfer-Encoding: chunked`로 내려갑니다. " +
                    "TTS가 생성되는 대로 청크가 전송되므로 `Content-Length`가 없고, 전체 오디오 길이도 " +
                    "응답 헤더로는 알 수 없습니다. 서버가 전체 오디오를 다 만들 때까지 기다렸다가 한 번에 " +
                    "내려주는 방식이 아니라는 점에 유의하세요.\n\n" +
                    "**클라이언트 처리 권장 방식**: 응답 전체를 다운로드한 뒤 재생을 시작하지 말고, " +
                    "청크가 도착하는 대로 점진적으로 재생(progressive playback)하는 플레이어를 사용하세요.\n" +
                    "- iOS: `AVPlayer`에 이 엔드포인트 URL을 그대로 `AVURLAsset`/`AVPlayerItem`으로 넘기면 " +
                    "HTTP 스트리밍을 기본 지원합니다.\n" +
                    "- Android: ExoPlayer의 `ProgressiveMediaSource`에 이 URL을 넘기면 청크 단위로 받으며 재생합니다.\n\n" +
                    "**주의사항**: 응답 헤더가 이미 전송된 이후 서버 쪽 오류(TTS 실패 등)가 나면 스트림이 중간에 " +
                    "끊길 수 있습니다. HTTP 상태 코드로 감지되지 않으므로, 클라이언트는 오디오 재생 자체의 " +
                    "에러/중단 콜백으로 실패를 감지하고 재시도하세요(같은 questionId로 재호출 시 TTS를 처음부터 " +
                    "다시 생성합니다)."
    )
    ResponseEntity<StreamingResponseBody> streamAudio(
            @Parameter(hidden = true) @CurrentUser UUID userId,
            @Parameter(description = "면접 세션 ID") @PathVariable Long sessionId,
            @Parameter(description = "질문 ID") @PathVariable Long questionId
    );
}
