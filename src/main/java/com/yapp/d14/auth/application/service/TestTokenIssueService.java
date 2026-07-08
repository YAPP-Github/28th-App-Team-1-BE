package com.yapp.d14.auth.application.service;

import com.yapp.d14.auth.application.port.in.TestTokenIssueUseCase;
import com.yapp.d14.auth.application.port.in.result.TestTokenIssueResult;
import com.yapp.d14.auth.application.port.out.JwtProvider;
import com.yapp.d14.user.domain.Provider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
class TestTokenIssueService implements TestTokenIssueUseCase {

    private static final UUID FIXED_TEST_USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final Provider FIXED_TEST_PROVIDER = Provider.KAKAO;

    private final JwtProvider jwtProvider;

    @Override
    public TestTokenIssueResult issue() {
        String accessToken = jwtProvider.issueAccessToken(FIXED_TEST_USER_ID, FIXED_TEST_PROVIDER);
        return new TestTokenIssueResult(accessToken, FIXED_TEST_USER_ID, FIXED_TEST_PROVIDER);
    }
}
