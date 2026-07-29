package com.yapp.d14.interview.application.service;

import com.yapp.d14.common.util.AfterCommitExecutor;
import com.yapp.d14.interview.application.port.in.InterviewSessionOwnershipCheckUseCase;
import com.yapp.d14.interview.application.port.in.InterviewVideoCompositeUseCase;
import com.yapp.d14.interview.application.port.in.InterviewVideoUploadCompleteUseCase;
import com.yapp.d14.interview.application.port.out.InterviewVideoRepository;
import com.yapp.d14.interview.domain.InterviewVideo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
class InterviewVideoUploadCompleteService implements InterviewVideoUploadCompleteUseCase {

    private final InterviewSessionOwnershipCheckUseCase interviewSessionOwnershipCheckUseCase;
    private final InterviewVideoRepository interviewVideoRepository;
    private final InterviewVideoCompositeUseCase interviewVideoCompositeUseCase;

    // 업로드가 리포트 채점보다 먼저 끝날 수 있어(둘 다 종료 후 비동기), 레코드가 없으면 보관 타이머와 함께 만든다.
    // DB upsert(ON CONFLICT DO UPDATE SET uploaded)라 채점의 최초 INSERT와 동시에 실행돼도 충돌하지 않고,
    // uploaded 한 컬럼만 건드려 보관기간 연장과의 Lost Update도 없다.
    @Override
    @Transactional
    public void complete(UUID userId, Long sessionId, Float wrapUpStartSec, Float wrapUpEndSec) {
        // TODO(step3): wrapUpStartSec/wrapUpEndSec을 InterviewVideo에 저장해 합성·대본에 마무리 멘트를 얹는다.
        interviewSessionOwnershipCheckUseCase.requireOwned(userId, sessionId);
        interviewVideoRepository.upsertUploaded(InterviewVideo.create(sessionId, LocalDateTime.now()));
        // raw.mp4가 S3에 올라온 지금이 합성의 유일한 blocker 해소 시점이다(질문 타임스탬프는 면접 중 이미 기록됨).
        // 마킹 대상 row가 커밋된 뒤에만 합성을 트리거해, 롤백 시 헛작업과 markComposited 0행 갱신을 막는다.
        AfterCommitExecutor.runAfterCommit(() -> triggerComposite(userId, sessionId));
    }

    // 합성 큐가 가득 차면 @Async 디스패치가 RejectedExecutionException을 커밋 이후 afterCommit 콜백으로 던진다.
    // 여기서 삼키지 않으면 이미 커밋된 업로드 완료가 500으로 응답되므로, 트리거 실패는 로깅만 하고 흘려보낸다.
    private void triggerComposite(UUID userId, Long sessionId) {
        try {
            interviewVideoCompositeUseCase.composite(userId, sessionId);
        } catch (Exception e) {
            log.error("[COMPOSITE] 합성 트리거 실패(업로드 완료는 정상): sessionId={}", sessionId, e);
        }
    }
}
