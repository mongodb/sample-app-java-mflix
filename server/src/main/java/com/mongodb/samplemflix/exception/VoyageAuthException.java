package com.mongodb.samplemflix.exception;

/**
 * Exception thrown when Voyage AI API authentication fails.
 * 
 * This exception results in a 401 Unauthorized response.
 * Typically occurs when:
 * - The API key is invalid
 * - The API key is missing
 * - The API key has expired
 */
public class VoyageAuthException extends RuntimeException {
    
    public VoyageAuthException(String message) {
        super(message);
    }
}

