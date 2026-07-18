package com.yapp.d14.interview.application.service;

import com.yapp.d14.interview.application.port.in.InterviewVideoRetentionExtendUseCase;
import com.yapp.d14.interview.application.port.out.InterviewVideoRepository;
import com.yapp.d14.interview.domain.InterviewVideo;
import com.yapp.d14.interview.domain.VideoRetentionTrigger;
import com.yapp.d14.interview.exception.InterviewErrorCode;
import com.yapp.d14.interview.exception.InterviewException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
class InterviewVideoRetentionExtendService implements InterviewVideoRetentionExtendUseCase {

    private final InterviewVideoRepository interviewVideoRepository;

    @Override
    @Transactional
    public void extendForShareRequested(Long sessionId) {
        extend(sessionId, VideoRetentionTrigger.SHARE_REQUESTED);
    }

    @Override
    @Transactional
    public void extendForGuestFirstViewed(Long sessionId) {
        extend(sessionId, VideoRetentionTrigger.GUEST_FIRST_VIEWED);
    }

    @Override
    @Transactional
    public void extendForGuestFirstSubmitted(Long sessionId) {
        extend(sessionId, VideoRetentionTrigger.GUEST_FIRST_SUBMITTED);
    }

    // 서로 다른 트리거(공유 요청/최초 열람/최초 제출)가 동시에 발생해도 Lost Update 없이
    // 항상 더 긴 보존 기간이 반영되도록 락을 잡고 조회한다.
    private void extend(Long sessionId, VideoRetentionTrigger trigger) {
        InterviewVideo video = interviewVideoRepository.findBySessionIdForUpdate(sessionId)
                .orElseThrow(() -> new InterviewException(InterviewErrorCode.INTERVIEW_VIDEO_NOT_FOUND));
        video.extend(trigger);
        interviewVideoRepository.save(video);
    }
}
