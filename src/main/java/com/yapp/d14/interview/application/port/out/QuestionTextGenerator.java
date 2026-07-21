package com.yapp.d14.interview.application.port.out;

import com.yapp.d14.interview.domain.JobType;
import com.yapp.d14.interview.domain.TestType;

// 선택된 캐물지점 하나(probeText+echoQuote)를 실제 질문 문장으로 변환하는 generate_question_text 포트
public interface QuestionTextGenerator {

    String generate(String probeText, String echoQuote);

    // 해당 axis에 캐물 지점 후보가 없을 때 직무·연차에 맞춰 직접 여는 질문(seed)을 생성한다
    String generateOpener(TestType axis, JobType jobType, Integer yearsOfExperience);
}
