package com.mongodb.samplemflix.integration;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.Conditional;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.lang.NonNull;
import org.springframework.test.context.DynamicPropertyRegistrar;
import org.testcontainers.mongodb.MongoDBAtlasLocalContainer;

@TestConfiguration
public class MongoDBTestContainersConfig {

    // Note: @ServiceConnection cannot be used here because MongoDBAtlasLocalContainer
    // extends GenericContainer, not MongoDBContainer. Spring Boot's auto-detection
    // only recognizes the latter (fixed in Spring Boot v4). We use initMethod instead so Spring manages the
    // full lifecycle (start on init, close/stop on context shutdown via AutoCloseable).

    @Bean(initMethod = "start")
    @Conditional(MongoUriMissingCondition.class)
    public MongoDBAtlasLocalContainer mongoDbContainer() {
        return new MongoDBAtlasLocalContainer("mongodb/mongodb-atlas-local:8");
    }

    @Bean
    @Conditional(MongoUriMissingCondition.class)
    public DynamicPropertyRegistrar mongoDbProperties(MongoDBAtlasLocalContainer mongoDBContainer) {
        return (registry) -> {
            registry.add("spring.data.mongodb.uri", mongoDBContainer::getConnectionString);
        };
    }

    static class MongoUriMissingCondition implements Condition {
        @Override
        public boolean matches(ConditionContext context, @NonNull AnnotatedTypeMetadata metadata) {
            String uri = context.getEnvironment().getProperty("spring.data.mongodb.uri");
            return uri == null || uri.isBlank();
        }
    }
}
