package com.yapp.d14.auth.adapter.out.integration.apple;

import lombok.Getter;

import java.util.List;

@Getter
class ApplePublicKeyResponse {

    private List<Key> keys;

    @Getter
    static class Key {
        private String kty;
        private String kid;
        private String use;
        private String alg;
        private String n;
        private String e;
    }
}
