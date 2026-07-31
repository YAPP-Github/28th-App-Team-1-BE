package com.yapp.d14.appversion.domain;

import com.yapp.d14.appversion.exception.AppVersionException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AppVersionTest {

    @ParameterizedTest
    @CsvSource({
            "1.2.0, 1.2.1, true",   // 패치 낮음
            "1.2.0, 1.3.0, true",   // 마이너 낮음
            "1.9.0, 2.0.0, true",   // 메이저 경계
            "1.2.0, 1.2.0, false",  // 동일
            "1.3.0, 1.2.9, false",  // 높음
            "2.0.0, 1.9.9, false"   // 메이저 높음
    })
    void 자리별_정수로_비교한다(String left, String right, boolean expectedLower) {
        assertThat(AppVersion.parse(left).isLowerThan(AppVersion.parse(right)))
                .isEqualTo(expectedLower);
    }

    @Test
    void 자리수가_다르면_부족한_자리를_0으로_채워_비교한다() {
        // 1.2 == 1.2.0
        assertThat(AppVersion.parse("1.2").compareTo(AppVersion.parse("1.2.0"))).isZero();
        // 1.2 < 1.2.1
        assertThat(AppVersion.parse("1.2").isLowerThan(AppVersion.parse("1.2.1"))).isTrue();
        // 1.2.0.3 > 1.2.0
        assertThat(AppVersion.parse("1.2.0.3").isLowerThan(AppVersion.parse("1.2.0"))).isFalse();
    }

    @Test
    void 문자열_길이가_아니라_정수값으로_비교한다() {
        // "10" > "9" (문자열 단순 비교라면 "10" < "9")
        assertThat(AppVersion.parse("1.9.0").isLowerThan(AppVersion.parse("1.10.0"))).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "1.a.0", "1..2", "v1.2.0", "1.2.0-beta", "1,2,0"})
    void 형식이_잘못되면_예외를_던진다(String raw) {
        assertThatThrownBy(() -> AppVersion.parse(raw))
                .isInstanceOf(AppVersionException.class);
    }

    @Test
    void null이면_예외를_던진다() {
        assertThatThrownBy(() -> AppVersion.parse(null))
                .isInstanceOf(AppVersionException.class);
    }

    @Test
    void 원본_문자열을_보존한다() {
        assertThat(AppVersion.parse(" 1.2.0 ").value()).isEqualTo("1.2.0");
    }
}
