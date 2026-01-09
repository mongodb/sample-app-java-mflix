package com.mongodb.samplemflix.exception;

/**
 * Exception thrown when a required service is unavailable or not configured.
 * 
 * This exception results in a 400 Bad Request response with SERVICE_UNAVAILABLE code.
 * Typically occurs when:
 * - A required API key is not configured
 * - A required service is not available
 */
public class ServiceUnavailableException extends RuntimeException {
    
    public ServiceUnavailableException(String message) {
        super(message);
    }
}

