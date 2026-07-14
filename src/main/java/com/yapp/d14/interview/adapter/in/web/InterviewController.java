package com.yapp.d14.interview.adapter.in.web;

import com.yapp.d14.common.response.ApiResponse;
import com.yapp.d14.common.web.CurrentUser;
import com.yapp.d14.interview.adapter.in.web.request.InterviewSessionCreateHttpRequest;
import com.yapp.d14.interview.adapter.in.web.response.InterviewSessionCreateHttpResponse;
import com.yapp.d14.interview.adapter.in.web.response.InterviewSessionStatusHttpResponse;
import com.yapp.d14.interview.application.port.in.InterviewSessionCreateUseCase;
import com.yapp.d14.interview.application.port.in.InterviewSessionStatusUseCase;
import com.yapp.d14.interview.application.port.in.result.InterviewSessionCreateResult;
import com.yapp.d14.interview.application.port.in.result.InterviewSessionStatusResult;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
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
class InterviewController implements InterviewControllerDocs {

    private final InterviewSessionCreateUseCase interviewSessionCreateUseCase;
    private final InterviewSessionStatusUseCase interviewSessionStatusUseCase;

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
}

