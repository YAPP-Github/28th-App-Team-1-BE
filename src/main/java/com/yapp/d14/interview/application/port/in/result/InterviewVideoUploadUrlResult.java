package com.yapp.d14.interview.application.port.in.result;

// uploadUrl: presigned PUT URL. contentType: 업로드 시 Content-Type 헤더로 그대로 보내야 하는 값(서명에 포함됨).
// expiresInSeconds: 이 URL 서명의 유효시간(초). 영상 콘텐츠 접근 만료(video_expires_at)와는 별개다.
public record InterviewVideoUploadUrlResult(
        String uploadUrl,
        String contentType,
        long expiresInSeconds
) {
}
