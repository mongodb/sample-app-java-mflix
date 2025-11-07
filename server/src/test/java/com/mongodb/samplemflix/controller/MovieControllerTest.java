package com.mongodb.samplemflix.controller;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.samplemflix.exception.ResourceNotFoundException;
import com.mongodb.samplemflix.exception.ValidationException;
import com.mongodb.samplemflix.model.Movie;
import com.mongodb.samplemflix.model.dto.BatchInsertResponse;
import com.mongodb.samplemflix.model.dto.BatchUpdateResponse;
import com.mongodb.samplemflix.model.dto.CreateMovieRequest;
import com.mongodb.samplemflix.model.dto.DeleteResponse;
import com.mongodb.samplemflix.model.dto.DirectorStatisticsResult;
import com.mongodb.samplemflix.model.dto.MovieSearchQuery;
import com.mongodb.samplemflix.model.dto.MovieWithCommentsResult;
import com.mongodb.samplemflix.model.dto.MoviesByYearResult;
import com.mongodb.samplemflix.model.dto.UpdateMovieRequest;
import com.mongodb.samplemflix.model.dto.VectorSearchResult;
import com.mongodb.samplemflix.service.MovieService;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.bson.BsonObjectId;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Unit tests for MovieControllerImpl.
 *
 * These tests verify the REST API endpoints by mocking the service layer.
 * Uses Spring's MockMvc for testing HTTP requests and responses.
 */
@WebMvcTest(MovieControllerImpl.class)
@DisplayName("MovieController Unit Tests")
class MovieControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private MovieService movieService;

    private ObjectId testId;
    private Movie testMovie;
    private CreateMovieRequest createRequest;
    private UpdateMovieRequest updateRequest;

    @BeforeEach
    void setUp() {
        testId = new ObjectId();

        testMovie = Movie.builder()
                .id(testId)
                .title("Test Movie")
                .year(2024)
                .plot("A test plot")
                .genres(Arrays.asList("Action", "Drama"))
                .build();

        createRequest = CreateMovieRequest.builder()
                .title("New Movie")
                .year(2024)
                .plot("A new movie plot")
                .build();

        updateRequest = UpdateMovieRequest.builder()
                .title("Updated Title")
                .year(2025)
                .build();
    }

    // ==================== GET ALL MOVIES TESTS ====================

    @Test
    @DisplayName("GET /api/movies - Should return list of movies")
    void testGetAllMovies_Success() throws Exception {
        // Arrange
        List<Movie> movies = Arrays.asList(testMovie);
        when(movieService.getAllMovies(any(MovieSearchQuery.class))).thenReturn(movies);

        // Act & Assert
        mockMvc.perform(get("/api/movies"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].title").value("Test Movie"))
                .andExpect(jsonPath("$.data[0].year").value(2024));
    }

    @Test
    @DisplayName("GET /api/movies - Should handle query parameters")
    void testGetAllMovies_WithQueryParams() throws Exception {
        // Arrange
        List<Movie> movies = Arrays.asList(testMovie);
        when(movieService.getAllMovies(any(MovieSearchQuery.class))).thenReturn(movies);

        // Act & Assert
        mockMvc.perform(get("/api/movies")
                        .param("q", "test")
                        .param("genre", "Action")
                        .param("year", "2024")
                        .param("limit", "10")
                        .param("skip", "0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray());
    }

    // ==================== GET MOVIE BY ID TESTS ====================

    @Test
    @DisplayName("GET /api/movies/{id} - Should return movie by ID")
    void testGetMovieById_Success() throws Exception {
        // Arrange
        String movieId = testId.toHexString();
        when(movieService.getMovieById(movieId)).thenReturn(testMovie);

        // Act & Assert
        mockMvc.perform(get("/api/movies/{id}", movieId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.title").value("Test Movie"))
                .andExpect(jsonPath("$.data.year").value(2024));
    }

    @Test
    @DisplayName("GET /api/movies/{id} - Should return 404 when movie not found")
    void testGetMovieById_NotFound() throws Exception {
        // Arrange
        String movieId = testId.toHexString();
        when(movieService.getMovieById(movieId))
                .thenThrow(new ResourceNotFoundException("Movie not found"));

        // Act & Assert
        mockMvc.perform(get("/api/movies/{id}", movieId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("RESOURCE_NOT_FOUND"))
                .andExpect(jsonPath("$.error.message").value("Movie not found"));
    }

    @Test
    @DisplayName("GET /api/movies/{id} - Should return 400 for invalid ID")
    void testGetMovieById_InvalidId() throws Exception {
        // Arrange
        String invalidId = "invalid-id";
        when(movieService.getMovieById(invalidId))
                .thenThrow(new ValidationException("Invalid movie ID format"));

        // Act & Assert
        mockMvc.perform(get("/api/movies/{id}", invalidId))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }

    // ==================== CREATE MOVIE TESTS ====================

    @Test
    @DisplayName("POST /api/movies - Should create movie successfully")
    void testCreateMovie_Success() throws Exception {
        // Arrange
        when(movieService.createMovie(any(CreateMovieRequest.class))).thenReturn(testMovie);

        // Act & Assert
        mockMvc.perform(post("/api/movies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.title").value("Test Movie"));
    }

    @Test
    @DisplayName("POST /api/movies - Should return 400 for validation error")
    void testCreateMovie_ValidationError() throws Exception {
        // Arrange
        when(movieService.createMovie(any(CreateMovieRequest.class)))
                .thenThrow(new ValidationException("Title is required"));

        // Act & Assert
        mockMvc.perform(post("/api/movies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }

    @Test
    @DisplayName("POST /api/movies/batch - Should create movies batch successfully")
    void testCreateMoviesBatch_Success() throws Exception {
        // Arrange
        List<CreateMovieRequest> requests = Arrays.asList(createRequest, createRequest);
        Map<Integer, org.bson.BsonValue> insertedIds = new HashMap<>();
        insertedIds.put(0, new BsonObjectId(new ObjectId()));
        insertedIds.put(1, new BsonObjectId(new ObjectId()));
        BatchInsertResponse response = new BatchInsertResponse(2, insertedIds.values());

        when(movieService.createMoviesBatch(anyList())).thenReturn(response);

        // Act & Assert
        mockMvc.perform(post("/api/movies/batch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requests)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.insertedCount").value(2));
    }

    // ==================== UPDATE MOVIE TESTS ====================

    @Test
    @DisplayName("PATCH /api/movies/{id} - Should update movie successfully")
    void testUpdateMovie_Success() throws Exception {
        // Arrange
        String movieId = testId.toHexString();
        Movie updatedMovie = Movie.builder()
                .id(testId)
                .title("Updated Title")
                .year(2025)
                .build();

        when(movieService.updateMovie(eq(movieId), any(UpdateMovieRequest.class)))
                .thenReturn(updatedMovie);

        // Act & Assert
        mockMvc.perform(patch("/api/movies/{id}", movieId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.title").value("Updated Title"))
                .andExpect(jsonPath("$.data.year").value(2025));
    }

    @Test
    @DisplayName("PATCH /api/movies/{id} - Should return 404 when movie not found")
    void testUpdateMovie_NotFound() throws Exception {
        // Arrange
        String movieId = testId.toHexString();
        when(movieService.updateMovie(eq(movieId), any(UpdateMovieRequest.class)))
                .thenThrow(new ResourceNotFoundException("Movie not found"));

        // Act & Assert
        mockMvc.perform(patch("/api/movies/{id}", movieId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("RESOURCE_NOT_FOUND"));
    }

    // ==================== DELETE MOVIE TESTS ====================

    @Test
    @DisplayName("DELETE /api/movies/{id} - Should delete movie successfully")
    void testDeleteMovie_Success() throws Exception {
        // Arrange
        String movieId = testId.toHexString();
        DeleteResponse response = new DeleteResponse(1L);

        when(movieService.deleteMovie(movieId)).thenReturn(response);

        // Act & Assert
        mockMvc.perform(delete("/api/movies/{id}", movieId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.deletedCount").value(1));
    }

    @Test
    @DisplayName("DELETE /api/movies/{id} - Should return 404 when movie not found")
    void testDeleteMovie_NotFound() throws Exception {
        // Arrange
        String movieId = testId.toHexString();
        when(movieService.deleteMovie(movieId))
                .thenThrow(new ResourceNotFoundException("Movie not found"));

        // Act & Assert
        mockMvc.perform(delete("/api/movies/{id}", movieId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("RESOURCE_NOT_FOUND"));
    }

    @Test
    @DisplayName("DELETE /api/movies/{id}/find-and-delete - Should find and delete movie successfully")
    void testFindAndDeleteMovie_Success() throws Exception {
        // Arrange
        String movieId = testId.toHexString();
        when(movieService.findAndDeleteMovie(movieId)).thenReturn(testMovie);

        // Act & Assert
        mockMvc.perform(delete("/api/movies/{id}/find-and-delete", movieId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.title").value("Test Movie"));
    }

    @Test
    @DisplayName("DELETE /api/movies/{id}/find-and-delete - Should return 404 when movie not found")
    void testFindAndDeleteMovie_NotFound() throws Exception {
        // Arrange
        String movieId = testId.toHexString();
        when(movieService.findAndDeleteMovie(movieId))
                .thenThrow(new ResourceNotFoundException("Movie not found"));

        // Act & Assert
        mockMvc.perform(delete("/api/movies/{id}/find-and-delete", movieId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("RESOURCE_NOT_FOUND"));
    }

    // ==================== AGGREGATION ENDPOINT TESTS ====================

    @Test
    @DisplayName("GET /api/movies/aggregations/reportingByComments - Should return movies with most comments")
    void testGetMoviesWithMostComments_Success() throws Exception {
        // Arrange
        MovieWithCommentsResult.CommentInfo comment = MovieWithCommentsResult.CommentInfo.builder()
                .id(new ObjectId().toHexString())
                .name("John Doe")
                .email("john@example.com")
                .text("Great movie!")
                .date(new Date())
                .build();

        MovieWithCommentsResult result = MovieWithCommentsResult.builder()
                ._id(testId.toHexString())
                .title("Test Movie")
                .year(2024)
                .plot("Test plot")
                .poster("http://example.com/poster.jpg")
                .genres(Arrays.asList("Action", "Drama"))
                .imdbRating(8.5)
                .recentComments(Arrays.asList(comment))
                .totalComments(5)
                .mostRecentCommentDate(new Date())
                .build();

        when(movieService.getMoviesWithMostRecentComments(anyInt(), isNull())).thenReturn(Arrays.asList(result));

        // Act & Assert
        mockMvc.perform(get("/api/movies/aggregations/reportingByComments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].title").value("Test Movie"))
                .andExpect(jsonPath("$.data[0].year").value(2024))
                .andExpect(jsonPath("$.data[0].totalComments").value(5))
                .andExpect(jsonPath("$.data[0].recentComments", hasSize(1)))
                .andExpect(jsonPath("$.data[0].recentComments[0].name").value("John Doe"));
    }

    @Test
    @DisplayName("GET /api/movies/aggregations/reportingByComments - Should accept limit parameter")
    void testGetMoviesWithMostComments_WithLimit() throws Exception {
        // Arrange
        when(movieService.getMoviesWithMostRecentComments(eq(5), isNull())).thenReturn(Arrays.asList());

        // Act & Assert
        mockMvc.perform(get("/api/movies/aggregations/reportingByComments")
                        .param("limit", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @DisplayName("GET /api/movies/aggregations/reportingByComments - Should accept movieId parameter")
    void testGetMoviesWithMostComments_WithMovieId() throws Exception {
        // Arrange
        String movieId = testId.toHexString();
        when(movieService.getMoviesWithMostRecentComments(anyInt(), eq(movieId))).thenReturn(Arrays.asList());

        // Act & Assert
        mockMvc.perform(get("/api/movies/aggregations/reportingByComments")
                        .param("movieId", movieId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @DisplayName("GET /api/movies/aggregations/reportingByComments - Should return 400 for invalid movieId")
    void testGetMoviesWithMostComments_InvalidMovieId() throws Exception {
        // Arrange
        String invalidMovieId = "invalid-id";
        when(movieService.getMoviesWithMostRecentComments(anyInt(), eq(invalidMovieId)))
                .thenThrow(new ValidationException("Invalid movie ID format"));

        // Act & Assert
        mockMvc.perform(get("/api/movies/aggregations/reportingByComments")
                        .param("movieId", invalidMovieId))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }

    @Test
    @DisplayName("GET /api/movies/aggregations/reportingByYear - Should return movies by year with statistics")
    void testGetMoviesByYearWithStats_Success() throws Exception {
        // Arrange
        MoviesByYearResult result1 = MoviesByYearResult.builder()
                .year(2024)
                .movieCount(10)
                .averageRating(7.5)
                .highestRating(9.0)
                .lowestRating(6.0)
                .totalVotes(5000L)
                .build();

        MoviesByYearResult result2 = MoviesByYearResult.builder()
                .year(2023)
                .movieCount(15)
                .averageRating(7.8)
                .highestRating(9.5)
                .lowestRating(6.5)
                .totalVotes(7500L)
                .build();

        when(movieService.getMoviesByYearWithStats()).thenReturn(Arrays.asList(result1, result2));

        // Act & Assert
        mockMvc.perform(get("/api/movies/aggregations/reportingByYear"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data", hasSize(2)))
                .andExpect(jsonPath("$.data[0].year").value(2024))
                .andExpect(jsonPath("$.data[0].movieCount").value(10))
                .andExpect(jsonPath("$.data[0].averageRating").value(7.5))
                .andExpect(jsonPath("$.data[1].year").value(2023))
                .andExpect(jsonPath("$.data[1].movieCount").value(15));
    }

    @Test
    @DisplayName("GET /api/movies/aggregations/reportingByDirectors - Should return directors with most movies")
    void testGetDirectorsWithMostMovies_Success() throws Exception {
        // Arrange
        DirectorStatisticsResult result1 = DirectorStatisticsResult.builder()
                .director("Christopher Nolan")
                .movieCount(10)
                .averageRating(8.5)
                .build();

        DirectorStatisticsResult result2 = DirectorStatisticsResult.builder()
                .director("Steven Spielberg")
                .movieCount(25)
                .averageRating(8.2)
                .build();

        when(movieService.getDirectorsWithMostMovies(anyInt())).thenReturn(Arrays.asList(result1, result2));

        // Act & Assert
        mockMvc.perform(get("/api/movies/aggregations/reportingByDirectors"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data", hasSize(2)))
                .andExpect(jsonPath("$.data[0].director").value("Christopher Nolan"))
                .andExpect(jsonPath("$.data[0].movieCount").value(10))
                .andExpect(jsonPath("$.data[0].averageRating").value(8.5))
                .andExpect(jsonPath("$.data[1].director").value("Steven Spielberg"))
                .andExpect(jsonPath("$.data[1].movieCount").value(25));
    }

    @Test
    @DisplayName("GET /api/movies/aggregations/reportingByDirectors - Should accept limit parameter")
    void testGetDirectorsWithMostMovies_WithLimit() throws Exception {
        // Arrange
        when(movieService.getDirectorsWithMostMovies(eq(10))).thenReturn(Arrays.asList());

        // Act & Assert
        mockMvc.perform(get("/api/movies/aggregations/reportingByDirectors")
                        .param("limit", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray());
    }

    // ==================== MongoDB SEARCH ENDPOINT TESTS ====================

    @Test
    @DisplayName("GET /api/movies/search - Should search movies by plot successfully")
    void testSearchMoviesByPlot_Success() throws Exception {
        // Arrange
        Movie movie1 = Movie.builder()
                .id(new ObjectId())
                .title("Space Adventure")
                .year(2024)
                .plot("An epic space adventure across the galaxy")
                .genres(Arrays.asList("Sci-Fi", "Adventure"))
                .build();

        Movie movie2 = Movie.builder()
                .id(new ObjectId())
                .title("Space Quest")
                .year(2023)
                .plot("A thrilling space adventure to save humanity")
                .genres(Arrays.asList("Sci-Fi", "Action"))
                .build();

        when(movieService.searchMovies(any(com.mongodb.samplemflix.model.dto.MovieSearchRequest.class)))
                .thenReturn(Arrays.asList(movie1, movie2));

        // Act & Assert
        mockMvc.perform(get("/api/movies/search")
                        .param("plot", "space adventure"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.movies").isArray())
                .andExpect(jsonPath("$.data.movies", hasSize(2)))
                .andExpect(jsonPath("$.data.totalCount").value(2))
                .andExpect(jsonPath("$.data.movies[0].title").value("Space Adventure"))
                .andExpect(jsonPath("$.data.movies[0].plot").value(containsString("space adventure")))
                .andExpect(jsonPath("$.data.movies[1].title").value("Space Quest"));
    }

    @Test
    @DisplayName("GET /api/movies/search - Should accept limit and skip parameters")
    void testSearchMoviesByPlot_WithPagination() throws Exception {
        // Arrange
        when(movieService.searchMovies(any(com.mongodb.samplemflix.model.dto.MovieSearchRequest.class)))
                .thenReturn(Arrays.asList());

        // Act & Assert
        mockMvc.perform(get("/api/movies/search")
                        .param("plot", "adventure")
                        .param("limit", "10")
                        .param("skip", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.movies").isArray())
                .andExpect(jsonPath("$.data.totalCount").value(0));
    }

    @Test
    @DisplayName("GET /api/movies/search - Should return 400 when no search parameters provided")
    void testSearchMoviesByPlot_MissingPlotParameter() throws Exception {
        // Arrange
        when(movieService.searchMovies(any(com.mongodb.samplemflix.model.dto.MovieSearchRequest.class)))
                .thenThrow(new ValidationException("At least one search parameter must be provided"));

        // Act & Assert
        mockMvc.perform(get("/api/movies/search"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }

    @Test
    @DisplayName("GET /api/movies/search - Should return 400 for validation error")
    void testSearchMoviesByPlot_ValidationError() throws Exception {
        // Arrange
        when(movieService.searchMovies(any(com.mongodb.samplemflix.model.dto.MovieSearchRequest.class)))
                .thenThrow(new ValidationException("Plot query cannot be empty"));

        // Act & Assert
        mockMvc.perform(get("/api/movies/search")
                        .param("plot", ""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }

    @Test
    @DisplayName("GET /api/movies/search - Should return empty list when no matches found")
    void testSearchMoviesByPlot_NoResults() throws Exception {
        // Arrange
        when(movieService.searchMovies(any(com.mongodb.samplemflix.model.dto.MovieSearchRequest.class)))
                .thenReturn(Arrays.asList());

        // Act & Assert
        mockMvc.perform(get("/api/movies/search")
                        .param("plot", "nonexistent"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.movies").isArray())
                .andExpect(jsonPath("$.data.movies", hasSize(0)))
                .andExpect(jsonPath("$.data.totalCount").value(0));
    }

    @Test
    @DisplayName("GET /api/movies/search - Should search by multiple fields")
    void testSearchMovies_MultipleFields() throws Exception {
        // Arrange
        Movie movie = Movie.builder()
                .id(new ObjectId())
                .title("The Godfather")
                .year(1972)
                .plot("The aging patriarch of an organized crime dynasty transfers control to his son")
                .directors(Arrays.asList("Francis Ford Coppola"))
                .cast(Arrays.asList("Marlon Brando", "Al Pacino"))
                .build();

        when(movieService.searchMovies(any(com.mongodb.samplemflix.model.dto.MovieSearchRequest.class)))
                .thenReturn(Arrays.asList(movie));

        // Act & Assert
        mockMvc.perform(get("/api/movies/search")
                        .param("directors", "Coppola")
                        .param("cast", "Pacino"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.movies").isArray())
                .andExpect(jsonPath("$.data.movies", hasSize(1)))
                .andExpect(jsonPath("$.data.totalCount").value(1))
                .andExpect(jsonPath("$.data.movies[0].title").value("The Godfather"));
    }

    @Test
    @DisplayName("GET /api/movies/search - Should accept searchOperator parameter")
    void testSearchMovies_WithSearchOperator() throws Exception {
        // Arrange
        when(movieService.searchMovies(any(com.mongodb.samplemflix.model.dto.MovieSearchRequest.class)))
                .thenReturn(Arrays.asList());

        // Act & Assert
        mockMvc.perform(get("/api/movies/search")
                        .param("plot", "adventure")
                        .param("searchOperator", "should"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.movies").isArray())
                .andExpect(jsonPath("$.data.totalCount").value(0));
    }

    @Test
    @DisplayName("GET /api/movies/search - Should return 400 for invalid searchOperator")
    void testSearchMovies_InvalidSearchOperator() throws Exception {
        // Arrange
        when(movieService.searchMovies(any(com.mongodb.samplemflix.model.dto.MovieSearchRequest.class)))
                .thenThrow(new ValidationException("Invalid search_operator 'invalid'. The search_operator must be one of: must, should, mustNot, filter"));

        // Act & Assert
        mockMvc.perform(get("/api/movies/search")
                        .param("plot", "adventure")
                        .param("searchOperator", "invalid"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }

    // ==================== VECTOR SEARCH ENDPOINT TESTS ====================

    @Test
    @DisplayName("GET /api/movies/vector-search - Should perform vector search successfully")
    void testVectorSearchMovies_Success() throws Exception {
        // Arrange
        VectorSearchResult result1 = VectorSearchResult.builder()
                .id(testId.toHexString())
                .title("Space Raiders")
                .plot("A futuristic space adventure")
                .score(0.85)
                .build();

        VectorSearchResult result2 = VectorSearchResult.builder()
                .id(new ObjectId().toHexString())
                .title("Galaxy Quest")
                .plot("An epic space journey")
                .score(0.78)
                .build();

        when(movieService.vectorSearchMovies(eq("space adventure"), eq(3)))
                .thenReturn(Arrays.asList(result1, result2));

        // Act & Assert
        mockMvc.perform(get("/api/movies/vector-search")
                        .param("q", "space adventure")
                        .param("limit", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data", hasSize(2)))
                .andExpect(jsonPath("$.data[0].title").value("Space Raiders"))
                .andExpect(jsonPath("$.data[0].plot").value("A futuristic space adventure"))
                .andExpect(jsonPath("$.data[0].score").value(0.85))
                .andExpect(jsonPath("$.data[1].title").value("Galaxy Quest"))
                .andExpect(jsonPath("$.data[1].score").value(0.78));
    }

    @Test
    @DisplayName("GET /api/movies/vector-search - Should use default limit")
    void testVectorSearchMovies_DefaultLimit() throws Exception {
        // Arrange
        when(movieService.vectorSearchMovies(eq("adventure"), eq(10)))
                .thenReturn(Arrays.asList());

        // Act & Assert
        mockMvc.perform(get("/api/movies/vector-search")
                        .param("q", "adventure"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @DisplayName("GET /api/movies/vector-search - Should return 400 when query is missing")
    void testVectorSearchMovies_MissingQuery() throws Exception {
        // Arrange
        when(movieService.vectorSearchMovies(isNull(), anyInt()))
                .thenThrow(new ValidationException("Search query is required"));

        // Act & Assert
        mockMvc.perform(get("/api/movies/vector-search"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }

    @Test
    @DisplayName("GET /api/movies/vector-search - Should return 400 when API key is missing")
    void testVectorSearchMovies_MissingApiKey() throws Exception {
        // Arrange
        when(movieService.vectorSearchMovies(eq("test"), anyInt()))
                .thenThrow(new ValidationException("Vector search unavailable: VOYAGE_API_KEY not configured"));

        // Act & Assert
        mockMvc.perform(get("/api/movies/vector-search")
                        .param("q", "test"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.error.message").value(containsString("VOYAGE_API_KEY")));
    }

    @Test
    @DisplayName("GET /api/movies/vector-search - Should return empty list when no results")
    void testVectorSearchMovies_NoResults() throws Exception {
        // Arrange
        when(movieService.vectorSearchMovies(eq("nonexistent"), anyInt()))
                .thenReturn(Arrays.asList());

        // Act & Assert
        mockMvc.perform(get("/api/movies/vector-search")
                        .param("q", "nonexistent"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data", hasSize(0)));
    }

    // ==================== FIND SIMILAR MOVIES ENDPOINT TESTS ====================

    @Test
    @DisplayName("GET /api/movies/find-similar-movies - Should find similar movies successfully")
    void testFindSimilarMovies_Success() throws Exception {
        // Arrange
        String movieId = testId.toHexString();

        Movie similarMovie1 = Movie.builder()
                .id(new ObjectId())
                .title("Similar Movie 1")
                .year(2024)
                .plot("A similar plot")
                .build();

        Movie similarMovie2 = Movie.builder()
                .id(new ObjectId())
                .title("Similar Movie 2")
                .year(2023)
                .plot("Another similar plot")
                .build();

        when(movieService.findSimilarMovies(eq(movieId), eq(5)))
                .thenReturn(Arrays.asList(similarMovie1, similarMovie2));

        // Act & Assert
        mockMvc.perform(get("/api/movies/find-similar-movies")
                        .param("movieId", movieId)
                        .param("limit", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data", hasSize(2)))
                .andExpect(jsonPath("$.data[0].title").value("Similar Movie 1"))
                .andExpect(jsonPath("$.data[1].title").value("Similar Movie 2"));
    }

    @Test
    @DisplayName("GET /api/movies/find-similar-movies - Should use default limit")
    void testFindSimilarMovies_DefaultLimit() throws Exception {
        // Arrange
        String movieId = testId.toHexString();
        when(movieService.findSimilarMovies(eq(movieId), eq(10)))
                .thenReturn(Arrays.asList());

        // Act & Assert
        mockMvc.perform(get("/api/movies/find-similar-movies")
                        .param("movieId", movieId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @DisplayName("GET /api/movies/find-similar-movies - Should return 400 for invalid movie ID")
    void testFindSimilarMovies_InvalidId() throws Exception {
        // Arrange
        String invalidId = "invalid-id";
        when(movieService.findSimilarMovies(eq(invalidId), anyInt()))
                .thenThrow(new ValidationException("Invalid movie ID format"));

        // Act & Assert
        mockMvc.perform(get("/api/movies/find-similar-movies")
                        .param("movieId", invalidId))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }

    @Test
    @DisplayName("GET /api/movies/find-similar-movies - Should return 404 when movie not found")
    void testFindSimilarMovies_MovieNotFound() throws Exception {
        // Arrange
        String movieId = testId.toHexString();
        when(movieService.findSimilarMovies(eq(movieId), anyInt()))
                .thenThrow(new ResourceNotFoundException("Movie not found"));

        // Act & Assert
        mockMvc.perform(get("/api/movies/find-similar-movies")
                        .param("movieId", movieId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("RESOURCE_NOT_FOUND"));
    }

    // ==================== BATCH UPDATE ENDPOINT TESTS ====================

    @Test
    @DisplayName("PATCH /api/movies/batch - Should update movies batch successfully")
    void testUpdateMoviesBatch_Success() throws Exception {
        // Arrange
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("filter", Map.of("year", 2024));
        requestBody.put("update", Map.of("$set", Map.of("rating", "PG-13")));

        BatchUpdateResponse response = new BatchUpdateResponse(5L, 5L);
        when(movieService.updateMoviesBatch(any(Document.class), any(Document.class)))
                .thenReturn(response);

        // Act & Assert
        mockMvc.perform(patch("/api/movies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.matchedCount").value(5))
                .andExpect(jsonPath("$.data.modifiedCount").value(5));
    }

    @Test
    @DisplayName("PATCH /api/movies - Should handle empty filter")
    void testUpdateMoviesBatch_EmptyFilter() throws Exception {
        // Arrange
        BatchUpdateResponse updateResponse = new BatchUpdateResponse(0L, 0L);

        when(movieService.updateMoviesBatch(any(Document.class), any(Document.class)))
                .thenReturn(updateResponse);

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("filter", Map.of());
        requestBody.put("update", Map.of("$set", Map.of("rating", "PG-13")));

        // Act & Assert
        mockMvc.perform(patch("/api/movies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.matchedCount").value(0))
                .andExpect(jsonPath("$.data.modifiedCount").value(0));
    }

    // ==================== BATCH DELETE ENDPOINT TESTS ====================

    @Test
    @DisplayName("DELETE /api/movies/batch - Should delete movies batch successfully")
    void testDeleteMoviesBatch_Success() throws Exception {
        // Arrange
        Map<String, Object> requestBody = Map.of("filter", Map.of("year", Map.of("$lt", 1950)));

        DeleteResponse response = new DeleteResponse(10L);
        when(movieService.deleteMoviesBatch(any(Document.class)))
                .thenReturn(response);

        // Act & Assert
        mockMvc.perform(delete("/api/movies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.deletedCount").value(10));
    }

    @Test
    @DisplayName("DELETE /api/movies - Should handle empty filter")
    void testDeleteMoviesBatch_EmptyFilter() throws Exception {
        // Arrange
        DeleteResponse deleteResponse = new DeleteResponse(0L);

        when(movieService.deleteMoviesBatch(any(Document.class)))
                .thenReturn(deleteResponse);

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("filter", Map.of());

        // Act & Assert
        mockMvc.perform(delete("/api/movies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.deletedCount").value(0));
    }
}
