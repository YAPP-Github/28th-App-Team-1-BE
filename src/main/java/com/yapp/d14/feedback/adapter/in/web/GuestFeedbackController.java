package com.yapp.d14.feedback.adapter.in.web;

import com.yapp.d14.common.response.ApiResponse;
import com.yapp.d14.feedback.adapter.in.web.request.GuestFeedbackSubmitHttpRequest;
import com.yapp.d14.feedback.adapter.in.web.response.GuestFeedbackEntryHttpResponse;
import com.yapp.d14.feedback.adapter.in.web.response.GuestFeedbackSubmitHttpResponse;
import com.yapp.d14.feedback.application.port.in.GuestFeedbackEntryUseCase;
import com.yapp.d14.feedback.application.port.in.GuestFeedbackSubmitUseCase;
import com.yapp.d14.feedback.application.port.in.result.GuestFeedbackEntryResult;
import com.yapp.d14.feedback.application.port.in.result.GuestFeedbackSubmitResult;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/feedback/guest")
@RequiredArgsConstructor
class GuestFeedbackController implements GuestFeedbackControllerDocs {

    private final GuestFeedbackEntryUseCase guestFeedbackEntryUseCase;
    private final GuestFeedbackSubmitUseCase guestFeedbackSubmitUseCase;

    @Override
    @GetMapping("/{token}")
    public ResponseEntity<ApiResponse<GuestFeedbackEntryHttpResponse>> enter(
            @PathVariable String token,
            @RequestHeader(value = "Device-Id", required = false) String deviceId
    ) {
        GuestFeedbackEntryResult result = guestFeedbackEntryUseCase.enter(token, deviceId);
        return ResponseEntity.ok(ApiResponse.ok(GuestFeedbackEntryHttpResponse.from(result)));
    }

    @Override
    @PostMapping("/{token}/submissions")
    public ResponseEntity<ApiResponse<GuestFeedbackSubmitHttpResponse>> submit(
            @PathVariable String token,
            @RequestHeader(value = "Device-Id", required = false) String deviceId,
            @Valid @RequestBody GuestFeedbackSubmitHttpRequest request
    ) {
        GuestFeedbackSubmitResult result = guestFeedbackSubmitUseCase.submit(request.toCommand(token, deviceId));
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(GuestFeedbackSubmitHttpResponse.from(result)));
    }
}
