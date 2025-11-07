package com.mongodb.samplemflix.model.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse implements ApiResponse {

    /**
     * Always false for error responses.
     */
    @Builder.Default
    private boolean success = false;

    /**
     * High-level error message.
     */
    private String message;

    /**
     * Detailed error information.
     */
    private ErrorDetails error;

    /**
     * ISO 8601 timestamp when the error occurred.
     */
    @Builder.Default
    private String timestamp = Instant.now().toString();

    /**
     * Nested class for detailed error information.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ErrorDetails {
        /**
         * Detailed error message.
         */
        private String message;

        /**
         * Error code (e.g., "VALIDATION_ERROR", "NOT_FOUND").
         */
        private String code;

        /**
         * Additional error details (optional).
         */
        private Object details;
    }
}
