package com.yapp.d14.interview.application.port.in;

import com.yapp.d14.interview.application.port.in.result.InterviewVideoPlaybackResult;
import com.yapp.d14.interview.application.port.in.result.InterviewVideoStatusResult;

public interface InterviewVideoQueryUseCase {

    /** 소유자(면접자)가 자기 리포트/공유 상태에서 보는 만료 상태. 단계형 expiresAt 기준. */
    InterviewVideoStatusResult getOwnerStatus(Long sessionId);

    /** 지인(공유 링크) 접근 판정용 만료 상태. 소유자 쪽 단계형 expiresAt과 무관하게 baseAt+30일 하드캡 기준. */
    InterviewVideoStatusResult getGuestStatus(Long sessionId);

    /**
     * 게이트 판정용 상태(만료 여부)와 재생 URL을 한 번의 조회로 묶어 반환한다.
     * playbackUrl은 합성이 끝났고(composited) 만료 전일 때만 채워지고, 그 외에는 null이다(합성 전/실패/만료).
     * 지인(공유 링크) 접근 전용이며, 만료 판정은 baseAt+30일 하드캡 기준이다.
     */
    InterviewVideoPlaybackResult getPlayback(Long sessionId);
}
