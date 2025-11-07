package com.mongodb.samplemflix.model.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Data Transfer Object for updating an existing movie.
 *
 * <p>This DTO is used for PATCH /api/movies/{id} requests.
 * All fields are optional since partial updates are allowed.
 * Any field that is null will not be updated in the database.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateMovieRequest {

    /**
     * Movie title (optional).
     */
    private String title;

    /**
     * Release year (optional).
     */
    private Integer year;

    /**
     * Short plot summary (optional).
     */
    private String plot;

    /**
     * Full plot description (optional).
     */
    private String fullplot;

    /**
     * List of genres (optional).
     */
    private List<String> genres;

    /**
     * List of directors (optional).
     */
    private List<String> directors;

    /**
     * List of writers (optional).
     */
    private List<String> writers;

    /**
     * List of cast members (optional).
     */
    private List<String> cast;

    /**
     * List of countries (optional).
     */
    private List<String> countries;

    /**
     * List of languages (optional).
     */
    private List<String> languages;

    /**
     * Movie rating (optional).
     */
    private String rated;

    /**
     * Runtime in minutes (optional).
     */
    private Integer runtime;

    /**
     * Poster image URL (optional).
     */
    private String poster;
}
