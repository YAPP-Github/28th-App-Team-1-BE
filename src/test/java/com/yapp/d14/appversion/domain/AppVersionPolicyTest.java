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
            "업데이트가 필요해요",
            "지금 버전에서는 앱을 이용할 수 없어요. 최신 버전으로 업데이트해 주세요.",
            "새 버전이 나왔어요",
            "면접 연습 화면이 더 빨라졌어요. 지금 업데이트할까요?",
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

    @Test
    void FORCE_상태면_강제_업데이트_문구를_반환한다() {
        assertThat(policy.resolveTitle(UpdateType.FORCE)).isEqualTo("업데이트가 필요해요");
        assertThat(policy.resolveBody(UpdateType.FORCE))
                .isEqualTo("지금 버전에서는 앱을 이용할 수 없어요. 최신 버전으로 업데이트해 주세요.");
    }

    @Test
    void OPTIONAL_상태면_권장_업데이트_문구를_반환한다() {
        assertThat(policy.resolveTitle(UpdateType.OPTIONAL)).isEqualTo("새 버전이 나왔어요");
        assertThat(policy.resolveBody(UpdateType.OPTIONAL)).isEqualTo("면접 연습 화면이 더 빨라졌어요. 지금 업데이트할까요?");
    }

    @Test
    void NONE_상태면_문구가_없다() {
        assertThat(policy.resolveTitle(UpdateType.NONE)).isNull();
        assertThat(policy.resolveBody(UpdateType.NONE)).isNull();
    }
}
