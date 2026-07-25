package com.yapp.d14.interview.application.port.in;

import com.yapp.d14.interview.application.port.in.result.InterviewVideoPlaybackResult;
import com.yapp.d14.interview.application.port.in.result.InterviewVideoStatusResult;

public interface InterviewVideoQueryUseCase {

    InterviewVideoStatusResult getStatus(Long sessionId);

    /**
     * 게이트 판정용 상태(만료 여부)와 재생 URL을 한 번의 조회로 묶어 반환한다.
     * playbackUrl은 합성이 끝났고(composited) 만료 전일 때만 채워지고, 그 외에는 null이다(합성 전/실패/만료).
     */
    InterviewVideoPlaybackResult getPlayback(Long sessionId);
}
