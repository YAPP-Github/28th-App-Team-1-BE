package com.yapp.d14.interview.application.port.out;

import com.yapp.d14.interview.domain.ActionKeyword;
import com.yapp.d14.interview.domain.HighlightSpan;
import com.yapp.d14.interview.domain.RewriteSuggestion;
import com.yapp.d14.interview.domain.TestType;

import java.util.List;

// 카드 = 질문/답변 턴 하나. questionId로 어느 턴인지 식별하고, depthLevel은 같은 축(testType) 안에서의 순서다.
public record ReportCardDraft(
        Long questionId,
        int depthLevel,
        TestType testType,
        String questionIntentTranslation,
        List<HighlightSpan> highlightSpans,
        List<ActionKeyword> actionKeywords,
        RewriteSuggestion rewriteSuggestion
) {
}
