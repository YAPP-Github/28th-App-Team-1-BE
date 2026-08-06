package com.yapp.d14.interview.adapter.out.integration.anthropic;

// 프롬프트에 싣는 외부 유래 문자열(이전 세션 질문 등)을 인용 데이터로 경계 짓는다.
// 이전 질문은 포트폴리오·답변에서 파생돼 저장된 값이라 지시문이 섞여 들어올 수 있는데,
// 줄바꿈과 따옴표를 그대로 두면 인용 부호 밖으로 빠져나가 프롬프트 본문처럼 읽힌다.
final class PromptQuoting {

    private PromptQuoting() {
    }

    static String quoted(String value) {
        if (value == null) {
            return "\"\"";
        }
        return "\"" + value.replaceAll("\\s+", " ").replace('"', '\'').trim() + "\"";
    }
}
