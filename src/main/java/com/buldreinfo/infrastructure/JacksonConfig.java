package com.buldreinfo.infrastructure;

import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.buldreinfo.infrastructure.json.CoercionModule;

import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.cfg.DateTimeFeature;

@Configuration
public class JacksonConfig {

    /**
     * Customizes the auto-configured {@code JsonMapper} instead of replacing it, so Spring Boot keeps its own
     * Jackson setup (modules, handler instantiator, properties) while the climbing coercion rules still apply.
     */
    @Bean
    public JsonMapperBuilderCustomizer climbingJsonMapperCustomizer() {
        return builder -> {
            builder.addModule(new CoercionModule());
            builder.disable(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS);
            builder.disable(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES);
            builder.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        };
    }
}