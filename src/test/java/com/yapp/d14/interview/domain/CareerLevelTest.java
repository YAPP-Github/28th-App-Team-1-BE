package com.yapp.d14.interview.domain;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

class CareerLevelTest {

    @ParameterizedTest
    @CsvSource({
            "0, JUNIOR",
            "2, JUNIOR",
            "3, MIDDLE",
            "7, MIDDLE",
            "8, SENIOR",
            "20, SENIOR"
    })
    void 연차_구간에_따라_레벨을_결정론적으로_판별한다(int careerYears, CareerLevel expected) {
        assertThat(CareerLevel.fromYears(careerYears)).isEqualTo(expected);
    }

    @Test
    void 모든_레벨의_델타_합은_0이다() {
        for (CareerLevel level : CareerLevel.values()) {
            int sum = Arrays.stream(TestType.values())
                    .mapToInt(level::getDelta)
                    .sum();

            assertThat(sum).as("%s 델타 합", level).isZero();
        }
    }

    @Test
    void MIDDLE은_모든_축에서_델타가_0이다() {
        for (TestType testType : TestType.values()) {
            assertThat(CareerLevel.MIDDLE.getDelta(testType)).isZero();
        }
    }
}
