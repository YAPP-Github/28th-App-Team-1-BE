package com.yapp.d14.auth.adapter.out.security;

import com.yapp.d14.auth.application.port.out.JwtClaims;
import com.yapp.d14.auth.application.port.out.JwtProvider;
import com.yapp.d14.auth.exception.AuthErrorCode;
import com.yapp.d14.auth.exception.AuthException;
import com.yapp.d14.common.properties.JwtProperties;
import com.yapp.d14.common.security.TokenParseException;
import com.yapp.d14.common.security.TokenParser;
import com.yapp.d14.user.domain.Provider;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.UUID;

@Component
@RequiredArgsConstructor
class JwtProviderAdapter implements JwtProvider, TokenParser {

    private final JwtProperties jwtProperties;

    private static final String TOKEN_TYPE_CLAIM = "type";
    private static final String TOKEN_TYPE_ACCESS = "access";
    private static final String TOKEN_TYPE_REFRESH = "refresh";

    @Override
    public String issueAccessToken(UUID userId, Provider provider) {
        Date now = new Date();
        return Jwts.builder()
                .subject(userId.toString())
                .claim("provider", provider.name())
                .claim(TOKEN_TYPE_CLAIM, TOKEN_TYPE_ACCESS)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + jwtProperties.getAccessTokenExpiryMs()))
                .signWith(secretKey())
                .compact();
    }

    @Override
    public String issueRefreshToken(UUID userId, Provider provider) {
        Date now = new Date();
        return Jwts.builder()
                .subject(userId.toString())
                .claim("provider", provider.name())
                .claim(TOKEN_TYPE_CLAIM, TOKEN_TYPE_REFRESH)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + jwtProperties.getRefreshTokenExpiryMs()))
                .signWith(secretKey())
                .compact();
    }

    @Override
    public JwtClaims parseAccessToken(String token) {
        return parseClaims(token, TOKEN_TYPE_ACCESS);
    }

    @Override
    public JwtClaims parseRefreshToken(String token) {
        return parseClaims(token, TOKEN_TYPE_REFRESH);
    }

    private JwtClaims parseClaims(String token, String expectedType) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(secretKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            String tokenType = claims.get(TOKEN_TYPE_CLAIM, String.class);
            if (!expectedType.equals(tokenType)) {
                throw new AuthException(AuthErrorCode.INVALID_TOKEN);
            }

            UUID userId = UUID.fromString(claims.getSubject());
            Provider provider = Provider.valueOf(claims.get("provider", String.class));

            return new JwtClaims(userId, provider);
        } catch (AuthException e) {
            throw e;
        } catch (io.jsonwebtoken.ExpiredJwtException e) {
            throw new AuthException(AuthErrorCode.TOKEN_EXPIRED);
        } catch (Exception e) {
            throw new AuthException(AuthErrorCode.INVALID_TOKEN);
        }
    }

    @Override
    public UUID parse(String token) {
        try {
            return parseAccessToken(token).userId();
        } catch (AuthException e) {
            throw new TokenParseException(
                    e.getErrorCode().getHttpStatus().value(),
                    e.getErrorCode().getCode(),
                    e.getErrorCode().getMessage()
            );
        }
    }

    private SecretKey secretKey() {
        return Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtProperties.getSecretKey()));
    }
}
