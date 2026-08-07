package com.yapp.d14.interview.adapter.out.integration.aws;

import com.yapp.d14.common.properties.S3Properties;
import com.yapp.d14.common.util.S3KeyGenerator;
import com.yapp.d14.interview.application.port.out.InterviewSessionFileCleaner;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.Delete;
import software.amazon.awssdk.services.s3.model.DeleteObjectsRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectsResponse;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.ObjectIdentifier;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
class S3InterviewSessionFileCleanerAdapter implements InterviewSessionFileCleaner {

    private final S3Client s3Client;
    private final S3Properties s3Properties;

    @Override
    public int deleteSessionFiles(UUID userId, Long sessionId) {
        String prefix = S3KeyGenerator.interviewSessionPrefix(userId, sessionId);
        ListObjectsV2Request listRequest = ListObjectsV2Request.builder()
                .bucket(s3Properties.getBucket())
                .prefix(prefix)
                .build();

        int deleted = 0;
        // 페이지당 최대 1000건이고 DeleteObjects도 1회 1000건이라, 페이지 단위로 그대로 넘긴다.
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

        // DeleteObjects는 일부만 실패해도 200을 주므로, 남은 게 있으면 정리 완료로 표시되지 않도록 예외로 올린다.
        if (response.hasErrors() && !response.errors().isEmpty()) {
            throw S3Exception.builder()
                    .message("세션 파일 일부를 삭제하지 못했어요: " + response.errors().getFirst().key())
                    .build();
        }
    }
}
