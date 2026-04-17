package com.mongodb.samplemflix.model.dto;

import java.util.Collection;
import org.bson.BsonValue;

/**
 * Response DTO for batch insert operations.
 */
public record BatchInsertResponse (
        int insertedCount,
        Collection<BsonValue> insertedIds) {}

