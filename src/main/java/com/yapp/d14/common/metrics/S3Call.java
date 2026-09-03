package com.yapp.d14.common.metrics;

// S3 호출 지점(Hilit-BE#178). 값이 Prometheus 태그로 그대로 나가므로 바꾸면 기존 시계열과 끊긴다.
// 목록 조회(list)는 따로 두지 않는다 — 페이지네이터가 지연 평가라 페이지별로 나눠 재기 어렵고,
// 실제로 궁금한 것은 "정리가 끝났는가"라는 한 덩어리라 delete 안에 포함해 잰다.
public enum S3Call {

    VOICE_PUT("put", "voice"),
    VOICE_GET("get", "voice"),
    ANSWER_PUT("put", "answer"),
    ANSWER_DELETE("delete", "answer"),
    PORTFOLIO_PUT("put", "portfolio"),
    PORTFOLIO_DELETE("delete", "portfolio"),
    SESSION_DELETE("delete", "session");

    private final String operation;
    private final String area;

    S3Call(String operation, String area) {
        this.operation = operation;
        this.area = area;
    }

    String operation() {
        return operation;
    }

    String area() {
        return area;
    }
}
