package com.yapp.d14.interview.domain;

// 하이라이트 구간의 개선유형(#78). 리포트 카드에서 어떤 마무리 안내를 보여줄지(A/B/C)를 결정한다.
// - PROBE_WORTHY: 질문에 정면으로 답했고 더 파고들 여지가 있음 → followUpQuestions 노출(케이스 A)
// - OFF_INTENT:   질문이 요구한 것과 다른 답(딴 답) → 질문 의도 리마인드(케이스 B)
// - SHALLOW:      방향은 맞지만 너무 짧/얕아 캐물 실마리가 없음 → "더 자세히" 코칭(케이스 C, IMPROVE)
// - SUFFICIENT:   원인·한계·결과까지 스스로 짚어 더 물을 지점이 없음 → "충분히 답했어요"(케이스 C, GOOD)
public enum HighlightReason {
    PROBE_WORTHY,
    OFF_INTENT,
    SHALLOW,
    SUFFICIENT
}
