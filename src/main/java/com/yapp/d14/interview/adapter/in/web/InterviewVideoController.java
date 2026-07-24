package com.yapp.d14.interview.adapter.in.web;

import com.yapp.d14.common.response.ApiResponse;
import com.yapp.d14.common.web.CurrentUser;
import com.yapp.d14.interview.adapter.in.web.response.InterviewVideoUploadUrlHttpResponse;
import com.yapp.d14.interview.application.port.in.InterviewVideoUploadCompleteUseCase;
import com.yapp.d14.interview.application.port.in.InterviewVideoUploadUrlIssueUseCase;
import com.yapp.d14.interview.application.port.in.result.InterviewVideoUploadUrlResult;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/interview/sessions")
@RequiredArgsConstructor
class InterviewVideoController implements InterviewVideoControllerDocs {

    private final InterviewVideoUploadUrlIssueUseCase interviewVideoUploadUrlIssueUseCase;
    private final InterviewVideoUploadCompleteUseCase interviewVideoUploadCompleteUseCase;

    @Override
    @PostMapping("/{sessionId}/video/upload-url")
    public ResponseEntity<ApiResponse<InterviewVideoUploadUrlHttpResponse>> issueUploadUrl(
            @CurrentUser UUID userId,
            @PathVariable Long sessionId
    ) {
        InterviewVideoUploadUrlResult result = interviewVideoUploadUrlIssueUseCase.issue(userId, sessionId);
        return ResponseEntity.ok(ApiResponse.ok(InterviewVideoUploadUrlHttpResponse.from(result)));
    }

    @Override
    @PostMapping("/{sessionId}/video/complete")
    public ResponseEntity<ApiResponse<Void>> completeUpload(
            @CurrentUser UUID userId,
            @PathVariable Long sessionId
    ) {
        interviewVideoUploadCompleteUseCase.complete(userId, sessionId);
        return ResponseEntity.ok(ApiResponse.ok());
    }
}
