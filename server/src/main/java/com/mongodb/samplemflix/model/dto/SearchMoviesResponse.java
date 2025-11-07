package com.mongodb.samplemflix.model.dto;

import com.mongodb.samplemflix.model.Movie;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response wrapper for movie search results.
 *
 * <p>This DTO wraps the search results with pagination metadata,
 * matching the structure returned by the Python backend.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SearchMoviesResponse {
    
    /**
     * List of movies matching the search criteria.
     */
    private List<Movie> movies;
    
    /**
     * Total count of movies matching the search criteria.
     */
    private Integer totalCount;
}

