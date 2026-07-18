package com.yapp.d14.interview.application.service;

import com.yapp.d14.interview.application.port.in.InterviewVideoRetentionExtendUseCase;
import com.yapp.d14.interview.application.port.out.InterviewVideoRepository;
import com.yapp.d14.interview.domain.InterviewVideo;
import com.yapp.d14.interview.domain.VideoRetentionTrigger;
import com.yapp.d14.interview.exception.InterviewErrorCode;
import com.yapp.d14.interview.exception.InterviewException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
class InterviewVideoRetentionExtendService implements InterviewVideoRetentionExtendUseCase {

    private final InterviewVideoRepository interviewVideoRepository;

    @Override
    public void extendForShareRequested(Long sessionId) {
        extend(sessionId, VideoRetentionTrigger.SHARE_REQUESTED);
    }

    @Override
    public void extendForGuestFirstViewed(Long sessionId) {
        extend(sessionId, VideoRetentionTrigger.GUEST_FIRST_VIEWED);
    }

    @Override
    public void extendForGuestFirstSubmitted(Long sessionId) {
        extend(sessionId, VideoRetentionTrigger.GUEST_FIRST_SUBMITTED);
    }

    private void extend(Long sessionId, VideoRetentionTrigger trigger) {
        InterviewVideo video = interviewVideoRepository.findBySessionId(sessionId)
                .orElseThrow(() -> new InterviewException(InterviewErrorCode.INTERVIEW_VIDEO_NOT_FOUND));
        video.extend(trigger);
        interviewVideoRepository.save(video);
    }
}
