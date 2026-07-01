package com.yapp.d14.user.adapter.out.security;

import com.yapp.d14.common.config.JwtProperties;
import com.yapp.d14.user.application.port.out.JwtClaims;
import com.yapp.d14.user.application.port.out.JwtProvider;
import com.yapp.d14.user.domain.Provider;
import com.yapp.d14.user.exception.UserErrorCode;
import com.yapp.d14.user.exception.UserException;
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
class JwtProviderAdapter implements JwtProvider {

    private final JwtProperties jwtProperties;

    @Override
    public String issueAccessToken(UUID userId, Provider provider) {
        SecretKey key = secretKey();
        Date now = new Date();
        Date expiry = new Date(now.getTime() + jwtProperties.getAccessTokenExpiryMs());

        return Jwts.builder()
                .subject(userId.toString())
                .claim("provider", provider.name())
                .issuedAt(now)
                .expiration(expiry)
                .signWith(key)
                .compact();
    }

    @Override
    public JwtClaims parseAccessToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(secretKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            UUID userId = UUID.fromString(claims.getSubject());
            Provider provider = Provider.valueOf(claims.get("provider", String.class));

            return new JwtClaims(userId, provider);
        } catch (Exception e) {
            throw new UserException(UserErrorCode.INVALID_TOKEN);
        }
    }

    private SecretKey secretKey() {
        return Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtProperties.getSecretKey()));
    }
}
