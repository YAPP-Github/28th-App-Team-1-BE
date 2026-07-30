package com.yapp.d14.interview.application.service;

import com.yapp.d14.common.util.AfterCommitExecutor;
import com.yapp.d14.interview.application.command.InterviewVideoUploadCompleteCommand;
import com.yapp.d14.interview.application.port.in.InterviewSessionOwnershipCheckUseCase;
import com.yapp.d14.interview.application.port.in.InterviewVideoCompositeUseCase;
import com.yapp.d14.interview.application.port.in.InterviewVideoUploadCompleteUseCase;
import com.yapp.d14.interview.application.port.out.InterviewVideoRepository;
import com.yapp.d14.interview.domain.InterviewVideo;
import com.yapp.d14.interview.exception.InterviewErrorCode;
import com.yapp.d14.interview.exception.InterviewException;
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
    public void complete(InterviewVideoUploadCompleteCommand command) {
        Long sessionId = command.sessionId();
        UUID userId = command.userId();
        interviewSessionOwnershipCheckUseCase.requireOwned(userId, sessionId);
        validateWrapUpRange(command.wrapUpStartSec(), command.wrapUpEndSec());
        // 마무리 멘트 재생 구간을 함께 저장해 합성(Step4)·대본(Step5)에서 마무리 멘트를 얹는다. 없으면 null.
        interviewVideoRepository.upsertUploaded(
                InterviewVideo.create(sessionId, LocalDateTime.now(), command.wrapUpStartSec(), command.wrapUpEndSec()));
        // raw.mp4가 S3에 올라온 지금이 합성의 유일한 blocker 해소 시점이다(질문 타임스탬프는 면접 중 이미 기록됨).
        // 마킹 대상 row가 커밋된 뒤에만 합성을 트리거해, 롤백 시 헛작업과 markComposited 0행 갱신을 막는다.
        AfterCommitExecutor.runAfterCommit(() -> triggerComposite(userId, sessionId));
    }

    // 마무리 멘트가 없으면(조기 종료 등) 둘 다 null만 허용하고, 있으면 둘 다 있어야 하며 0 <= start < end여야 한다.
    // 한쪽만 있으면 대본에 endSec=null인 불완전한 구간이 노출될 수 있어 여기서 막는다.
    private void validateWrapUpRange(Float wrapUpStartSec, Float wrapUpEndSec) {
        if (wrapUpStartSec == null && wrapUpEndSec == null) {
            return;
        }
        if (wrapUpStartSec == null || wrapUpEndSec == null || wrapUpStartSec < 0 || wrapUpStartSec >= wrapUpEndSec) {
            throw new InterviewException(InterviewErrorCode.INVALID_WRAP_UP_RANGE);
        }
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
