package com.yapp.d14.auth.adapter.out.security;

import com.yapp.d14.auth.application.port.out.JwtProvider;
import com.yapp.d14.auth.exception.AuthException;
import com.yapp.d14.common.security.TokenParseException;
import com.yapp.d14.common.security.TokenParser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
class JwtTokenParserAdapter implements TokenParser {

    private final JwtProvider jwtProvider;

    @Override
    public UUID parse(String token) {
        try {
            return jwtProvider.parseAccessToken(token).userId();
        } catch (AuthException e) {
            throw new TokenParseException(
                    e.getErrorCode().getHttpStatus().value(),
                    e.getErrorCode().getCode(),
                    e.getErrorCode().getMessage()
            );
        }
    }
}
