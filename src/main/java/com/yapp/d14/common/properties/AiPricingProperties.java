package com.yapp.d14.common.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "ai.pricing")
public class AiPricingProperties {

    private Map<String, ModelPrice> models = new HashMap<>();

    public ModelPrice priceOf(String model) {
        if (model == null || model.isBlank()) {
            return null;
        }
        return models.get(model.toLowerCase(Locale.ROOT));
    }

    @Getter
    @Setter
    public static class ModelPrice {

        private long inputNanoUsdPerToken;
        private long outputNanoUsdPerToken;
        private long cacheWriteNanoUsdPerToken;
        private long cacheReadNanoUsdPerToken;
        private long nanoUsdPerCharacter;
        private long nanoUsdPerSecond;
    }
}
