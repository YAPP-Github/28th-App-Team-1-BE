package com.yapp.d14.interview.adapter.in.web;

import com.yapp.d14.common.response.ApiResponse;
import com.yapp.d14.common.web.CurrentUser;
import com.yapp.d14.interview.adapter.in.web.request.InterviewAbandonHttpRequest;
import com.yapp.d14.interview.adapter.in.web.response.InterviewAbandonHttpResponse;
import com.yapp.d14.interview.adapter.in.web.response.InterviewSessionResumeConfirmHttpResponse;
import com.yapp.d14.interview.adapter.in.web.response.InterviewSessionResumeHttpResponse;
import com.yapp.d14.interview.application.port.in.InterviewAbandonUseCase;
import com.yapp.d14.interview.application.port.in.InterviewSessionResumeConfirmUseCase;
import com.yapp.d14.interview.application.port.in.InterviewSessionResumeQueryUseCase;
import com.yapp.d14.interview.application.port.in.result.InterviewAbandonResult;
import com.yapp.d14.interview.application.port.in.result.InterviewSessionResumeConfirmResult;
import com.yapp.d14.interview.application.port.in.result.InterviewSessionResumeResult;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/interview/sessions")
@RequiredArgsConstructor
class InterviewResumeController implements InterviewResumeControllerDocs {

    private final InterviewSessionResumeQueryUseCase interviewSessionResumeQueryUseCase;
    private final InterviewSessionResumeConfirmUseCase interviewSessionResumeConfirmUseCase;
    private final InterviewAbandonUseCase interviewAbandonUseCase;

    @Override
    @GetMapping("/{sessionId}/resume")
    public ResponseEntity<ApiResponse<InterviewSessionResumeHttpResponse>> getResume(
            @CurrentUser UUID userId,
            @PathVariable Long sessionId
    ) {
        InterviewSessionResumeResult result = interviewSessionResumeQueryUseCase.resume(userId, sessionId);
        return ResponseEntity.ok(ApiResponse.ok(InterviewSessionResumeHttpResponse.from(result)));
    }

    @Override
    @PostMapping("/{sessionId}/resume")
    public ResponseEntity<ApiResponse<InterviewSessionResumeConfirmHttpResponse>> confirmResume(
            @CurrentUser UUID userId,
            @PathVariable Long sessionId
    ) {
        InterviewSessionResumeConfirmResult result = interviewSessionResumeConfirmUseCase.confirmResume(userId, sessionId);
        return ResponseEntity.ok(ApiResponse.ok(InterviewSessionResumeConfirmHttpResponse.from(result)));
    }

    @Override
    @PostMapping("/{sessionId}/abandon")
    public ResponseEntity<ApiResponse<InterviewAbandonHttpResponse>> abandon(
            @CurrentUser UUID userId,
            @PathVariable Long sessionId,
            @Valid @RequestBody InterviewAbandonHttpRequest request
    ) {
        InterviewAbandonResult result = interviewAbandonUseCase.abandon(userId, request.toCommand(sessionId));
        return ResponseEntity.ok(ApiResponse.ok(InterviewAbandonHttpResponse.from(result)));
    }
}
