package com.mongodb.samplemflix.config;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoDatabase;
import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.config.AbstractMongoClientConfiguration;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;
import org.springframework.lang.NonNull;

/**
 * MongoDB configuration class for the Sample MFlix application using Spring Data MongoDB.
 *
 * <p>This class extends AbstractMongoClientConfiguration to customize MongoDB client settings
 * while leveraging Spring Data MongoDB's autoconfiguration for repositories and templates.
 *
 * <p>Key features:
* <ul>
*   <li>Connection pooling with configurable settings (max 100 connections, min 10)</li>
*   <li>Connection timeout configuration (10 seconds for connect and read)</li>
*   <li>Automatic POJO mapping (no manual codec configuration needed)</li>
*   <li>Repository scanning and auto-configuration</li>
*   <li>MongoTemplate bean creation for complex queries</li>
* </ul>
 * <p>Spring Data MongoDB automatically:
* <ul>
*   <li>Creates MongoClient and MongoTemplate beans</li>
*   <li>Handles POJO to BSON conversion</li>
*   <li>Manages connection lifecycle</li>
*   <li>Provides repository implementations</li>
* </ul>
 */
@Configuration
@EnableMongoRepositories(basePackages = "com.mongodb.samplemflix.repository")
public class MongoConfig extends AbstractMongoClientConfiguration {

    @Value("${spring.data.mongodb.uri}")
    private String mongoUri;

    @Value("${spring.data.mongodb.database}")
    private String databaseName;

    @Override
    protected String getDatabaseName() {
        return databaseName;
    }

    @Override
    protected void configureClientSettings(MongoClientSettings.Builder builder) {
        // Validate connection string is not empty
        if (mongoUri == null || mongoUri.trim().isEmpty()) {
            throw new IllegalArgumentException(
                "MONGODB_URI is not configured. Please check application.properties"
            );
        }

        // Parse and validate the connection string
        ConnectionString connectionString = new ConnectionString(mongoUri);

        // Apply connection string and custom settings
        builder.applyConnectionString(connectionString)
                // Set application name
                .applicationName("sample-app-java-mflix")
                // Configure connection pool for optimal performance
                .applyToConnectionPoolSettings(poolBuilder ->
                    poolBuilder.maxSize(100)                                    // Maximum connections in pool
                           .minSize(5)                                          // Minimum connections to maintain
                           .maxConnectionIdleTime(60000, TimeUnit.MILLISECONDS) // Release idle connections after 60s
                           .maxWaitTime(10000, TimeUnit.MILLISECONDS)           // Wait up to 10s for available connection
                           .maintenanceInitialDelay(0, TimeUnit.MILLISECONDS)   // Start maintenance immediately
                           .maintenanceFrequency(60000, TimeUnit.MILLISECONDS)  // Run maintenance every 60s
                )
                // Configure socket timeouts to prevent hanging connections
                .applyToSocketSettings(socketBuilder ->
                    socketBuilder.connectTimeout(10000, TimeUnit.MILLISECONDS)  // 10s to establish connection
                           .readTimeout(60000, TimeUnit.MILLISECONDS)           // 60s to wait for server response (increased for aggregations)
                )
                // Configure server selection timeout
                .applyToClusterSettings(clusterBuilder ->
                    clusterBuilder.serverSelectionTimeout(10000, TimeUnit.MILLISECONDS)  // 10s to select server
                )
                // Retry writes for better reliability
                .retryWrites(true)
                .retryReads(true);
    }

    /**
     * Provides a MongoDatabase bean for direct MongoDB driver access.
     *
     * <p>This bean is needed for components that require direct access to the MongoDB
     * driver API (like DatabaseVerification), while still using Spring Data MongoDB
     * for repository operations.
     *
     * <p><strong>Note on null safety:</strong> The {@code mongoClient()} method is provided by
     * {@link AbstractMongoClientConfiguration} and is guaranteed to return a non-null instance
     * by Spring's bean lifecycle management. The MongoClient bean is created before this method
     * is called during application context initialization.
     *
     * @return the configured MongoDatabase instance
     * @throws IllegalStateException if MongoClient is not properly initialized (should never happen
     *         in normal Spring context lifecycle)
     */
    @Bean
    @NonNull
    public MongoDatabase mongoDatabase() {
        MongoClient client = mongoClient();

        // Defensive check - should never be null due to Spring's bean lifecycle guarantees,
        // but we check anyway to fail fast with a clear error message if something is wrong
        if (client == null) {
            throw new IllegalStateException(
                "MongoClient is not initialized. This indicates a problem with Spring context initialization."
            );
        }

        return client.getDatabase(databaseName);
    }
}
