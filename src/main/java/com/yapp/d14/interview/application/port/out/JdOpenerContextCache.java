package com.yapp.d14.interview.application.port.out;

import java.util.Optional;

// preload 시점에 계산해둔 JD∩포폴 오프너 소재를, 라이브 턴 중 opener 생성 시 재조회 없이 재사용하기 위한 캐시.
public interface JdOpenerContextCache {

    Optional<JdOpenerContext> get(Long sessionId);

    void save(Long sessionId, JdOpenerContext context);

    // 세션 종료(정상 종료/STT_RESET) 시 캐시를 정리한다.
    void clear(Long sessionId);
}
