package com.yapp.d14.auth.adapter.out.integration.apple;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

@Getter
class AppleTokenResponse {

    @JsonProperty("id_token")
    private String idToken;

    @JsonProperty("access_token")
    private String accessToken;

    @JsonProperty("token_type")
    private String tokenType;

    @JsonProperty("expires_in")
    private Long expiresIn;
}
