package com.mongodb.samplemflix.model.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.With;

/**
 * DTO for director statistics aggregation result.
 *
 * <p>This class represents the result of the reportingByDirectors aggregation
 * which finds directors with the most movies and their statistics.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Builder
public record DirectorStatisticsResult (
    /**
     * Director name.
     */
    String director,

    /**
     * Number of movies directed by this director.
     */
    Integer movieCount,

    /**
     * Average IMDB rating of this director's movies.
     */
    @With
    Double averageRating) {}

