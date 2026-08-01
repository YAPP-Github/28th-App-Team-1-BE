package com.yapp.d14.user.adapter.in.web.request;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class UserNameRegisterHttpRequestTest {

    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void setUp() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void tearDown() {
        factory.close();
    }

    @DisplayName("한글·영문 5자 이하는 통과한다")
    @ParameterizedTest
    @ValueSource(strings = {"홍", "홍길동", "가나다라마", "abc", "abcde", "홍Kim"})
    void 유효한_이름(String name) {
        assertThat(validator.validate(new UserNameRegisterHttpRequest(name))).isEmpty();
    }

    @DisplayName("빈 값·6자 이상·숫자·특수문자·자모는 거부한다")
    @ParameterizedTest
    @ValueSource(strings = {"", " ", "가나다라마바", "abcdef", "홍길동1", "홍길동!", "hong gil", "ㄱㄴㄷ", "홍길ㄷ"})
    void 무효한_이름(String name) {
        assertThat(validator.validate(new UserNameRegisterHttpRequest(name))).isNotEmpty();
    }
}
