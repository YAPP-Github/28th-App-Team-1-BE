package com.yapp.d14.feedback.adapter.in.web;

import com.yapp.d14.common.response.ApiResponse;
import com.yapp.d14.common.web.CurrentUser;
import com.yapp.d14.feedback.adapter.in.web.request.FeedbackShareCreateHttpRequest;
import com.yapp.d14.feedback.adapter.in.web.request.FeedbackShareUpdateHttpRequest;
import com.yapp.d14.feedback.adapter.in.web.response.FeedbackShareCreateHttpResponse;
import com.yapp.d14.feedback.adapter.in.web.response.FeedbackShareStatusHttpResponse;
import com.yapp.d14.feedback.application.port.in.FeedbackShareCloseUseCase;
import com.yapp.d14.feedback.application.port.in.FeedbackShareCreateUseCase;
import com.yapp.d14.feedback.application.port.in.FeedbackShareQueryUseCase;
import com.yapp.d14.feedback.application.port.in.result.FeedbackShareCreateResult;
import com.yapp.d14.feedback.application.port.in.result.FeedbackShareStatusResult;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/feedback/sessions/{sessionId}/share")
@RequiredArgsConstructor
class FeedbackShareController implements FeedbackShareControllerDocs {

    private final FeedbackShareCreateUseCase feedbackShareCreateUseCase;
    private final FeedbackShareQueryUseCase feedbackShareQueryUseCase;
    private final FeedbackShareCloseUseCase feedbackShareCloseUseCase;

    @Override
    @PostMapping
    public ResponseEntity<ApiResponse<FeedbackShareCreateHttpResponse>> create(
            @CurrentUser UUID userId,
            @PathVariable Long sessionId,
            @Valid @RequestBody FeedbackShareCreateHttpRequest request
    ) {
        FeedbackShareCreateResult result = feedbackShareCreateUseCase.create(request.toCommand(userId, sessionId));
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(FeedbackShareCreateHttpResponse.from(result)));
    }

    @Override
    @GetMapping
    public ResponseEntity<ApiResponse<FeedbackShareStatusHttpResponse>> get(
            @CurrentUser UUID userId,
            @PathVariable Long sessionId
    ) {
        FeedbackShareStatusResult result = feedbackShareQueryUseCase.get(userId, sessionId);
        return ResponseEntity.ok(ApiResponse.ok(FeedbackShareStatusHttpResponse.from(result)));
    }

    @Override
    @PatchMapping
    public ResponseEntity<ApiResponse<Void>> updateStatus(
            @CurrentUser UUID userId,
            @PathVariable Long sessionId,
            @Valid @RequestBody FeedbackShareUpdateHttpRequest request
    ) {
        feedbackShareCloseUseCase.close(request.toCommand(userId, sessionId));
        return ResponseEntity.ok(ApiResponse.ok());
    }
}
