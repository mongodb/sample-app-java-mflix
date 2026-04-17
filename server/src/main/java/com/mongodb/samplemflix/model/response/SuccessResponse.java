package com.mongodb.samplemflix.model.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;
import lombok.Builder;

/**
 * Success response wrapper for API responses.
 *
 * <p>This class wraps successful API responses with metadata like timestamp and pagination.
 * It uses a generic type parameter T to hold the response data.
 *
 * <pre>  {
 *   success: true,
 *   message?: string,
 *   data: T,
 *   timestamp: string,
 *   pagination?: { page, limit, total, pages }
 * }</pre>
 */
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public record SuccessResponse<T> (

    /**
     * Always true for success responses.
     */
    boolean success,

    /**
     * Optional success message.
     */
    String message,

    /**
     * The response data (generic type).
     */
    T data,

    /**
     * ISO 8601 timestamp when the response was generated.
     */
    String timestamp,

    /**
     * Optional pagination metadata (for list responses).
     */
    Pagination pagination) implements ApiResponse {

    // Partial builder declaration to provide defaults for records (like @Builder.Default for classes)
    public static class SuccessResponseBuilder<T> {
        private boolean success = true;
        private String timestamp = Instant.now().toString();
    }

    /**
     * Nested class for pagination metadata.
     */
    @Builder
    public record Pagination (
        /**
         * Current page number (1-based).
         */
        int page,

        /**
         * Number of items per page.
         */
        int limit,

        /**
         * Total number of items.
         */
        long total,

        /**
         * Total number of pages.
         */
        int pages) {}
}
