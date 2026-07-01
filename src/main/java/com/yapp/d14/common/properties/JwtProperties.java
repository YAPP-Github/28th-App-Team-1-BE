package com.yapp.d14.common.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {

    private String secretKey;
    private long accessTokenExpiryMs = 10_800_000L; // 3시간
    private long refreshTokenExpiryMs = 604_800_000L; // 7일
}
