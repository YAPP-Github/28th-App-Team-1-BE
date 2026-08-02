package com.yapp.d14.interview.adapter.in.web;

import com.yapp.d14.common.response.ApiResponse;
import com.yapp.d14.common.web.CurrentUser;
import com.yapp.d14.interview.adapter.in.web.request.InterviewAnswerSubmitHttpRequest;
import com.yapp.d14.interview.adapter.in.web.request.InterviewSessionCreateHttpRequest;
import com.yapp.d14.interview.adapter.in.web.response.InterviewAnswerSubmitHttpResponse;
import com.yapp.d14.interview.adapter.in.web.response.InterviewReportListHttpResponse;
import com.yapp.d14.interview.adapter.in.web.response.InterviewSessionCreateHttpResponse;
import com.yapp.d14.interview.adapter.in.web.response.InterviewSessionStatusHttpResponse;
import com.yapp.d14.interview.application.port.in.InterviewAnswerSubmitUseCase;
import com.yapp.d14.interview.application.port.in.InterviewReportListQueryUseCase;
import com.yapp.d14.interview.application.port.in.InterviewSessionCreateUseCase;
import com.yapp.d14.interview.application.port.in.InterviewSessionStatusUseCase;
import com.yapp.d14.interview.application.port.in.result.InterviewAnswerSubmitResult;
import com.yapp.d14.interview.application.port.in.result.InterviewSessionCreateResult;
import com.yapp.d14.interview.application.port.in.result.InterviewSessionStatusResult;
import com.yapp.d14.interview.exception.InterviewErrorCode;
import com.yapp.d14.interview.exception.InterviewException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/interview/sessions")
@RequiredArgsConstructor
class InterviewController implements InterviewControllerDocs {

    // m4a(MP4 컨테이너/AAC) 계열로 허용하는 content-type. 클라·프록시가 붙이는 표기 편차를 흡수한다.
    private static final Set<String> M4A_CONTENT_TYPES =
            Set.of("audio/mp4", "audio/x-m4a", "audio/m4a");

    private final InterviewSessionCreateUseCase interviewSessionCreateUseCase;
    private final InterviewSessionStatusUseCase interviewSessionStatusUseCase;
    private final InterviewAnswerSubmitUseCase interviewAnswerSubmitUseCase;
    private final InterviewReportListQueryUseCase interviewReportListQueryUseCase;

    @Override
    @GetMapping
    public ResponseEntity<ApiResponse<InterviewReportListHttpResponse>> getReportList(@CurrentUser UUID userId) {
        return ResponseEntity.ok(ApiResponse.ok(
                InterviewReportListHttpResponse.from(interviewReportListQueryUseCase.getReportList(userId))));
    }

    @Override
    @PostMapping
    public ResponseEntity<ApiResponse<InterviewSessionCreateHttpResponse>> create(
            @CurrentUser UUID userId,
            @Valid @RequestBody InterviewSessionCreateHttpRequest request
    ) {
        InterviewSessionCreateResult result = interviewSessionCreateUseCase.create(request.toCommand(userId));
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(ApiResponse.ok(InterviewSessionCreateHttpResponse.from(result)));
    }

    @Override
    @GetMapping("/{sessionId}/status")
    public ResponseEntity<ApiResponse<InterviewSessionStatusHttpResponse>> getStatus(
            @CurrentUser UUID userId,
            @PathVariable Long sessionId
    ) {
        InterviewSessionStatusResult result = interviewSessionStatusUseCase.getStatus(userId, sessionId);
        return ResponseEntity.ok(ApiResponse.ok(InterviewSessionStatusHttpResponse.from(result)));
    }

    @Override
    @PostMapping(value = "/{sessionId}/answers", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<InterviewAnswerSubmitHttpResponse>> submitAnswer(
            @CurrentUser UUID userId,
            @PathVariable Long sessionId,
            @RequestPart(value = "audio", required = false) MultipartFile audio,
            @Valid @ModelAttribute InterviewAnswerSubmitHttpRequest request
    ) {
        validateAudioFormat(audio);
        InterviewAnswerSubmitResult result =
                interviewAnswerSubmitUseCase.submit(userId, request.toCommand(sessionId, audio));
        return ResponseEntity.ok(ApiResponse.ok(InterviewAnswerSubmitHttpResponse.from(result)));
    }

    // 클라이언트(iOS/Android 네이티브)는 답변 음성을 m4a로 통일 업로드한다. STT(Whisper)는 확장자로 포맷을
    // 추론하므로, m4a 계약을 어긴 업로드(예: 브라우저 기본 webm)를 여기서 400으로 조기 차단한다.
    // content-type이 일반값(application/octet-stream 등)일 수 있어 파일명 확장자도 함께 신호로 본다.
    private static void validateAudioFormat(MultipartFile audio) {
        if (audio == null || audio.isEmpty()) {
            return;
        }
        String contentType = audio.getContentType();
        if (contentType != null && M4A_CONTENT_TYPES.contains(contentType.toLowerCase(Locale.ROOT))) {
            return;
        }
        String filename = audio.getOriginalFilename();
        if (filename != null) {
            String lower = filename.toLowerCase(Locale.ROOT);
            if (lower.endsWith(".m4a") || lower.endsWith(".mp4")) {
                return;
            }
        }
        throw new InterviewException(InterviewErrorCode.INVALID_AUDIO_FORMAT);
    }
}

