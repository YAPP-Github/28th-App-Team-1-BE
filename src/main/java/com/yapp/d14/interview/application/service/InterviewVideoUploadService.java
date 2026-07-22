package com.yapp.d14.interview.application.service;

import com.yapp.d14.interview.application.port.in.InterviewSessionOwnershipCheckUseCase;
import com.yapp.d14.interview.application.port.in.InterviewVideoUploadCompleteUseCase;
import com.yapp.d14.interview.application.port.in.InterviewVideoUploadUrlIssueUseCase;
import com.yapp.d14.interview.application.port.in.result.InterviewVideoUploadUrlResult;
import com.yapp.d14.interview.application.port.out.InterviewVideoRepository;
import com.yapp.d14.interview.application.port.out.InterviewVideoStorage;
import com.yapp.d14.interview.application.port.out.InterviewVideoStorage.PresignedUpload;
import com.yapp.d14.interview.domain.InterviewVideo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
class InterviewVideoUploadService implements InterviewVideoUploadUrlIssueUseCase, InterviewVideoUploadCompleteUseCase {

    private final InterviewSessionOwnershipCheckUseCase interviewSessionOwnershipCheckUseCase;
    private final InterviewVideoStorage interviewVideoStorage;
    private final InterviewVideoRepository interviewVideoRepository;

    @Override
    public InterviewVideoUploadUrlResult issue(UUID userId, Long sessionId) {
        interviewSessionOwnershipCheckUseCase.requireOwned(userId, sessionId);
        PresignedUpload upload = interviewVideoStorage.presignUpload(userId, sessionId);
        return new InterviewVideoUploadUrlResult(upload.url(), upload.contentType(), upload.expiresInSeconds());
    }

    // 업로드가 리포트 채점보다 먼저 끝날 수 있어(둘 다 종료 후 비동기), 아직 보관 레코드가 없으면 여기서 만든다.
    // 이후 리포트 채점의 ensureVideoRetentionStarted는 레코드가 있으면 건너뛰므로 중복 생성되지 않는다.
    @Override
    @Transactional
    public void complete(UUID userId, Long sessionId) {
        interviewSessionOwnershipCheckUseCase.requireOwned(userId, sessionId);
        InterviewVideo video = interviewVideoRepository.findBySessionIdForUpdate(sessionId)
                .orElseGet(() -> InterviewVideo.create(sessionId, LocalDateTime.now()));
        video.markUploaded();
        interviewVideoRepository.save(video);
    }
}
