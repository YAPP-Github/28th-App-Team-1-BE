package com.yapp.d14.interview.application.service;

import com.yapp.d14.interview.application.port.in.InterviewSessionOwnershipCheckUseCase;
import com.yapp.d14.interview.application.port.in.InterviewVideoUploadCompleteUseCase;
import com.yapp.d14.interview.application.port.in.InterviewVideoUploadUrlIssueUseCase;
import com.yapp.d14.interview.application.port.in.result.InterviewVideoUploadUrlResult;
import com.yapp.d14.interview.application.port.out.InterviewVideoStorage;
import com.yapp.d14.interview.application.port.out.InterviewVideoStorage.PresignedUpload;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
class InterviewVideoUploadService implements InterviewVideoUploadUrlIssueUseCase, InterviewVideoUploadCompleteUseCase {

    private final InterviewSessionOwnershipCheckUseCase interviewSessionOwnershipCheckUseCase;
    private final InterviewVideoStorage interviewVideoStorage;

    @Override
    public InterviewVideoUploadUrlResult issue(UUID userId, Long sessionId) {
        interviewSessionOwnershipCheckUseCase.requireOwned(userId, sessionId);
        PresignedUpload upload = interviewVideoStorage.presignUpload(userId, sessionId);
        return new InterviewVideoUploadUrlResult(upload.url(), upload.contentType(), upload.expiresInSeconds());
    }

    @Override
    public void complete(UUID userId, Long sessionId) {
        interviewSessionOwnershipCheckUseCase.requireOwned(userId, sessionId);
        // TODO(#63 Step3): InterviewVideo에 uploaded=true 표시(없으면 upsert)한다.
    }
}
