package com.yapp.d14.interview.application.port.out;

import com.yapp.d14.interview.domain.ActionKeyword;
import com.yapp.d14.interview.domain.HighlightSpan;
import com.yapp.d14.interview.domain.RewriteSuggestion;
import com.yapp.d14.interview.domain.TestType;

import java.util.List;

public record ReportCardDraft(
        TestType testType,
        String questionIntentTranslation,
        List<HighlightSpan> highlightSpans,
        List<ActionKeyword> actionKeywords,
        RewriteSuggestion rewriteSuggestion
) {
}
