package com.yapp.d14.interview.adapter.out.integration.llm;

import com.yapp.d14.interview.application.port.out.ProbeCandidateDraft;
import com.yapp.d14.interview.application.port.out.ProbeCandidateExtractor;
import org.springframework.stereotype.Component;

import java.util.List;

// TODO: 실제 LLM 연동 시 이 클래스를 교체. Sonnet급 모델(설계 문서 §3-2 "LLM 호출 2",
// §2-1 preload_probe_pool)에 포트폴리오 청크 + jdKeywords(참고용, JD 원문은 전달 금지)를 보내
// 캐물지점 후보를 추출 — axes.yaml을 시스템 프롬프트에 고정 삽입해 axis 태깅 기준으로 삼는다.
@Component
class StubProbeCandidateExtractorAdapter implements ProbeCandidateExtractor {

    @Override
    public List<ProbeCandidateDraft> extract(List<String> portfolioChunks, List<String> jdKeywords) {
        return List.of();
    }
}
