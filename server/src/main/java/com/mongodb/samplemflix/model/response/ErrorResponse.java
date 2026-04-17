package com.mongodb.samplemflix.model.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;
import lombok.Builder;

/**
 * Error response wrapper for API error responses.
 *
 * <p>This class wraps error responses with error codes, messages, and metadata.
 *
 * <pre> {
 *   success: false,
 *   message: string,
 *   error: {
 *     message: string,
 *     code?: string,
 *     details?: any
 *   },
 *   timestamp: string
 * }</pre>
 */
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse (

    /**
     * Always false for error responses.
     */
    boolean success,

    /**
     * High-level error message.
     */
    String message,

    /**
     * Detailed error information.
     */
    ErrorDetails error,

    /**
     * ISO 8601 timestamp when the error occurred.
     */
    String timestamp) implements ApiResponse {

    // Partial builder declaration to provide defaults for records (like @Builder.Default for classes)
    public static class ErrorResponseBuilder {
        private boolean success = false;
        private String timestamp = Instant.now().toString();
    }

    /**
     * Nested class for detailed error information.
     */
    @Builder
    public record ErrorDetails (
        /**
         * Detailed error message.
         */
        String message,

        /**
         * Error code (e.g., "VALIDATION_ERROR", "NOT_FOUND").
         */
        String code,

        /**
         * Additional error details (optional).
         */
        Object details) {}
}
