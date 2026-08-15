package com.yapp.d14.auth.adapter.out.integration.kakao;

import lombok.Getter;

@Getter
class KakaoErrorResponse {

    // 카카오 API 에러 코드: 앱과 연결되어 있지 않은 사용자(NotRegisteredUserException)
    private static final int NOT_REGISTERED_USER_CODE = -101;

    private Integer code;
    private String msg;

    boolean isNotRegisteredUser() {
        return code != null && code == NOT_REGISTERED_USER_CODE;
    }
}
