package com.yapp.d14.interview.application.service;

import com.yapp.d14.interview.application.port.in.InterviewSessionOwnershipCheckUseCase;
import com.yapp.d14.interview.application.port.in.InterviewVideoUploadCompleteUseCase;
import com.yapp.d14.interview.application.port.in.InterviewVideoUploadUrlIssueUseCase;
import com.yapp.d14.interview.application.port.in.result.InterviewVideoUploadUrlResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

// TODO(#63 Step2~3): presigned URL 발급·업로드 완료 표시를 실제로 구현한다. 현재는 계약 확정용 스텁이다.
@Service
@RequiredArgsConstructor
class InterviewVideoUploadService implements InterviewVideoUploadUrlIssueUseCase, InterviewVideoUploadCompleteUseCase {

    private final InterviewSessionOwnershipCheckUseCase interviewSessionOwnershipCheckUseCase;

    @Override
    public InterviewVideoUploadUrlResult issue(UUID userId, Long sessionId) {
        interviewSessionOwnershipCheckUseCase.requireOwned(userId, sessionId);
        return new InterviewVideoUploadUrlResult("https://stub.local/upload-url", "video/webm", 600L);
    }

    @Override
    public void complete(UUID userId, Long sessionId) {
        interviewSessionOwnershipCheckUseCase.requireOwned(userId, sessionId);
    }
}
