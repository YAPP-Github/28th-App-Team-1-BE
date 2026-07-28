package com.yapp.d14.interview.domain;

// STT(Whisper)가 끊은 발화 세그먼트 하나 — 대체로 문장(또는 절) 단위다. text는 그 구간의 대본,
// startSec/endSec는 해당 음성(답변/질문 TTS) 시작 기준 상대 발화 시각(초)이다. 영상 타임라인 값은 아니며,
// 문장 단위 발화 시각 매핑(#78) 시 답변은 answerStartSec, 질문은 questionStartSec만큼 더해 보정한다.
public record TranscriptSegment(String text, float startSec, float endSec) {
}
