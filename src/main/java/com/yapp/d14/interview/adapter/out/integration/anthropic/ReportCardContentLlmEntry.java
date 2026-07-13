package com.yapp.d14.interview.adapter.out.integration.anthropic;

import java.util.List;

record ReportCardContentLlmEntry(
        String axis,
        String questionIntentTranslation,
        List<HighlightSpanLlmEntry> highlightSpans,
        List<ActionKeywordLlmEntry> actionKeywords,
        RewriteSuggestionLlmEntry rewriteSuggestion
) {

    record HighlightSpanLlmEntry(Float startSec, Float endSec, String tone) {
    }

    record ActionKeywordLlmEntry(
            String keyword,
            String problemAnalysis,
            String improvementReason,
            String applicationMethod,
            int priority
    ) {
    }

    record RewriteSuggestionLlmEntry(String originalQuote, String rewrittenText) {
    }
}
