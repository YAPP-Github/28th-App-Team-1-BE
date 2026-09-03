package com.yapp.d14.interview.adapter.out.integration.media;

// 합성 실패 이유를 계측까지 그대로 전달하려고 둔다. 실패 세 종류가 모두 IllegalStateException이라
// 예외 타입으로 못 가르고, 메시지 문자열 매칭은 문구가 바뀌면 깨지기 때문.
// IllegalStateException을 그대로 상속하므로 호출부가 보는 동작은 이전과 같다.
class VideoCompositeFailedException extends IllegalStateException {

    private final transient String reason;

    VideoCompositeFailedException(String reason, String message) {
        super(message);
        this.reason = reason;
    }

    String reason() {
        return reason;
    }
}
