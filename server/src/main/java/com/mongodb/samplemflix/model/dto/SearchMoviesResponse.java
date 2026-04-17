package com.mongodb.samplemflix.model.dto;

import com.mongodb.samplemflix.model.Movie;
import java.util.List;
import lombok.Builder;

/**
 * Response wrapper for movie search results.
 *
 * <p>This DTO wraps the search results with pagination metadata,
 * matching the structure returned by the Python backend.
 */
@Builder
public record SearchMoviesResponse (
    
    /**
     * List of movies matching the search criteria.
     */
    List<Movie> movies,
    
    /**
     * Total count of movies matching the search criteria.
     */
    Integer totalCount) {}

