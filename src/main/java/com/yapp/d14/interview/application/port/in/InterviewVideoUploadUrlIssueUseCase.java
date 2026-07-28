package com.yapp.d14.interview.application.port.in;

import com.yapp.d14.interview.application.port.in.result.InterviewVideoUploadUrlResult;

import java.util.UUID;

public interface InterviewVideoUploadUrlIssueUseCase {

    /** 면접 종료 후, 프론트가 녹화 영상을 S3에 직접 올릴 수 있는 presigned PUT URL을 발급한다. */
    InterviewVideoUploadUrlResult issue(UUID userId, Long sessionId);
}
