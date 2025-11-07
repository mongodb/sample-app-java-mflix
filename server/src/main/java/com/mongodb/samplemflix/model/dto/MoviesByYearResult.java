package com.mongodb.samplemflix.model.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for movies aggregated by year with statistics.
 *
 * <p>This class represents the result of the reportingByYear aggregation
 * which groups movies by release year and calculates statistics per year.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MoviesByYearResult {

    /**
     * Release year.
     */
    private Integer year;

    /**
     * Number of movies released in this year.
     */
    private Integer movieCount;

    /**
     * Average IMDB rating for movies in this year.
     */
    private Double averageRating;

    /**
     * Highest IMDB rating for movies in this year.
     */
    private Double highestRating;

    /**
     * Lowest IMDB rating for movies in this year.
     */
    private Double lowestRating;

    /**
     * Total number of IMDB votes for all movies in this year.
     */
    private Long totalVotes;
}

