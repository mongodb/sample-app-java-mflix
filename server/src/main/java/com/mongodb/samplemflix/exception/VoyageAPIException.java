package com.mongodb.samplemflix.exception;

/**
 * Exception thrown when Voyage AI API returns an error.
 * 
 * This exception results in a 503 Service Unavailable response.
 * Typically occurs when:
 * - The Voyage AI API is down or unavailable
 * - The API returns an error response
 * - Network issues prevent communication with the API
 */
public class VoyageAPIException extends RuntimeException {
    
    private final int statusCode;
    
    public VoyageAPIException(String message, int statusCode) {
        super(message);
        this.statusCode = statusCode;
    }
    
    public VoyageAPIException(String message) {
        this(message, 503);
    }
    
    public int getStatusCode() {
        return statusCode;
    }
}

