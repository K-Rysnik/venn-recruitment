package ca.venn.hometask;

import java.math.BigDecimal;
import java.util.Currency;

import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import ca.venn.hometask.api.Amount;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.ValueDeserializer;
import tools.jackson.databind.module.SimpleModule;

@Configuration
public class ObjectMapperConfig {
    @Bean
    public JsonMapperBuilderCustomizer jsonMapperBuilderCustomizer() {
        return builder -> {
            builder.propertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
            builder.enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
            SimpleModule amountModule = new SimpleModule();
            // Register your custom deserializer for a specific type
            amountModule.addDeserializer(Amount.class, new ValueDeserializer<Amount>() {
                @Override
                public Amount deserialize(JsonParser p, DeserializationContext ctxt) {
                    // Implement your custom parsing logic here
                    String value = p.getValueAsString();
                    if(value.startsWith("$")) {
                        return new Amount(Currency.getInstance("CAD"), new BigDecimal(value.substring(1)));
                    }
                    throw new UnsupportedOperationException("Unsupported amount format: " + value);
                }
            });
            builder.addModule(amountModule);
        };
    }
}
