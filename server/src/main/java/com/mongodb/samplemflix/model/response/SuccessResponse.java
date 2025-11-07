package com.mongodb.samplemflix.model.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SuccessResponse<T> implements ApiResponse {

    /**
     * Always true for success responses.
     */
    @Builder.Default
    private boolean success = true;

    /**
     * Optional success message.
     */
    private String message;

    /**
     * The response data (generic type).
     */
    private T data;

    /**
     * ISO 8601 timestamp when the response was generated.
     */
    @Builder.Default
    private String timestamp = Instant.now().toString();

    /**
     * Optional pagination metadata (for list responses).
     */
    private Pagination pagination;

    /**
     * Nested class for pagination metadata.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Pagination {
        /**
         * Current page number (1-based).
         */
        private int page;

        /**
         * Number of items per page.
         */
        private int limit;

        /**
         * Total number of items.
         */
        private long total;

        /**
         * Total number of pages.
         */
        private int pages;
    }
}
