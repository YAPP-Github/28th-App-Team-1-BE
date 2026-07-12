package com.yapp.d14.interview.adapter.out.integration.llm;

import com.yapp.d14.interview.application.port.out.JdKeywordExtractor;
import org.springframework.stereotype.Component;

import java.util.List;

// TODO: 실제 LLM 연동 시 이 클래스를 교체. jdText를 LLM(설계 문서 §3-2 "LLM 호출 1")에 보내
// 키워드 리스트만 뽑아 반환 — 캐물지점은 만들지 않음. JdKeywordExtractor 인터페이스는 그대로 유지.
@Component
class StubJdKeywordExtractorAdapter implements JdKeywordExtractor {

    @Override
    public List<String> extractKeywords(String jdText) {
        return List.of();
    }
}
