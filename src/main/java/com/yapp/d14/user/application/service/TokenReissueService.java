package com.yapp.d14.user.application.service;

import com.yapp.d14.user.application.command.TokenReissueCommand;
import com.yapp.d14.user.application.port.in.AuthToken;
import com.yapp.d14.user.application.port.in.TokenReissueUseCase;
import com.yapp.d14.user.application.port.out.JwtClaims;
import com.yapp.d14.user.application.port.out.JwtProvider;
import com.yapp.d14.user.application.port.out.TokenRepository;
import com.yapp.d14.user.exception.UserErrorCode;
import com.yapp.d14.user.exception.UserException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
class TokenReissueService implements TokenReissueUseCase {

    private final JwtProvider jwtProvider;
    private final TokenRepository tokenRepository;

    @Override
    public AuthToken reissue(TokenReissueCommand command) {
        JwtClaims claims = jwtProvider.parseRefreshToken(command.refreshToken());

        String storedToken = tokenRepository.find(claims.userId())
                .orElseThrow(() -> new UserException(UserErrorCode.INVALID_TOKEN));

        if (!storedToken.equals(command.refreshToken())) {
            throw new UserException(UserErrorCode.INVALID_TOKEN);
        }

        tokenRepository.delete(claims.userId());

        String newAccessToken = jwtProvider.issueAccessToken(claims.userId(), claims.provider());
        String newRefreshToken = jwtProvider.issueRefreshToken(claims.userId(), claims.provider());
        tokenRepository.save(claims.userId(), newRefreshToken);

        return new AuthToken(newAccessToken, newRefreshToken);
    }
}
