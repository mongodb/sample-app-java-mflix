package com.mongodb.samplemflix.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Data Transfer Object for vector search results.
 *
 * <p>This DTO represents the result of a MongoDB Vector Search query,
 * containing the movie information and similarity score.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VectorSearchResult {
    
    /**
     * Movie ObjectId as a string.
     */
    private String id;
    
    /**
     * Movie title.
     */
    private String title;
    
    /**
     * Movie plot summary.
     */
    private String plot;
    
    /**
     * Movie poster URL.
     */
    private String poster;
    
    /**
     * Movie release year.
     */
    private Integer year;
    
    /**
     * Movie genres.
     */
    private java.util.List<String> genres;
    
    /**
     * Movie directors.
     */
    private java.util.List<String> directors;
    
    /**
     * Movie cast members.
     */
    private java.util.List<String> cast;
    
    /**
     * Vector search similarity score (0.0 to 1.0, higher = more similar).
     */
    private Double score;
}

