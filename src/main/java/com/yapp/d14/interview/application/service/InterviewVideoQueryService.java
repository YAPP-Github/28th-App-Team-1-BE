package com.yapp.d14.interview.application.service;

import com.yapp.d14.interview.application.port.in.InterviewSessionOwnerQueryUseCase;
import com.yapp.d14.interview.application.port.in.InterviewVideoQueryUseCase;
import com.yapp.d14.interview.application.port.in.result.InterviewVideoPlaybackResult;
import com.yapp.d14.interview.application.port.in.result.InterviewVideoStatusResult;
import com.yapp.d14.interview.application.port.out.InterviewVideoRepository;
import com.yapp.d14.interview.application.port.out.InterviewVideoStorage;
import com.yapp.d14.interview.domain.InterviewVideo;
import com.yapp.d14.interview.exception.InterviewErrorCode;
import com.yapp.d14.interview.exception.InterviewException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
class InterviewVideoQueryService implements InterviewVideoQueryUseCase {

    private final InterviewVideoRepository interviewVideoRepository;
    private final InterviewVideoStorage interviewVideoStorage;
    private final InterviewSessionOwnerQueryUseCase interviewSessionOwnerQueryUseCase;

    @Override
    public InterviewVideoStatusResult getStatus(Long sessionId) {
        InterviewVideo video = interviewVideoRepository.findBySessionId(sessionId)
                .orElseThrow(() -> new InterviewException(InterviewErrorCode.INTERVIEW_VIDEO_NOT_FOUND));
        return new InterviewVideoStatusResult(video.getExpiresAt(), video.isExpired());
    }

    @Override
    public InterviewVideoPlaybackResult getPlayback(Long sessionId) {
        InterviewVideo video = interviewVideoRepository.findBySessionId(sessionId)
                .orElseThrow(() -> new InterviewException(InterviewErrorCode.INTERVIEW_VIDEO_NOT_FOUND));
        // URL이 필요할 때만(합성 완료+만료 전) 소유자 조회·presign을 수행한다 — 그 외엔 헛작업을 하지 않는다.
        String playbackUrl = null;
        if (video.isComposited() && !video.isExpired()) {
            UUID ownerUserId = interviewSessionOwnerQueryUseCase.getOwnerUserId(sessionId);
            playbackUrl = interviewVideoStorage.presignComposite(ownerUserId, sessionId);
        }
        return new InterviewVideoPlaybackResult(video.getExpiresAt(), video.isExpired(), playbackUrl);
    }
}
