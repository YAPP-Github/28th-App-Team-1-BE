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

        // fromBytes는 방어적으로 배열을 복제한다. 재시도마다 새로 만들면 매번 복제가 반복되므로
        // 루프 밖에서 한 번만 만들어 재사용한다(byte[] 기반 RequestBody는 재사용 가능).
        RequestBody requestBody = RequestBody.fromBytes(content);

        SdkException lastException = null;
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                s3Client.putObject(request, requestBody);
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
