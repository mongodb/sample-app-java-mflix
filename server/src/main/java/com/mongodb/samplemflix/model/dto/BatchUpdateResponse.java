package com.mongodb.samplemflix.model.dto;

/**
 * Response DTO for batch update operations.
 */
public record BatchUpdateResponse (
        long matchedCount,
        long modifiedCount) {}
