package com.yapp.d14.auth.adapter.out.integration.apple;

import com.yapp.d14.auth.application.port.out.AppleSocialClient;
import com.yapp.d14.auth.application.port.out.SocialUserInfo;
import com.yapp.d14.auth.exception.AuthErrorCode;
import com.yapp.d14.auth.exception.AuthException;
import com.yapp.d14.common.properties.AppleProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Date;

@Slf4j
@Component
@RequiredArgsConstructor
class AppleAuthAdapter implements AppleSocialClient {

    private static final String APPLE_BASE_URL = "https://appleid.apple.com";
    private static final String APPLE_PUBLIC_KEY_URL = APPLE_BASE_URL + "/auth/keys";
    private static final long CLIENT_SECRET_EXPIRY_MS = 30L * 24 * 60 * 60 * 1000;

    private final AppleProperties appleProperties;

    @Override
    public SocialUserInfo getUserInfo(String authorizationCode) {
        AppleTokenResponse tokenResponse = exchangeAuthorizationCode(authorizationCode);
        Claims claims = verifyIdToken(tokenResponse.getIdToken());

        String providerId = claims.getSubject();
        String email = claims.get("email", String.class);

        return new SocialUserInfo(providerId, email, null);
    }

    private AppleTokenResponse exchangeAuthorizationCode(String authorizationCode) {
        try {
            return RestClient.builder()
                    .baseUrl(APPLE_BASE_URL)
                    .defaultHeader(HttpHeaders.CONTENT_TYPE, "application/x-www-form-urlencoded;charset=utf-8")
                    .build()
                    .post()
                    .uri(uriBuilder -> uriBuilder.path("/auth/token")
                            .queryParam("grant_type", "authorization_code")
                            .queryParam("client_id", appleProperties.getClientId())
                            .queryParam("client_secret", makeClientSecret())
                            .queryParam("code", authorizationCode)
                            .build()
                    )
                    .retrieve()
                    .body(AppleTokenResponse.class);
        } catch (Exception e) {
            log.error("[APPLE LOGIN] authorization code 교환 실패", e);
            throw new AuthException(AuthErrorCode.SOCIAL_LOGIN_FAILED);
        }
    }

    private Claims verifyIdToken(String idToken) {
        try {
            ApplePublicKeyResponse publicKeys = RestClient.create()
                    .get()
                    .uri(APPLE_PUBLIC_KEY_URL)
                    .retrieve()
                    .body(ApplePublicKeyResponse.class);

            return Jwts.parser()
                    .keyLocator(new AppleKeyLocator(publicKeys.getKeys()))
                    .build()
                    .parseSignedClaims(idToken)
                    .getPayload();
        } catch (Exception e) {
            log.error("[APPLE LOGIN] id_token 검증 실패", e);
            throw new AuthException(AuthErrorCode.SOCIAL_LOGIN_FAILED);
        }
    }

    private String makeClientSecret() {
        return Jwts.builder()
                .subject(appleProperties.getClientId())
                .issuer(appleProperties.getTeamId())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + CLIENT_SECRET_EXPIRY_MS))
                .audience().add(APPLE_BASE_URL).and()
                .header().keyId(appleProperties.getKeyId()).and()
                .signWith(getPrivateKey(), Jwts.SIG.ES256)
                .compact();
    }

    private PrivateKey getPrivateKey() {
        try {
            String rawKey = appleProperties.getPrivateKey()
                    .replace("-----BEGIN PRIVATE KEY-----", "")
                    .replace("-----END PRIVATE KEY-----", "")
                    .replaceAll("\\s", "");
            byte[] keyBytes = Decoders.BASE64.decode(rawKey);
            return KeyFactory.getInstance("EC")
                    .generatePrivate(new PKCS8EncodedKeySpec(keyBytes));
        } catch (Exception e) {
            log.error("[APPLE LOGIN] private key 생성 실패", e);
            throw new AuthException(AuthErrorCode.SOCIAL_LOGIN_FAILED);
        }
    }
}
