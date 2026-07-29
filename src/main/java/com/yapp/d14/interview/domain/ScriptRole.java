package com.yapp.d14.interview.domain;

// 대본 문장 세그먼트의 발화 주체. 문장의 startIndex/endIndex가 어느 대본 문자열을 가리키는지도 구분한다
// (INTERVIEWER=면접관 발화 대본[질문·오프닝·마무리 멘트], INTERVIEWEE=면접자 답변 대본).
public enum ScriptRole {
    INTERVIEWER,
    INTERVIEWEE
}
