package com.yapp.d14.interview.application.port.out;

// 캐물지점을 고르는 순간의 후보 풀 크기를 남긴다(E-8).
// 풀이 1개뿐이면 "채택"이 아니라 사실상 강제 선택이므로, 그 비율이 면접 깊이의 신호가 된다.
// 계측 기술(Micrometer)은 어댑터가 알고, Application 레이어는 이 포트만 본다.
public interface ProbeSelectionRecorder {

    void recordPoolSize(int poolSize);
}
