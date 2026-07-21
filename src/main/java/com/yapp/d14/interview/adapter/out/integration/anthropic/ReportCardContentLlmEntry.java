package com.yapp.d14.interview.adapter.out.integration.anthropic;

import java.util.List;

// axis·depthLevel은 서버가 이미 아는 값이라 LLM에게 되받지 않는다. questionId만 어느 턴의 카드인지
// 식별하는 키로 echo 받고, testType·depthLevel은 questionId로 서버 컨텍스트에서 되찾는다.
record ReportCardContentLlmEntry(
        Long questionId,
        String questionIntentTranslation,
        List<HighlightSpanLlmEntry> highlightSpans
) {

    record HighlightSpanLlmEntry(int startIndex, int endIndex, String tone, String analysis, List<String> followUpQuestions) {
    }
}
