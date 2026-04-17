package com.mongodb.samplemflix.model.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.With;

/**
 * DTO for movies aggregated by year with statistics.
 *
 * <p>This class represents the result of the reportingByYear aggregation
 * which groups movies by release year and calculates statistics per year.
 */
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public record MoviesByYearResult (

    /**
     * Release year.
     */
    Integer year,

    /**
     * Number of movies released in this year.
     */
    Integer movieCount,

    /**
     * Average IMDB rating for movies in this year.
     */
    @With
    Double averageRating,

    /**
     * Highest IMDB rating for movies in this year.
     */
    Double highestRating,

    /**
     * Lowest IMDB rating for movies in this year.
     */
    Double lowestRating,

    /**
     * Total number of IMDB votes for all movies in this year.
     */
    Long totalVotes) {}

