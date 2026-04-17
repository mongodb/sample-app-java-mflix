package com.mongodb.samplemflix.model.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.List;
import lombok.Builder;

/**
 * Data Transfer Object for creating a new movie.
 *
 * <p>This DTO is used for POST /api/movies requests.
 * It includes validation annotations to ensure required fields are present.
 * Only the title field is required, all other fields are optional.
 */
@Builder
public record CreateMovieRequest (

    /**
     * Movie title (required).
     * Must not be blank.
     */
    @NotBlank(message = "Title is required")
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
