package com.yapp.d14.interview.adapter.out.integration.aws;

import com.yapp.d14.common.properties.S3Properties;
import com.yapp.d14.interview.application.port.out.InterviewVoiceStorage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.util.Base64;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
class S3InterviewVoiceStorageAdapter implements InterviewVoiceStorage {

    private static final int MAX_ATTEMPTS = 3;
    private static final String CONTENT_TYPE = "audio/mpeg";

    private final S3Client s3Client;
    private final S3Properties s3Properties;

    @Override
    public String upload(UUID userId, Long sessionId, int turnLevel, byte[] audioContent) {
        String key = buildKey(userId, sessionId, turnLevel);

        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(s3Properties.getBucket())
                .key(key)
                .contentType(CONTENT_TYPE)
                .build();
        RequestBody requestBody = RequestBody.fromBytes(audioContent);

        SdkException lastException = null;
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                s3Client.putObject(request, requestBody);
                return key;
            } catch (SdkException e) {
                lastException = e;
                log.warn("[INTERVIEW VOICE UPLOAD] {}번째 시도 실패: key={}", attempt, key, e);
            }
        }
        throw lastException;
    }

    @Override
    public String readBase64(String s3Key) {
        byte[] content = s3Client.getObjectAsBytes(GetObjectRequest.builder()
                        .bucket(s3Properties.getBucket())
                        .key(s3Key)
                        .build())
                .asByteArray();
        return Base64.getEncoder().encodeToString(content);
    }

    private String buildKey(UUID userId, Long sessionId, int turnLevel) {
        return "users/%s/sessions/%s/questions/%s.mp3".formatted(userId, sessionId, turnLevel);
    }
}
