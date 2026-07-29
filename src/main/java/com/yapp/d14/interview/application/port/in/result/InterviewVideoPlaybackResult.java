package com.yapp.d14.interview.application.port.in.result;

import java.time.LocalDateTime;

/**
 * 영상 재생 정보를 한 번의 조회로 묶어 반환한다(게이트 판정용 상태 + 재생 URL).
 * playbackUrl은 합성 완료(composited)+만료 전일 때만 채워지고, 그 외에는 null이다(합성 전/실패/만료).
 */
public record InterviewVideoPlaybackResult(
        LocalDateTime expiresAt,
        boolean expired,
        String playbackUrl
) {
}
