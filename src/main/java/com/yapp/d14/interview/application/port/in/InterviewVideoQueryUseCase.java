package com.yapp.d14.interview.application.port.in;

import com.yapp.d14.interview.application.port.in.result.InterviewVideoStatusResult;

public interface InterviewVideoQueryUseCase {

    InterviewVideoStatusResult getStatus(Long sessionId);

    /**
     * 합성 완료본(final.mp4) 재생용 presigned URL을 반환한다.
     * 합성이 끝났고(composited) 만료 전일 때만 URL을 주고, 그 외에는 null이다(합성 전/실패/만료).
     */
    String getPlaybackUrl(Long sessionId);
}
