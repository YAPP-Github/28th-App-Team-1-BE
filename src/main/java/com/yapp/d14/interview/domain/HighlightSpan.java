package com.yapp.d14.interview.domain;

import java.util.List;

// title: 하이라이트 한 줄 제목(명사구). GOOD은 잘한 점을, IMPROVE는 핵심 문제/개선점을 명명한다(#78).
// reason: 개선유형. 이 값으로 리포트 카드의 마무리 안내(A/B/C)와 followUpQuestions 노출 여부가 결정된다.
// followUpQuestions: 이 하이라이트 구간에 대해 면접관이 이어서 던질 법한 추가 질문. reason=PROBE_WORTHY일 때만 채워진다.
// answerTopicTitle: 딴 답(reason=OFF_INTENT)일 때, 답변이 실제로 다룬 주제를 짧은 명사구로 요약한 값
// (예: "팀 내 신뢰와 성향"). "질문 의도 ↔ 내 답변" 대비 UI의 '내 답변' 자리에 쓴다. OFF_INTENT가 아니면 null.
public record HighlightSpan(
        TextRange range,
        HighlightTone tone,
        HighlightReason reason,
        String title,
        String analysis,
        List<String> followUpQuestions,
        String answerTopicTitle
) {
}
