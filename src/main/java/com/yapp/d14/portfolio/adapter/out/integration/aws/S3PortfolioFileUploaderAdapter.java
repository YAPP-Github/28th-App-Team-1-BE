package com.yapp.d14.portfolio.adapter.out.integration.aws;

import com.yapp.d14.common.properties.S3Properties;
import com.yapp.d14.portfolio.application.port.out.PortfolioFileUploader;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@Slf4j
@Component
@RequiredArgsConstructor
class S3PortfolioFileUploaderAdapter implements PortfolioFileUploader {

    private static final int MAX_ATTEMPTS = 3;

    private final S3Client s3Client;
    private final S3Properties s3Properties;

    @Override
    public void upload(String key, byte[] content, String contentType) {
        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(s3Properties.getBucket())
                .key(key)
                .contentType(contentType)
                .build();

        SdkException lastException = null;
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                s3Client.putObject(request, RequestBody.fromBytes(content));
                return;
            } catch (SdkException e) {
                lastException = e;
                log.warn("[S3 UPLOAD] {}번째 시도 실패: key={}", attempt, key, e);
            }
        }
        throw lastException;
    }

    @Override
    public void delete(String key) {
        try {
            s3Client.deleteObject(DeleteObjectRequest.builder()
                    .bucket(s3Properties.getBucket())
                    .key(key)
                    .build());
        } catch (SdkException e) {
            log.error("[S3 DELETE] 삭제 실패, 고아 파일로 남을 수 있음: key={}", key, e);
        }
    }
}
