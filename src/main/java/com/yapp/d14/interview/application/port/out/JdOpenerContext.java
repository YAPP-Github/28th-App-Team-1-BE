package com.yapp.d14.interview.application.port.out;

import java.util.List;

// 조건부 opener 생성 시 참고할 JD 키워드와, 그 키워드로 검색한 관련 포트폴리오 청크
public record JdOpenerContext(List<String> jdKeywords, List<String> relatedPortfolioChunks) {
}
