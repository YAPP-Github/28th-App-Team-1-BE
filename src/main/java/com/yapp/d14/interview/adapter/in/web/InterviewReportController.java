package com.yapp.d14.interview.adapter.in.web;

import com.yapp.d14.common.response.ApiResponse;
import com.yapp.d14.common.web.CurrentUser;
import com.yapp.d14.interview.adapter.in.web.response.InterviewReportHttpResponse;
import com.yapp.d14.interview.application.port.in.InterviewReportQueryUseCase;
import com.yapp.d14.interview.application.port.in.result.InterviewReportQueryResult;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/interview/sessions")
@RequiredArgsConstructor
class InterviewReportController implements InterviewReportControllerDocs {

    private final InterviewReportQueryUseCase interviewReportQueryUseCase;

    @Override
    @GetMapping("/{sessionId}/report")
    public ResponseEntity<ApiResponse<InterviewReportHttpResponse>> getReport(
            @CurrentUser UUID userId,
            @PathVariable Long sessionId
    ) {
        InterviewReportQueryResult result = interviewReportQueryUseCase.getReport(userId, sessionId);
        return ResponseEntity.ok(ApiResponse.ok(InterviewReportHttpResponse.from(result)));
    }
}
