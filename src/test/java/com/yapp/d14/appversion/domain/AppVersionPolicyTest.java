package com.yapp.d14.appversion.domain;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class AppVersionPolicyTest {

    // min=1.3.0, latest=1.4.0
    private final AppVersionPolicy policy = AppVersionPolicy.of(
            Platform.IOS,
            AppVersion.parse("1.3.0"),
            AppVersion.parse("1.4.0"),
            "https://apps.apple.com/app/idXXXXXXXXX",
            LocalDateTime.now()
    );

    @ParameterizedTest
    @CsvSource({
            "1.2.9, FORCE",     // min 미만
            "1.0.0, FORCE",     // min 한참 미만
            "1.3.0, OPTIONAL",  // min 경계(포함) → 권장
            "1.3.5, OPTIONAL",  // min 이상 latest 미만
            "1.3.9, OPTIONAL",  // latest 직전
            "1.4.0, NONE",      // latest 경계(포함) → 최신
            "1.5.0, NONE"       // latest 초과
    })
    void 버전에_따라_업데이트_유형을_판정한다(String current, UpdateType expected) {
        assertThat(policy.determineUpdateType(AppVersion.parse(current))).isEqualTo(expected);
    }

    @Test
    void 자리수가_다른_버전도_경계를_올바르게_판정한다() {
        // min=1.3.0 과 동일한 "1.3" → OPTIONAL
        assertThat(policy.determineUpdateType(AppVersion.parse("1.3"))).isEqualTo(UpdateType.OPTIONAL);
        // latest=1.4.0 과 동일한 "1.4" → NONE
        assertThat(policy.determineUpdateType(AppVersion.parse("1.4"))).isEqualTo(UpdateType.NONE);
    }
}
