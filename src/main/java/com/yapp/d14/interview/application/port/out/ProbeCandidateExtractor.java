package com.yapp.d14.interview.application.port.out;

import java.util.List;

public interface ProbeCandidateExtractor {

    List<ProbeCandidateDraft> extract(List<String> portfolioChunks, List<String> jdKeywords);
}
