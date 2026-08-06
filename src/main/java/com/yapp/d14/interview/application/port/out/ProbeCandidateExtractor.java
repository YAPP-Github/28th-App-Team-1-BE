package com.yapp.d14.interview.application.port.out;

import java.util.List;

public interface ProbeCandidateExtractor {

    // priorQuestions: 같은 사용자의 이전 면접에서 이미 나간 질문 — 같은 지점을 다시 뽑지 않도록 회피 대상으로 넘긴다.
    List<ProbeCandidateDraft> extract(
            String focusProject, List<String> portfolioChunks, List<String> jdKeywords, List<String> priorQuestions
    );
}
