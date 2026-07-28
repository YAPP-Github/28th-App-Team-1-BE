package com.yapp.d14.interview.domain;

// 대본 문장 세그먼트의 발화 주체. 문장의 startIndex/endIndex가 어느 대본 문자열을 가리키는지도 구분한다
// (QUESTION=질문 대본, ANSWER=답변 대본).
public enum ScriptRole {
    QUESTION,
    ANSWER
}
