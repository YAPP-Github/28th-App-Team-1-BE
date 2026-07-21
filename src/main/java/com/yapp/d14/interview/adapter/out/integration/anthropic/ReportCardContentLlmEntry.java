package com.yapp.d14.interview.adapter.out.integration.anthropic;

import java.util.List;

record ReportCardContentLlmEntry(
        String axis,
        Long questionId,
        int depthLevel,
        String questionIntentTranslation,
        List<HighlightSpanLlmEntry> highlightSpans
) {

    record HighlightSpanLlmEntry(int startIndex, int endIndex, String tone) {
    }
}
