package com.yapp.d14.interview.application.port.out;

// 선택된 캐물지점 하나(probeText+echoQuote)를 실제 질문 문장으로 변환하는 generate_question_text 포트
public interface QuestionTextGenerator {

    String generate(String probeText, String echoQuote);
}
