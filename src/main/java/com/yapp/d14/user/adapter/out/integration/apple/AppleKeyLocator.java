package com.yapp.d14.user.adapter.out.integration.apple;

import io.jsonwebtoken.JwsHeader;
import io.jsonwebtoken.LocatorAdapter;

import java.math.BigInteger;
import java.security.Key;
import java.security.KeyFactory;
import java.security.spec.RSAPublicKeySpec;
import java.util.Base64;
import java.util.List;

class AppleKeyLocator extends LocatorAdapter<Key> {

    private final List<ApplePublicKeyResponse.Key> publicKeys;

    AppleKeyLocator(List<ApplePublicKeyResponse.Key> publicKeys) {
        this.publicKeys = publicKeys;
    }

    @Override
    protected Key locate(JwsHeader header) {
        ApplePublicKeyResponse.Key matchedKey = publicKeys.stream()
                .filter(key ->
                        key.getKid().equals(header.getKeyId()) &&
                        key.getAlg().equals(header.getAlgorithm())
                )
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "일치하는 Apple public key 없음. kid=" + header.getKeyId()
                ));

        BigInteger n = new BigInteger(1, Base64.getUrlDecoder().decode(matchedKey.getN()));
        BigInteger e = new BigInteger(1, Base64.getUrlDecoder().decode(matchedKey.getE()));

        try {
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            return keyFactory.generatePublic(new RSAPublicKeySpec(n, e));
        } catch (Exception ex) {
            throw new IllegalStateException("Apple public key 생성 실패", ex);
        }
    }
}
