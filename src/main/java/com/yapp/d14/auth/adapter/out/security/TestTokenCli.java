package com.yapp.d14.auth.adapter.out.security;

import com.yapp.d14.common.properties.JwtProperties;
import com.yapp.d14.user.domain.Provider;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class TestTokenCli {

    private static final UUID DEFAULT_USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final Provider DEFAULT_PROVIDER = Provider.KAKAO;

    public static void main(String[] args) {
        Map<String, String> options = parseArgs(args);

        UUID userId = options.containsKey("userId")
                ? UUID.fromString(options.get("userId"))
                : DEFAULT_USER_ID;
        Provider provider = options.containsKey("provider")
                ? Provider.valueOf(options.get("provider").toUpperCase())
                : DEFAULT_PROVIDER;

        JwtProperties jwtProperties = new JwtProperties();
        jwtProperties.setSecretKey(requireEnv("JWT_SECRET_KEY"));
        if (options.containsKey("expiryMs")) {
            jwtProperties.setAccessTokenExpiryMs(Long.parseLong(options.get("expiryMs")));
        }

        JwtProviderAdapter jwtProvider = new JwtProviderAdapter(jwtProperties);
        String accessToken = jwtProvider.issueAccessToken(userId, provider);

        System.out.println("==============================================");
        System.out.println("userId   : " + userId);
        System.out.println("provider : " + provider);
        System.out.println("expiryMs : " + jwtProperties.getAccessTokenExpiryMs());
        System.out.println("token    : " + accessToken);
        System.out.println("==============================================");
    }

    private static Map<String, String> parseArgs(String[] args) {
        Map<String, String> options = new HashMap<>();
        for (String arg : args) {
            String[] parts = arg.split("=", 2);
            if (parts.length == 2 && parts[0].startsWith("--")) {
                options.put(parts[0].substring(2), parts[1]);
            }
        }
        return options;
    }

    private static String requireEnv(String key) {
        String value = System.getenv(key);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(key + " 환경변수가 설정되어 있지 않습니다.");
        }
        return value;
    }
}
