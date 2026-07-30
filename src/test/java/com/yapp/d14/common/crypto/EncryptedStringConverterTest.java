package com.yapp.d14.common.crypto;

import com.yapp.d14.common.properties.EncryptionProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class EncryptedStringConverterTest {

    private EncryptedStringConverter converter;

    @BeforeEach
    void setUp() {
        EncryptionProperties properties = new EncryptionProperties();
        properties.setTokenKey("XcfpLbY6AL5973FRt2FntuVuLaNAsSZPrXR6Z23fwGE=");
        converter = new EncryptedStringConverter(properties);
    }

    @Test
    void 암호화한_값을_다시_복호화하면_원래_값이_나온다() {
        String plainText = "apple-refresh-token-value";

        String encrypted = converter.convertToDatabaseColumn(plainText);

        assertThat(encrypted).startsWith("enc:v1:").isNotEqualTo(plainText);
        assertThat(converter.convertToEntityAttribute(encrypted)).isEqualTo(plainText);
    }

    @Test
    void 같은_평문도_매번_다른_암호문을_생성한다() {
        String plainText = "apple-refresh-token-value";

        String first = converter.convertToDatabaseColumn(plainText);
        String second = converter.convertToDatabaseColumn(plainText);

        assertThat(first).isNotEqualTo(second);
    }

    @Test
    void 접두어가_없는_기존_평문_값은_그대로_반환한다() {
        String legacyPlainText = "legacy-unencrypted-token";

        assertThat(converter.convertToEntityAttribute(legacyPlainText)).isEqualTo(legacyPlainText);
    }

    @Test
    void null은_그대로_null을_반환한다() {
        assertThat(converter.convertToDatabaseColumn(null)).isNull();
        assertThat(converter.convertToEntityAttribute(null)).isNull();
    }
}
