package com.yapp.d14.interview.adapter.out.integration.aws;

import com.yapp.d14.common.properties.S3Properties;
import com.yapp.d14.common.util.S3KeyGenerator;
import com.yapp.d14.interview.application.port.out.InterviewVideoStorage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.time.Duration;
import java.util.UUID;

@Component
@RequiredArgsConstructor
class S3InterviewVideoStorageAdapter implements InterviewVideoStorage {

    // 녹화본은 webm 컨테이너로 고정한다(s3-policy.md의 recording/raw.webm). 서명에 포함되므로 업로드 시 동일 헤더 필요.
    private static final String CONTENT_TYPE = "video/webm";
    // presigned URL 자체의 서명 유효시간. 콘텐츠 접근 만료(video_expires_at)와는 별개다(s3-policy.md §2).
    private static final Duration UPLOAD_TTL = Duration.ofMinutes(10);
    private static final Duration PLAYBACK_TTL = Duration.ofMinutes(10);

    private final S3Presigner s3Presigner;
    private final S3Properties s3Properties;

    @Override
    public PresignedUpload presignUpload(UUID userId, Long sessionId) {
        String key = S3KeyGenerator.interviewRecordingKey(userId, sessionId);
        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(s3Properties.getBucket())
                .key(key)
                .contentType(CONTENT_TYPE)
                .build();
        PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                .signatureDuration(UPLOAD_TTL)
                .putObjectRequest(putObjectRequest)
                .build();

        String url = s3Presigner.presignPutObject(presignRequest).url().toString();
        return new PresignedUpload(url, CONTENT_TYPE, UPLOAD_TTL.toSeconds());
    }

    @Override
    public String presignPlayback(UUID userId, Long sessionId) {
        String key = S3KeyGenerator.interviewRecordingKey(userId, sessionId);
        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(s3Properties.getBucket())
                .key(key)
                .build();
        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(PLAYBACK_TTL)
                .getObjectRequest(getObjectRequest)
                .build();

        return s3Presigner.presignGetObject(presignRequest).url().toString();
    }
}
