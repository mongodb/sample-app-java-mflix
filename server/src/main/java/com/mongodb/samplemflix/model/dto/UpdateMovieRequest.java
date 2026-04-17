package com.mongodb.samplemflix.model.dto;

import java.util.List;
import lombok.Builder;

/**
 * Data Transfer Object for updating an existing movie.
 *
 * <p>This DTO is used for PATCH /api/movies/{id} requests.
 * All fields are optional since partial updates are allowed.
 * Any field that is null will not be updated in the database.
 */
@Builder
public record UpdateMovieRequest (

    /**
     * Movie title (optional).
     */
    String title,

    /**
     * Release year (optional).
     */
    Integer year,

    /**
     * Short plot summary (optional).
     */
    String plot,

    /**
     * Full plot description (optional).
     */
    String fullplot,

    /**
     * List of genres (optional).
     */
    List<String> genres,

    /**
     * List of directors (optional).
     */
    List<String> directors,

    /**
     * List of writers (optional).
     */
    List<String> writers,

    /**
     * List of cast members (optional).
     */
    List<String> cast,

    /**
     * List of countries (optional).
     */
    List<String> countries,

    /**
     * List of languages (optional).
     */
    List<String> languages,

    /**
     * Movie rating (optional).
     */
    String rated,

    /**
     * Runtime in minutes (optional).
     */
    Integer runtime,

    /**
     * Poster image URL (optional).
     */
    String poster) {}
