package com.yapp.d14.interview.application.port.out;

import com.yapp.d14.interview.domain.JobType;
import com.yapp.d14.interview.domain.TestType;

import java.util.List;

// 선택된 캐물지점 하나(probeText+echoQuote)를 실제 질문 문장으로 변환하는 generate_question_text 포트
public interface QuestionTextGenerator {

    // jobType·yearsOfExperience는 질문의 톤·후속 방향만 조정하고, probeText/echoQuote의 핵심 내용은 바꾸지 않는다.
    String generate(String probeText, String echoQuote, JobType jobType, Integer yearsOfExperience);

    // 해당 axis에 캐물 지점 후보가 없을 때 직무·연차에 맞춰 직접 여는 질문(seed)을 생성한다.
    // jdKeywords/relatedPortfolioChunks가 있으면(JD∩포폴이 실제로 뒷받침하는 소재) 조건부로 반영하고,
    // 없으면(빈 리스트) 직무·연차 기반 일반 질문으로 생성한다.
    String generateOpener(
            TestType axis, JobType jobType, Integer yearsOfExperience,
            List<String> jdKeywords, List<String> relatedPortfolioChunks
    );
}
