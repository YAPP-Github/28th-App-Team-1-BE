package com.yapp.d14.appversion.application.service;

import com.yapp.d14.appversion.application.command.AppVersionCheckCommand;
import com.yapp.d14.appversion.application.port.in.result.AppVersionCheckResult;
import com.yapp.d14.appversion.application.port.out.AppVersionPolicyRepository;
import com.yapp.d14.appversion.domain.AppVersion;
import com.yapp.d14.appversion.domain.AppVersionPolicy;
import com.yapp.d14.appversion.domain.Platform;
import com.yapp.d14.appversion.domain.UpdateType;
import com.yapp.d14.appversion.exception.AppVersionErrorCode;
import com.yapp.d14.appversion.exception.AppVersionException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class AppVersionCheckServiceTest {

    @Mock
    private AppVersionPolicyRepository appVersionPolicyRepository;

    @InjectMocks
    private AppVersionCheckService service;

    private AppVersionPolicy iosPolicy() {
        return AppVersionPolicy.of(
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
    }

    @Test
    void 정책을_조회해_업데이트_유형과_버전_정보를_반환한다() {
        given(appVersionPolicyRepository.findByPlatform(Platform.IOS))
                .willReturn(Optional.of(iosPolicy()));

        AppVersionCheckResult result = service.check(AppVersionCheckCommand.of("IOS", "1.2.0"));

        assertThat(result.updateType()).isEqualTo(UpdateType.FORCE);
        assertThat(result.latestVersion()).isEqualTo("1.4.0");
        assertThat(result.minSupportedVersion()).isEqualTo("1.3.0");
        assertThat(result.storeUrl()).isEqualTo("https://apps.apple.com/app/idXXXXXXXXX");
        assertThat(result.title()).isEqualTo("업데이트가 필요해요");
        assertThat(result.body()).isEqualTo("지금 버전에서는 앱을 이용할 수 없어요. 최신 버전으로 업데이트해 주세요.");
    }

    @Test
    void NONE_상태면_문구가_없다() {
        given(appVersionPolicyRepository.findByPlatform(Platform.IOS))
                .willReturn(Optional.of(iosPolicy()));

        AppVersionCheckResult result = service.check(AppVersionCheckCommand.of("IOS", "1.4.0"));

        assertThat(result.updateType()).isEqualTo(UpdateType.NONE);
        assertThat(result.title()).isNull();
        assertThat(result.body()).isNull();
    }

    @Test
    void 정책이_없으면_POLICY_NOT_FOUND_예외를_던진다() {
        given(appVersionPolicyRepository.findByPlatform(Platform.ANDROID))
                .willReturn(Optional.empty());

        assertThatThrownBy(() -> service.check(AppVersionCheckCommand.of("ANDROID", "1.2.0")))
                .isInstanceOf(AppVersionException.class)
                .extracting("errorCode")
                .isEqualTo(AppVersionErrorCode.POLICY_NOT_FOUND);
    }

    @Test
    void 버전_형식이_잘못되면_INVALID_VERSION_FORMAT_예외를_던진다() {
        given(appVersionPolicyRepository.findByPlatform(Platform.IOS))
                .willReturn(Optional.of(iosPolicy()));

        assertThatThrownBy(() -> service.check(AppVersionCheckCommand.of("IOS", "1.a.0")))
                .isInstanceOf(AppVersionException.class)
                .extracting("errorCode")
                .isEqualTo(AppVersionErrorCode.INVALID_VERSION_FORMAT);
    }

    @Test
    void 지원하지_않는_플랫폼이면_INVALID_PLATFORM_예외를_던진다() {
        assertThatThrownBy(() -> AppVersionCheckCommand.of("WINDOWS", "1.2.0"))
                .isInstanceOf(AppVersionException.class)
                .extracting("errorCode")
                .isEqualTo(AppVersionErrorCode.INVALID_PLATFORM);
    }
}
