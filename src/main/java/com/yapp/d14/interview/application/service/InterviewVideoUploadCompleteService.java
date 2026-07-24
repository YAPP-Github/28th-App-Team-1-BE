package com.yapp.d14.interview.application.service;

import com.yapp.d14.interview.application.port.in.InterviewSessionOwnershipCheckUseCase;
import com.yapp.d14.interview.application.port.in.InterviewVideoUploadCompleteUseCase;
import com.yapp.d14.interview.application.port.out.InterviewVideoRepository;
import com.yapp.d14.interview.domain.InterviewVideo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
class InterviewVideoUploadCompleteService implements InterviewVideoUploadCompleteUseCase {

    private final InterviewSessionOwnershipCheckUseCase interviewSessionOwnershipCheckUseCase;
    private final InterviewVideoRepository interviewVideoRepository;

    // 업로드가 리포트 채점보다 먼저 끝날 수 있어(둘 다 종료 후 비동기), 레코드가 없으면 보관 타이머와 함께 만든다.
    // DB upsert(ON CONFLICT DO UPDATE SET uploaded)라 채점의 최초 INSERT와 동시에 실행돼도 충돌하지 않고,
    // uploaded 한 컬럼만 건드려 보관기간 연장과의 Lost Update도 없다.
    @Override
    @Transactional
    public void complete(UUID userId, Long sessionId) {
        interviewSessionOwnershipCheckUseCase.requireOwned(userId, sessionId);
        interviewVideoRepository.upsertUploaded(InterviewVideo.create(sessionId, LocalDateTime.now()));
    }
}
