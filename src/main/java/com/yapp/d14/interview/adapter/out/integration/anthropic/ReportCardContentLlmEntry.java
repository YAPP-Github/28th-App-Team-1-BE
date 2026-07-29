package com.yapp.d14.interview.adapter.out.integration.anthropic;

import java.util.List;

// axis·depthLevel은 서버가 이미 아는 값이라 LLM에게 되받지 않는다. questionId만 어느 턴의 카드인지
// 식별하는 키로 echo 받고, testType·depthLevel은 questionId로 서버 컨텍스트에서 되찾는다.
record ReportCardContentLlmEntry(
        Long questionId,
        String questionIntentTitle,
        String questionIntentTranslation,
        List<HighlightSpanLlmEntry> highlightSpans
) {

    // quote: answerText에서 그대로 옮겨 적은 하이라이트 원문(공백·문장부호까지 정확히 일치해야 함).
    // 인덱스를 직접 요구하지 않는다 — LLM은 긴 텍스트의 문자 위치를 정확히 세지 못하므로, 서버가 quote를
    // answerText에서 검색(indexOf)해 실제 startIndex/endIndex를 되찾는다(#78, ReportCardHighlightMappingTest).
    record HighlightSpanLlmEntry(
            String quote, String tone, String reason, String title, String analysis,
            List<String> followUpQuestions
    ) {
    }
}
