package com.yapp.d14.interview.adapter.out.integration.aws;

import com.yapp.d14.common.metrics.S3Call;
import com.yapp.d14.common.metrics.S3Metrics;
import com.yapp.d14.common.properties.S3Properties;
import com.yapp.d14.common.util.S3KeyGenerator;
import com.yapp.d14.interview.application.port.out.InterviewVoiceStorage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.Delete;
import software.amazon.awssdk.services.s3.model.DeleteObjectsRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectsResponse;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.ObjectIdentifier;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.util.Base64;
import java.util.List;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
class S3InterviewVoiceStorageAdapter implements InterviewVoiceStorage {

    private static final int MAX_ATTEMPTS = 3;
    private static final long BASE_BACKOFF_MILLIS = 200L;
    private static final String CONTENT_TYPE = "audio/mpeg";
    private static final String ANSWER_CONTENT_TYPE = "audio/mp4";

    private final S3Client s3Client;
    private final S3Properties s3Properties;
    private final S3Metrics s3Metrics;

    @Override
    public String upload(UUID userId, Long sessionId, int turnLevel, byte[] audioContent) {
        return upload(S3KeyGenerator.interviewVoiceKey(userId, sessionId, turnLevel), audioContent);
    }

    @Override
    public String upload(String key, byte[] audioContent) {
        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(s3Properties.getBucket())
                .key(key)
                .contentType(CONTENT_TYPE)
                .build();
        RequestBody requestBody = RequestBody.fromBytes(audioContent);

        SdkException lastException = null;
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                s3Metrics.run(S3Call.VOICE_PUT, () -> s3Client.putObject(request, requestBody));
                return key;
            } catch (SdkException e) {
                lastException = e;
                log.warn("[INTERVIEW VOICE UPLOAD] {}번째 시도 실패: key={}", attempt, key, e);
                if (attempt < MAX_ATTEMPTS && !sleepBackoff(attempt)) {
                    break;
                }
            }
        }
        throw lastException;
    }

    @Override
    @Async("audioArchiveTaskExecutor")
    public void uploadAsync(UUID userId, Long sessionId, int turnLevel, byte[] audioContent) {
        String key = S3KeyGenerator.interviewVoiceKey(userId, sessionId, turnLevel);

        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(s3Properties.getBucket())
                .key(key)
                .contentType(CONTENT_TYPE)
                .build();
        RequestBody requestBody = RequestBody.fromBytes(audioContent);

        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                s3Metrics.run(S3Call.VOICE_PUT, () -> s3Client.putObject(request, requestBody));
                return;
            } catch (SdkException e) {
                log.warn("[INTERVIEW VOICE UPLOAD ASYNC] {}번째 시도 실패: key={}", attempt, key, e);
                if (attempt < MAX_ATTEMPTS && !sleepBackoff(attempt)) {
                    break;
                }
            }
        }
        s3Metrics.abandoned(S3Call.VOICE_PUT);
        log.error("[INTERVIEW VOICE UPLOAD ASYNC] 재시도 소진, 업로드 실패: key={}", key);
    }

    @Override
    @Async("audioArchiveTaskExecutor")
    public void uploadAnswerAsync(UUID userId, Long sessionId, int turnLevel, byte[] audioContent) {
        String key = S3KeyGenerator.interviewAnswerKey(userId, sessionId, turnLevel);

        // 컨테이너는 클라이언트가 올린 원본대로 저장한다. content-type은 메타데이터일 뿐이며,
        // 합성 시 ffmpeg가 파일 내용으로 포맷을 판별하므로 재생/합성에는 영향을 주지 않는다.
        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(s3Properties.getBucket())
                .key(key)
                .contentType(ANSWER_CONTENT_TYPE)
                .build();
        RequestBody requestBody = RequestBody.fromBytes(audioContent);

        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                s3Metrics.run(S3Call.ANSWER_PUT, () -> s3Client.putObject(request, requestBody));
                return;
            } catch (SdkException e) {
                log.warn("[INTERVIEW ANSWER UPLOAD] {}번째 시도 실패: key={}", attempt, key, e);
                if (attempt < MAX_ATTEMPTS && !sleepBackoff(attempt)) {
                    break;
                }
            }
        }
        s3Metrics.abandoned(S3Call.ANSWER_PUT);
        log.error("[INTERVIEW ANSWER UPLOAD] 재시도 소진, 업로드 실패: key={}", key);
    }

    // 합성 성공 후 답변 음성(answers/ 하위)을 정리한다. 세션당 턴 수만큼이라 한 페이지(1000개)로 충분하지만
    // 페이지네이터로 순회해 안전하게 처리한다. 부분 실패는 예외로 알려 호출부(합성 서비스)가 삼키고 로깅하게 한다.
    @Override
    public int deleteAnswerAudio(UUID userId, Long sessionId) {
        String prefix = S3KeyGenerator.interviewAnswerPrefix(userId, sessionId);
        return s3Metrics.record(S3Call.ANSWER_DELETE, () -> deleteByPrefix(prefix));
    }

    private int deleteByPrefix(String prefix) {
        ListObjectsV2Request listRequest = ListObjectsV2Request.builder()
                .bucket(s3Properties.getBucket())
                .prefix(prefix)
                .build();

        int deleted = 0;
        for (ListObjectsV2Response page : s3Client.listObjectsV2Paginator(listRequest)) {
            List<ObjectIdentifier> keys = page.contents().stream()
                    .map(object -> ObjectIdentifier.builder().key(object.key()).build())
                    .toList();
            if (keys.isEmpty()) {
                continue;
            }
            deleteBatch(keys);
            deleted += keys.size();
        }
        return deleted;
    }

    private void deleteBatch(List<ObjectIdentifier> keys) {
        DeleteObjectsResponse response = s3Client.deleteObjects(DeleteObjectsRequest.builder()
                .bucket(s3Properties.getBucket())
                .delete(Delete.builder().objects(keys).build())
                .build());

        if (response.hasErrors() && !response.errors().isEmpty()) {
            throw S3Exception.builder()
                    .message("답변 음성 일부를 삭제하지 못했어요: " + response.errors().getFirst().key())
                    .build();
        }
    }

    private boolean sleepBackoff(int attempt) {
        try {
            Thread.sleep(BASE_BACKOFF_MILLIS << (attempt - 1));
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    @Override
    public String readBase64(String s3Key) {
        try {
            byte[] content = s3Metrics.record(S3Call.VOICE_GET,
                    () -> s3Client.getObjectAsBytes(GetObjectRequest.builder()
                            .bucket(s3Properties.getBucket())
                            .key(s3Key)
                            .build())
                    .asByteArray());
            return Base64.getEncoder().encodeToString(content);
        } catch (SdkException e) {
            log.warn("[INTERVIEW VOICE READ] S3 조회 실패: key={}", s3Key, e);
            return null;
        }
    }
}
