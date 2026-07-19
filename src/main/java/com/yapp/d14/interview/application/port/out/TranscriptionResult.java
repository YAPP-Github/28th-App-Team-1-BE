package com.yapp.d14.interview.application.port.out;

// Whisper verbose_json 응답 1건의 결과. totalSegmentCount/failedSegmentCount는 5-2장 STT 누적 인식률 계산에 쓰인다
// (failedSegmentCount = no_speech_prob > 0.6인 세그먼트 수).
public record TranscriptionResult(String text, int totalSegmentCount, int failedSegmentCount) {
}
