package com.yapp.d14.auth.application.service;

import com.yapp.d14.auth.application.command.TokenReissueCommand;
import com.yapp.d14.auth.application.port.in.AuthToken;
import com.yapp.d14.auth.application.port.in.TokenReissueUseCase;
import com.yapp.d14.auth.application.port.out.JwtClaims;
import com.yapp.d14.auth.application.port.out.JwtProvider;
import com.yapp.d14.auth.application.port.out.TokenRepository;
import com.yapp.d14.auth.exception.AuthErrorCode;
import com.yapp.d14.auth.exception.AuthException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
class TokenReissueService implements TokenReissueUseCase {

    private final JwtProvider jwtProvider;
    private final TokenRepository tokenRepository;

    @Override
    public AuthToken reissue(TokenReissueCommand command) {
        JwtClaims claims;
        try {
            claims = jwtProvider.parseRefreshToken(command.refreshToken());
        } catch (AuthException e) {
            log.warn("[TokenReissue] 유효하지 않은 리프레시 토큰");
            throw new AuthException(AuthErrorCode.LOGIN_EXPIRED);
        }

        String storedToken = tokenRepository.find(claims.userId())
                .orElseThrow(() -> {
                    log.warn("[TokenReissue] 저장된 토큰 없음 - 만료되었거나 로그아웃된 사용자: {}", claims.userId());
                    return new AuthException(AuthErrorCode.LOGIN_EXPIRED);
                });

        if (!storedToken.equals(command.refreshToken())) {
            log.error("[TokenReissue] 토큰 불일치 - 탈취 가능성: userId={}", claims.userId());
            throw new AuthException(AuthErrorCode.LOGIN_EXPIRED);
        }

        tokenRepository.delete(claims.userId());

        String newAccessToken = jwtProvider.issueAccessToken(claims.userId(), claims.provider());
        String newRefreshToken = jwtProvider.issueRefreshToken(claims.userId(), claims.provider());
        tokenRepository.save(claims.userId(), newRefreshToken);

        return new AuthToken(newAccessToken, newRefreshToken);
    }
}
