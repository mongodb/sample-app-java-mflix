package com.mongodb.samplemflix.controller;

import com.mongodb.samplemflix.model.Movie;
import com.mongodb.samplemflix.model.dto.BatchInsertResponse;
import com.mongodb.samplemflix.model.dto.BatchUpdateResponse;
import com.mongodb.samplemflix.model.dto.CreateMovieRequest;
import com.mongodb.samplemflix.model.dto.DeleteResponse;
import com.mongodb.samplemflix.model.dto.DirectorStatisticsResult;
import com.mongodb.samplemflix.model.dto.MovieSearchQuery;
import com.mongodb.samplemflix.model.dto.MovieWithCommentsResult;
import com.mongodb.samplemflix.model.dto.MoviesByYearResult;
import com.mongodb.samplemflix.model.dto.SearchMoviesResponse;
import com.mongodb.samplemflix.model.dto.UpdateMovieRequest;
import com.mongodb.samplemflix.model.dto.VectorSearchResult;
import com.mongodb.samplemflix.model.response.SuccessResponse;
import com.mongodb.samplemflix.service.MovieService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.bson.Document;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for movie-related endpoints.
 *
 * <p>This controller handles all HTTP requests for movie operations including:
 * <pre>
 * - GET /api/movies - Get all movies with filtering, sorting, and pagination
 * - GET /api/movies/{id} - Get a single movie by ID
 * - POST /api/movies - Create a new movie
 * - POST /api/movies/batch - Create multiple movies
 * - PATCH /api/movies/{id} - Update a movie
 * - PATCH /api/movies - Update multiple movies
 * - DELETE /api/movies/{id} - Delete a movie
 * - DELETE /api/movies - Delete multiple movies
 * - DELETE /api/movies/{id}/find-and-delete - Find and delete a movie
 * - GET /api/movies/aggregations/reportingByComments - Aggregate movies with most comments
 * - GET /api/movies/aggregations/reportingByYear - Aggregate movies by year with statistics
 * - GET /api/movies/aggregations/reportingByDirectors - Aggregate directors with most movies
 * - GET /api/movies/search - Text search using MongoDB Search Index across multiple fields (plot, fullplot, directors, writers, cast)
 * - GET /api/movies/vector-search - Vector search using Voyage AI embeddings to find movies with similar plots
 * - GET /api/movies/find-similar-movies - Vector search to find similar movies based on plot embeddings
 * </pre>
 */
@RestController
@RequestMapping("/api/movies")
@Tag(name = "Movies", description = "Movie management endpoints for CRUD operations, search, and aggregations")
public class MovieControllerImpl {
    
    private final MovieService movieService;
    
    public MovieControllerImpl(MovieService movieService) {
        this.movieService = movieService;
    }
    
    @Operation(
        summary = "Get all movies with optional filtering, sorting, and pagination",
        description = "Retrieve a list of movies with optional filtering by text search, genre, year, and rating. " +
                     "Supports sorting and pagination. Text search (q parameter) uses MongoDB text index to search " +
                     "across plot, title, and fullplot fields."
    )
    @GetMapping
    public ResponseEntity<SuccessResponse<List<Movie>>> getAllMovies(
            @Parameter(description = "Text search query (searches plot, title, fullplot)")
            @RequestParam(required = false) String q,
            @Parameter(description = "Filter by genre (case-insensitive partial match)")
            @RequestParam(required = false) String genre,
            @Parameter(description = "Filter by exact year")
            @RequestParam(required = false) Integer year,
            @Parameter(description = "Minimum IMDB rating (inclusive)")
            @RequestParam(required = false) Double minRating,
            @Parameter(description = "Maximum IMDB rating (inclusive)")
            @RequestParam(required = false) Double maxRating,
            @Parameter(description = "Number of results to return (default: 20)")
            @RequestParam(defaultValue = "20") Integer limit,
            @Parameter(description = "Number of results to skip for pagination (default: 0)")
            @RequestParam(defaultValue = "0") Integer skip,
            @Parameter(description = "Field to sort by (default: title)")
            @RequestParam(defaultValue = "title") String sortBy,
            @Parameter(description = "Sort order: 'asc' or 'desc' (default: asc)")
            @RequestParam(defaultValue = "asc") String sortOrder) {
        
        MovieSearchQuery query = MovieSearchQuery.builder()
                .q(q)
                .genre(genre)
                .year(year)
                .minRating(minRating)
                .maxRating(maxRating)
                .limit(limit)
                .skip(skip)
                .sortBy(sortBy)
                .sortOrder(sortOrder)
                .build();
        
        List<Movie> movies = movieService.getAllMovies(query);
        
        SuccessResponse<List<Movie>> response = SuccessResponse.<List<Movie>>builder()
                .success(true)
                .message("Found " + movies.size() + " movies")
                .data(movies)
                .timestamp(Instant.now().toString())
                .build();
        
        return ResponseEntity.ok(response);
    }

    @Operation(
        summary = "Get all distinct genres",
        description = "Retrieve a list of all unique genre values from the movies collection. " +
                     "Demonstrates the distinct() operation. Returns genres sorted alphabetically."
    )
    @GetMapping("/genres")
    public ResponseEntity<SuccessResponse<List<String>>> getDistinctGenres() {
        List<String> genres = movieService.getDistinctGenres();

        SuccessResponse<List<String>> response = SuccessResponse.<List<String>>builder()
                .success(true)
                .message("Found " + genres.size() + " distinct genres")
                .data(genres)
                .timestamp(Instant.now().toString())
                .build();

        return ResponseEntity.ok(response);
    }

    @Operation(
        summary = "Get a single movie by ID",
        description = "Retrieve a single movie by its MongoDB ObjectId."
    )
    @GetMapping("/{id}")
    public ResponseEntity<SuccessResponse<Movie>> getMovieById(
            @Parameter(description = "Movie ObjectId (24-character hex string)", required = true)
            @PathVariable String id) {
        Movie movie = movieService.getMovieById(id);
        
        SuccessResponse<Movie> response = SuccessResponse.<Movie>builder()
                .success(true)
                .message("Movie retrieved successfully")
                .data(movie)
                .timestamp(Instant.now().toString())
                .build();
        
        return ResponseEntity.ok(response);
    }
    
    @Operation(
        summary = "Create a new movie",
        description = "Create a single new movie document. Only the title field is required; all other fields are optional."
    )
    @PostMapping
    public ResponseEntity<SuccessResponse<Movie>> createMovie(
            @Parameter(description = "Movie data to create", required = true)
            @Valid @RequestBody CreateMovieRequest request) {
        Movie movie = movieService.createMovie(request);
        
        SuccessResponse<Movie> response = SuccessResponse.<Movie>builder()
                .success(true)
                .message("Movie '" + request.getTitle() + "' created successfully")
                .data(movie)
                .timestamp(Instant.now().toString())
                .build();
        
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    @Operation(
        summary = "Create multiple movies in batch",
        description = "Create multiple movie documents in a single operation using insertMany."
    )
    @PostMapping("/batch")
    public ResponseEntity<SuccessResponse<BatchInsertResponse>> createMoviesBatch(
            @Parameter(description = "List of movies to create", required = true)
            @RequestBody List<CreateMovieRequest> requests) {
        BatchInsertResponse result = movieService.createMoviesBatch(requests);

        SuccessResponse<BatchInsertResponse> response = SuccessResponse.<BatchInsertResponse>builder()
                .success(true)
                .message("Successfully created " + result.getInsertedCount() + " movies")
                .data(result)
                .timestamp(Instant.now().toString())
                .build();

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    /**
     * PATCH /api/movies/{id}
     *
     * <p>Updates a single movie document.
     */

    @Operation(
        summary = "Update a movie by ID",
        description = "Update a single movie document by its ObjectId using updateOne with $set operator."
    )
    @PatchMapping("/{id}")
    public ResponseEntity<SuccessResponse<Movie>> updateMovie(
            @Parameter(description = "Movie ObjectId to update", required = true)
            @PathVariable String id,
            @Parameter(description = "Updated movie data (only provided fields will be updated)", required = true)
            @RequestBody UpdateMovieRequest request) {
        Movie movie = movieService.updateMovie(id, request);

        SuccessResponse<Movie> response = SuccessResponse.<Movie>builder()
                .success(true)
                .message("Movie updated successfully")
                .data(movie)
                .timestamp(Instant.now().toString())
                .build();

        return ResponseEntity.ok(response);
    }
    
    @Operation(
        summary = "Update multiple movies in batch",
        description = "Update multiple movies matching the given filter using updateMany. " +
                     "Request body should contain 'filter' and 'update' objects."
    )
    @SuppressWarnings("unchecked")
    @PatchMapping
    public ResponseEntity<SuccessResponse<BatchUpdateResponse>> updateMoviesBatch(
            @Parameter(description = "Request body with 'filter' and 'update' objects", required = true)
            @RequestBody Map<String, Object> body) {
        Document filter = new Document((Map<String, Object>) body.get("filter"));
        Document update = new Document((Map<String, Object>) body.get("update"));

        BatchUpdateResponse result = movieService.updateMoviesBatch(filter, update);

        SuccessResponse<BatchUpdateResponse> response = SuccessResponse.<BatchUpdateResponse>builder()
                .success(true)
                .message("Update operation completed. Matched " + result.getMatchedCount() +
                        " documents, modified " + result.getModifiedCount() + " documents.")
                .data(result)
                .timestamp(Instant.now().toString())
                .build();

        return ResponseEntity.ok(response);
    }
    
    @Operation(
        summary = "Find and delete a movie atomically",
        description = "Find and delete a movie in a single atomic operation using findOneAndDelete. " +
                     "Returns the deleted movie document."
    )
    @DeleteMapping("/{id}/find-and-delete")
    public ResponseEntity<SuccessResponse<Movie>> findAndDeleteMovie(
            @Parameter(description = "Movie ObjectId to find and delete", required = true)
            @PathVariable String id) {
        Movie movie = movieService.findAndDeleteMovie(id);
        
        SuccessResponse<Movie> response = SuccessResponse.<Movie>builder()
                .success(true)
                .message("Movie found and deleted successfully")
                .data(movie)
                .timestamp(Instant.now().toString())
                .build();
        
        return ResponseEntity.ok(response);
    }
    
    @Operation(
        summary = "Delete a movie by ID",
        description = "Delete a single movie document by its ObjectId using deleteOne."
    )
    @DeleteMapping("/{id}")
    public ResponseEntity<SuccessResponse<DeleteResponse>> deleteMovie(
            @Parameter(description = "Movie ObjectId to delete", required = true)
            @PathVariable String id) {
        DeleteResponse result = movieService.deleteMovie(id);

        SuccessResponse<DeleteResponse> response = SuccessResponse.<DeleteResponse>builder()
                .success(true)
                .message("Movie deleted successfully")
                .data(result)
                .timestamp(Instant.now().toString())
                .build();

        return ResponseEntity.ok(response);
    }
    
    @Operation(
        summary = "Delete multiple movies in batch",
        description = "Delete multiple movies matching the given filter using deleteMany. " +
                     "Request body should contain a 'filter' object."
    )
    @SuppressWarnings("unchecked")
    @DeleteMapping
    public ResponseEntity<SuccessResponse<DeleteResponse>> deleteMoviesBatch(
            @Parameter(description = "Request body with 'filter' object", required = true)
            @RequestBody Map<String, Object> body) {
        Document filter = new Document((Map<String, Object>) body.get("filter"));

        DeleteResponse result = movieService.deleteMoviesBatch(filter);

        SuccessResponse<DeleteResponse> response = SuccessResponse.<DeleteResponse>builder()
                .success(true)
                .message("Delete operation completed. Removed " + result.getDeletedCount() + " documents.")
                .data(result)
                .timestamp(Instant.now().toString())
                .build();

        return ResponseEntity.ok(response);
    }

    // Aggregation endpoints for reporting

    @Operation(
        summary = "Aggregate movies with their most recent comments",
        description = "Aggregates movies with their most recent comments using MongoDB $lookup (join) operation. " +
                     "Demonstrates how to combine data from the movies and comments collections."
    )
    @GetMapping("/aggregations/reportingByComments")
    public ResponseEntity<SuccessResponse<List<MovieWithCommentsResult>>> getMoviesWithMostRecentComments(
            @Parameter(description = "Maximum number of movies to return (default: 10, max: 50)")
            @RequestParam(defaultValue = "10") Integer limit,
            @Parameter(description = "Optional movie ID to filter by specific movie")
            @RequestParam(required = false) String movieId) {

        List<MovieWithCommentsResult> results = movieService.getMoviesWithMostRecentComments(limit, movieId);

        // Calculate total comments across all movies
        int totalComments = results.stream()
                .mapToInt(result -> result.getTotalComments() != null ? result.getTotalComments() : 0)
                .sum();

        String message = movieId != null
                ? String.format("Found %d comments from movie", totalComments)
                : String.format("Found %d comments from %d movie%s",
                        totalComments, results.size(), results.size() != 1 ? "s" : "");

        SuccessResponse<List<MovieWithCommentsResult>> response =
                SuccessResponse.<List<MovieWithCommentsResult>>builder()
                        .success(true)
                        .message(message)
                        .data(results)
                        .timestamp(Instant.now().toString())
                        .build();

        return ResponseEntity.ok(response);
    }

    @Operation(
        summary = "Aggregate movies by year with statistics",
        description = "Aggregates movies by year with statistics including movie count and average rating. " +
                     "Demonstrates MongoDB $group operation for statistical aggregation."
    )
    @GetMapping("/aggregations/reportingByYear")
    public ResponseEntity<SuccessResponse<List<MoviesByYearResult>>> getMoviesByYearWithStats() {

        List<MoviesByYearResult> results = movieService.getMoviesByYearWithStats();

        SuccessResponse<List<MoviesByYearResult>> response =
                SuccessResponse.<List<MoviesByYearResult>>builder()
                        .success(true)
                        .message(String.format("Aggregated statistics for %d years", results.size()))
                        .data(results)
                        .timestamp(Instant.now().toString())
                        .build();

        return ResponseEntity.ok(response);
    }

    @Operation(
        summary = "Aggregate directors with the most movies",
        description = "Aggregates directors with the most movies and their statistics. " +
                     "Demonstrates MongoDB $unwind operation for array flattening and aggregation."
    )
    @GetMapping("/aggregations/reportingByDirectors")
    public ResponseEntity<SuccessResponse<List<DirectorStatisticsResult>>> getDirectorsWithMostMovies(
            @Parameter(description = "Maximum number of directors to return (default: 20, max: 100)")
            @RequestParam(defaultValue = "20") Integer limit) {

        List<DirectorStatisticsResult> results = movieService.getDirectorsWithMostMovies(limit);

        SuccessResponse<List<DirectorStatisticsResult>> response =
                SuccessResponse.<List<DirectorStatisticsResult>>builder()
                        .success(true)
                        .message(String.format("Found %d directors with most movies", results.size()))
                        .data(results)
                        .timestamp(Instant.now().toString())
                        .build();

        return ResponseEntity.ok(response);
    }

    // MongoDB Search endpoints

    @Operation(
        summary = "Search movies using MongoDB Search",
        description = "Search movies using MongoDB Search across multiple fields (plot, fullplot, directors, writers, cast). " +
                     "You can combine multiple fields in a single query and control how they are combined using the searchOperator parameter. " +
                     "At least one search field must be provided. " +
                     "Plot and fullplot use phrase operator for exact matching, while directors, writers, and cast use text operator with fuzzy matching."
    )
    @GetMapping("/search")
    public ResponseEntity<SuccessResponse<SearchMoviesResponse>> searchMovies(
            @Parameter(description = "Text to search in the plot field (phrase matching)")
            @RequestParam(required = false) String plot,
            @Parameter(description = "Text to search in the fullplot field (phrase matching)")
            @RequestParam(required = false) String fullplot,
            @Parameter(description = "Text to search in the directors field (fuzzy matching)")
            @RequestParam(required = false) String directors,
            @Parameter(description = "Text to search in the writers field (fuzzy matching)")
            @RequestParam(required = false) String writers,
            @Parameter(description = "Text to search in the cast field (fuzzy matching)")
            @RequestParam(required = false) String cast,
            @Parameter(description = "Maximum number of movies to return (default: 20, max: 100)")
            @RequestParam(defaultValue = "20") Integer limit,
            @Parameter(description = "Number of results to skip for pagination (default: 0)")
            @RequestParam(defaultValue = "0") Integer skip,
            @Parameter(description = "Compound operator: must, should, mustNot, or filter (default: must)")
            @RequestParam(defaultValue = "must") String searchOperator) {

        com.mongodb.samplemflix.model.dto.MovieSearchRequest searchRequest =
            com.mongodb.samplemflix.model.dto.MovieSearchRequest.builder()
                .plot(plot)
                .fullplot(fullplot)
                .directors(directors)
                .writers(writers)
                .cast(cast)
                .limit(limit)
                .skip(skip)
                .searchOperator(searchOperator)
                .build();

        List<Movie> movies = movieService.searchMovies(searchRequest);

        // Wrap results in SearchMoviesResponse
        SearchMoviesResponse searchResponse = SearchMoviesResponse.builder()
                .movies(movies)
                .totalCount(movies.size())
                .build();

        SuccessResponse<SearchMoviesResponse> response = SuccessResponse.<SearchMoviesResponse>builder()
                .success(true)
                .message(String.format("Found %d movies matching the search criteria", movies.size()))
                .data(searchResponse)
                .timestamp(Instant.now().toString())
                .build();

        return ResponseEntity.ok(response);
    }

    @Operation(
        summary = "Search movies using vector search with Voyage AI embeddings",
        description = "Search movies using MongoDB Vector Search to find movies with similar plots. " +
                     "Uses embeddings generated by the Voyage AI model to perform semantic similarity search. " +
                     "This endpoint generates an embedding for the search query and finds movies with similar plot embeddings."
    )
    @GetMapping("/vector-search")
    public ResponseEntity<SuccessResponse<List<VectorSearchResult>>> vectorSearchMovies(
            @Parameter(description = "Search query text to find movies with similar plots", required = true)
            @RequestParam String q,
            @Parameter(description = "Maximum number of results to return (default: 10, max: 50)")
            @RequestParam(defaultValue = "10") Integer limit) {

        List<VectorSearchResult> results = movieService.vectorSearchMovies(q, limit);

        SuccessResponse<List<VectorSearchResult>> response = SuccessResponse.<List<VectorSearchResult>>builder()
                .success(true)
                .message(String.format("Found %d similar movies for query: '%s'", results.size(), q))
                .data(results)
                .timestamp(Instant.now().toString())
                .build();

        return ResponseEntity.ok(response);
    }

    @Operation(
        summary = "Find similar movies using vector search",
        description = "Find similar movies using MongoDB Vector Search on plot embeddings. " +
                     "Demonstrates how to use vector search to find movies with similar plots based on semantic similarity."
    )
    @GetMapping("/find-similar-movies")
    public ResponseEntity<SuccessResponse<List<Movie>>> findSimilarMovies(
            @Parameter(description = "ID of the movie to find similar movies for", required = true)
            @RequestParam String movieId,
            @Parameter(description = "Maximum number of similar movies to return (default: 10, max: 50)")
            @RequestParam(defaultValue = "10") Integer limit) {

        List<Movie> movies = movieService.findSimilarMovies(movieId, limit);

        SuccessResponse<List<Movie>> response = SuccessResponse.<List<Movie>>builder()
                .success(true)
                .message(String.format("Found %d similar movies", movies.size()))
                .data(movies)
                .timestamp(Instant.now().toString())
                .build();

        return ResponseEntity.ok(response);
    }
}
