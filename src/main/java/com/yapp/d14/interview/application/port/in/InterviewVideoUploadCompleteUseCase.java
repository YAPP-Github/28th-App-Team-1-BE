package com.yapp.d14.interview.application.port.in;

import java.util.UUID;

public interface InterviewVideoUploadCompleteUseCase {

    /** 프론트가 S3 업로드를 끝낸 뒤 호출한다. 해당 세션 영상을 "업로드 완료"로 표시해 재생 URL 발급을 허용한다. */
    void complete(UUID userId, Long sessionId);
}
