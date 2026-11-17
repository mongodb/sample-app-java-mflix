package com.mongodb.samplemflix.config;

import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.bson.types.ObjectId;
import org.springframework.boot.jackson2.autoconfigure.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

/**
 * Configuration for customizing the ObjectMapper used for JSON serialization and deserialization.
 *
 * <p>This configuration disables the default timestamp serialization for dates and registers a
 * custom serializer for MongoDB's ObjectId to convert it to a string representation.
 *
 * <p>It also registers a JavaTimeModule to handle Java 8 date and time types.
 *
 */

@Configuration
public class ObjectMapperConfig {

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer objectMapperBuilderCustomizer() {
        return (Jackson2ObjectMapperBuilder builder) -> {
            builder.featuresToDisable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
            builder.modulesToInstall(new JavaTimeModule());
            SimpleModule module = new SimpleModule();
            module.addSerializer(ObjectId.class, new ObjectIdSerializer());
            builder.modulesToInstall(module);
        };
    }

    @Bean
    public DataBufferFactory dataBufferFactory() {
        return new DefaultDataBufferFactory();
    }
}
