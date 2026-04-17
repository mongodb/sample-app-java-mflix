package com.mongodb.samplemflix.model.dto;

import lombok.Builder;

/**
 * Data Transfer Object for movie search query parameters.
 *
 * <p>This DTO is used to parse and validate query parameters for GET /api/movies requests.
 * It supports full-text search, filtering by genre/year/rating, sorting, and pagination.
 */
@Builder
public record MovieSearchQuery (
    
    /**
     * Full-text search query.
     * Searches across plot, title, and fullplot fields using MongoDB text index.
     */
    String q,
    
    /**
     * Filter by genre (case-insensitive partial match).
     */
    String genre,
    
    /**
     * Filter by exact year.
     */
    Integer year,
    
    /**
     * Minimum IMDB rating (inclusive).
     */
    Double minRating,
    
    /**
     * Maximum IMDB rating (inclusive).
     */
    Double maxRating,
    
    /**
     * Number of results to return (default: 20, max: 100).
     */
    Integer limit,
    
    /**
     * Number of results to skip for pagination (default: 0).
     */
    Integer skip,
    
    /**
     * Field to sort by (e.g., "title", "year", "imdb.rating").
     * Default: "title"
     */
    String sortBy,
    
    /**
     * Sort order: "asc" or "desc".
     * Default: "asc"
     */
    String sortOrder) {}
