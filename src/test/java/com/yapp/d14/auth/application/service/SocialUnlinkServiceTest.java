package com.yapp.d14.auth.application.service;

import com.yapp.d14.auth.application.command.SocialUnlinkCommand;
import com.yapp.d14.auth.application.port.out.AppleSocialClient;
import com.yapp.d14.auth.application.port.out.KakaoSocialClient;
import com.yapp.d14.auth.exception.AuthErrorCode;
import com.yapp.d14.auth.exception.AuthException;
import com.yapp.d14.user.domain.Provider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SocialUnlinkServiceTest {

    @Mock
    private KakaoSocialClient kakaoSocialClient;

    @Mock
    private AppleSocialClient appleSocialClient;

    @InjectMocks
    private SocialUnlinkService service;

    @Test
    void 카카오는_providerId로_연결을_끊는다() {
        service.unlink(new SocialUnlinkCommand(Provider.KAKAO, "pid", null));

        verify(kakaoSocialClient).unlink("pid");
        verify(appleSocialClient, never()).revoke(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void 애플은_refresh_token으로_토큰을_폐기한다() {
        service.unlink(new SocialUnlinkCommand(Provider.APPLE, "pid", "refresh-token"));

        verify(appleSocialClient).revoke("refresh-token");
        verify(kakaoSocialClient, never()).unlink(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void 애플인데_저장된_refresh_token이_없으면_재로그인_유도_예외를_던진다() {
        assertThatThrownBy(() -> service.unlink(new SocialUnlinkCommand(Provider.APPLE, "pid", null)))
                .isInstanceOf(AuthException.class)
                .extracting(e -> ((AuthException) e).getErrorCode())
                .isEqualTo(AuthErrorCode.SOCIAL_RECONNECT_REQUIRED);

        verify(appleSocialClient, never()).revoke(org.mockito.ArgumentMatchers.any());
    }
}
