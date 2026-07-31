package com.yapp.d14.interview.application.port.out;

import com.yapp.d14.interview.domain.TranscriptSegment;

import java.util.List;

// Whisper verbose_json 응답 1건의 결과. totalSegmentCount/failedSegmentCount는 5-2장 STT 누적 인식률 계산에 쓰인다
// (failedSegmentCount = no_speech_prob > 0.6인 세그먼트 수).
// segments는 발화 세그먼트별 텍스트·시각으로, 문장 단위 발화 시각 매핑(#78)에 쓰인다 — 없으면 빈 리스트.
public record TranscriptionResult(
        String text, int totalSegmentCount, int failedSegmentCount, List<TranscriptSegment> segments
) {

    // 세그먼트 시각이 필요 없는(또는 없는) 호출부용 편의 생성자 — segments를 빈 리스트로 둔다.
    public TranscriptionResult(String text, int totalSegmentCount, int failedSegmentCount) {
        this(text, totalSegmentCount, failedSegmentCount, List.of());
    }
}
