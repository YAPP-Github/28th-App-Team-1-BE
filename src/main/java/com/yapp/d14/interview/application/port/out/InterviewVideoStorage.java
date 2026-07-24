package com.yapp.d14.interview.application.port.out;

import java.util.UUID;

public interface InterviewVideoStorage {

    /** 녹화 영상 업로드용 presigned PUT URL을 발급한다. contentType/서명 유효시간은 어댑터가 정한다. */
    PresignedUpload presignUpload(UUID userId, Long sessionId);

    /** 녹화 영상 재생용 presigned GET URL을 발급한다. */
    String presignPlayback(UUID userId, Long sessionId);

    // url: presigned PUT URL, contentType: PUT 시 반드시 붙여야 하는 Content-Type(서명 포함), expiresInSeconds: 서명 유효시간(초)
    record PresignedUpload(String url, String contentType, long expiresInSeconds) {
    }
}
