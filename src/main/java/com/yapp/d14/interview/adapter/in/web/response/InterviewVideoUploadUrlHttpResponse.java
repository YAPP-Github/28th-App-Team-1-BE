package com.yapp.d14.interview.adapter.in.web.response;

import com.yapp.d14.interview.application.port.in.result.InterviewVideoUploadUrlResult;
import io.swagger.v3.oas.annotations.media.Schema;

public record InterviewVideoUploadUrlHttpResponse(
        @Schema(description = "녹화 영상을 업로드할 presigned PUT URL. 이 URL로 영상 바이너리를 PUT 요청한다(서버 경유 X, S3 직접 업로드)",
                example = "https://bucket.s3.ap-northeast-2.amazonaws.com/users/.../recording/raw.webm?X-Amz-Algorithm=...")
        String uploadUrl,

        @Schema(description = "PUT 요청 시 Content-Type 헤더로 그대로 보내야 하는 값. 이 값이 서명에 포함돼 있어 다르면 업로드가 거부된다",
                example = "video/webm")
        String contentType,

        @Schema(description = "uploadUrl 서명의 유효시간(초). 이 시간이 지나면 URL을 다시 발급받아야 한다. (영상 접근 만료와는 별개)",
                example = "600")
        long expiresInSeconds
) {

    public static InterviewVideoUploadUrlHttpResponse from(InterviewVideoUploadUrlResult result) {
        return new InterviewVideoUploadUrlHttpResponse(result.uploadUrl(), result.contentType(), result.expiresInSeconds());
    }
}
