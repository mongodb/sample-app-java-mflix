package com.mongodb.samplemflix.model.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;
import java.util.List;
import lombok.Builder;

/**
 * DTO for movies with their most recent comments aggregation result.
 *
 * <p>This class represents the result of the reportingByComments aggregation
 * which joins movies with their comments and returns movies with the most comments.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Builder
public record MovieWithCommentsResult (

    /**
     * Movie ID as string.
     */
    String _id,

    /**
     * Movie title.
     */
    String title,

    /**
     * Release year.
     */
    Integer year,

    /**
     * Short plot summary.
     */
    String plot,

    /**
     * Poster image URL.
     */
    String poster,

    /**
     * List of genres.
     */
    List<String> genres,

    /**
     * IMDB rating (0.0 to 10.0).
     */
    Double imdbRating,

    /**
     * Most recent comments for this movie.
     */
    List<CommentInfo> recentComments,

    /**
     * Total number of comments for this movie.
     */
    Integer totalComments,

    /**
     * Timestamp of the most recent comment as a UTC instant.
     *
     * <p>Uses {@link Instant} for an immutable, unambiguous UTC representation.
     * BSON DateTime values are converted via {@code Date.toInstant()}.
     */
    Instant mostRecentCommentDate) {

    /**
     * Nested record for comment information.
     */
    @Builder
    public record CommentInfo (
        /**
         * Comment ID as string.
         */
        String id,

        /**
         * Commenter name.
         */
        String name,

        /**
         * Commenter email.
         */
        String email,

        /**
         * Comment text.
         */
        String text,

        /**
         * Comment timestamp as a UTC instant.
         *
         * <p>Stored as BSON DateTime in MongoDB. Uses {@link Instant} for immutability
         * and unambiguous UTC semantics.
         */
        Instant date) {}
}

