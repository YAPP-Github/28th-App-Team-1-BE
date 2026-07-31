package com.yapp.d14.interview.domain;

// 대본(질문/답변) 한 문장(발화 세그먼트) 단위의 발화 구간. startIndex/endIndex는 role에 해당하는 대본 문자열
// (질문=questionText, 답변=transcript) 기준 문자 오프셋이라 text = 대본.substring(startIndex, endIndex)이고,
// startSec/endSec는 합성 영상(=녹화) 타임라인 기준 발화 시각(초)이다.
public record UtteranceSegment(
        ScriptRole role, String text, int startIndex, int endIndex, float startSec, float endSec
) {
}
