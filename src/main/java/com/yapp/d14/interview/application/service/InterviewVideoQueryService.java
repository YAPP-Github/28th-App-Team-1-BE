package com.yapp.d14.interview.application.service;

import com.yapp.d14.interview.application.port.in.InterviewVideoQueryUseCase;
import com.yapp.d14.interview.application.port.in.result.InterviewVideoStatusResult;
import com.yapp.d14.interview.application.port.out.InterviewVideoRepository;
import com.yapp.d14.interview.domain.InterviewVideo;
import com.yapp.d14.interview.exception.InterviewErrorCode;
import com.yapp.d14.interview.exception.InterviewException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
class InterviewVideoQueryService implements InterviewVideoQueryUseCase {

    private final InterviewVideoRepository interviewVideoRepository;

    @Override
    public InterviewVideoStatusResult getStatus(Long sessionId) {
        InterviewVideo video = interviewVideoRepository.findBySessionId(sessionId)
                .orElseThrow(() -> new InterviewException(InterviewErrorCode.INTERVIEW_VIDEO_NOT_FOUND));
        return new InterviewVideoStatusResult(video.getExpiresAt(), video.isExpired());
    }
}
