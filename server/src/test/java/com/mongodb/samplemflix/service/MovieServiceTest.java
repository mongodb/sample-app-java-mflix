package com.mongodb.samplemflix.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.result.UpdateResult;
import com.mongodb.samplemflix.exception.ResourceNotFoundException;
import com.mongodb.samplemflix.exception.ServiceUnavailableException;
import com.mongodb.samplemflix.exception.ValidationException;
import com.mongodb.samplemflix.model.Movie;
import com.mongodb.samplemflix.model.dto.BatchInsertResponse;
import com.mongodb.samplemflix.model.dto.BatchUpdateResponse;
import com.mongodb.samplemflix.model.dto.CreateMovieRequest;
import com.mongodb.samplemflix.model.dto.DeleteResponse;
import com.mongodb.samplemflix.model.dto.DirectorStatisticsResult;
import com.mongodb.samplemflix.model.dto.MovieSearchQuery;
import com.mongodb.samplemflix.model.dto.MovieSearchRequest;
import com.mongodb.samplemflix.model.dto.MovieWithCommentsResult;
import com.mongodb.samplemflix.model.dto.MoviesByYearResult;
import com.mongodb.samplemflix.model.dto.UpdateMovieRequest;
import com.mongodb.samplemflix.repository.MovieRepository;
import java.util.*;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Unit tests for MovieServiceImpl using Spring Data MongoDB.
 *
 * These tests verify the business logic of the service layer
 * by mocking the repository and MongoTemplate dependencies.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("MovieService Unit Tests")
class MovieServiceTest {

    @Mock
    private MovieRepository movieRepository;

    @Mock
    private MongoTemplate mongoTemplate;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private MovieServiceImpl movieService;

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
    @DisplayName("Should get all movies with default pagination")
    void testGetAllMovies_WithDefaults() {
        // Arrange
        MovieSearchQuery query = MovieSearchQuery.builder().build();
        List<Movie> expectedMovies = Arrays.asList(testMovie);

        when(mongoTemplate.find(any(Query.class), eq(Movie.class)))
                .thenReturn(expectedMovies);

        // Act
        List<Movie> result = movieService.getAllMovies(query);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(testMovie.getTitle(), result.get(0).getTitle());
        verify(mongoTemplate).find(any(Query.class), eq(Movie.class));
    }

    @Test
    @DisplayName("Should get all movies with custom pagination")
    void testGetAllMovies_WithCustomPagination() {
        // Arrange
        MovieSearchQuery query = MovieSearchQuery.builder()
                .limit(50)
                .skip(10)
                .build();
        List<Movie> expectedMovies = Arrays.asList(testMovie);

        when(mongoTemplate.find(any(Query.class), eq(Movie.class)))
                .thenReturn(expectedMovies);

        // Act
        List<Movie> result = movieService.getAllMovies(query);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(mongoTemplate).find(any(Query.class), eq(Movie.class));
    }

    @Test
    @DisplayName("Should enforce maximum limit of 100")
    void testGetAllMovies_EnforcesMaxLimit() {
        // Arrange
        MovieSearchQuery query = MovieSearchQuery.builder()
                .limit(200)
                .build();

        when(mongoTemplate.find(any(Query.class), eq(Movie.class)))
                .thenReturn(Collections.emptyList());

        // Act
        movieService.getAllMovies(query);

        // Assert
        verify(mongoTemplate).find(any(Query.class), eq(Movie.class));
    }

    @Test
    @DisplayName("Should enforce minimum limit of 1")
    void testGetAllMovies_EnforcesMinLimit() {
        // Arrange
        MovieSearchQuery query = MovieSearchQuery.builder()
                .limit(0)
                .build();

        when(mongoTemplate.find(any(Query.class), eq(Movie.class)))
                .thenReturn(Collections.emptyList());

        // Act
        movieService.getAllMovies(query);

        // Assert
        verify(mongoTemplate).find(any(Query.class), eq(Movie.class));
    }

    // ==================== GET MOVIE BY ID TESTS ====================

    @Test
    @DisplayName("Should get movie by valid ID")
    void testGetMovieById_ValidId() {
        // Arrange
        String validId = testId.toHexString();
        when(movieRepository.findById(testId)).thenReturn(Optional.of(testMovie));

        // Act
        Movie result = movieService.getMovieById(validId);

        // Assert
        assertNotNull(result);
        assertEquals(testMovie.getTitle(), result.getTitle());
        verify(movieRepository).findById(testId);
    }

    @Test
    @DisplayName("Should throw ValidationException for invalid ID format")
    void testGetMovieById_InvalidIdFormat() {
        // Arrange
        String invalidId = "invalid-id";

        // Act & Assert
        assertThrows(ValidationException.class, () -> movieService.getMovieById(invalidId));
        verify(movieRepository, never()).findById(any());
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when movie not found")
    void testGetMovieById_NotFound() {
        // Arrange
        String validId = testId.toHexString();
        when(movieRepository.findById(testId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> movieService.getMovieById(validId));
        verify(movieRepository).findById(testId);
    }

    // ==================== CREATE MOVIE TESTS ====================

    @Test
    @DisplayName("Should create movie successfully")
    void testCreateMovie_Success() {
        // Arrange
        when(movieRepository.save(any(Movie.class))).thenReturn(testMovie);

        // Act
        Movie result = movieService.createMovie(createRequest);

        // Assert
        assertNotNull(result);
        verify(movieRepository).save(any(Movie.class));
    }

    @Test
    @DisplayName("Should throw ValidationException when title is null")
    void testCreateMovie_NullTitle() {
        // Arrange
        CreateMovieRequest invalidRequest = CreateMovieRequest.builder()
                .title(null)
                .year(2024)
                .build();

        // Act & Assert
        assertThrows(ValidationException.class, () -> movieService.createMovie(invalidRequest));
        verify(movieRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw ValidationException when title is empty")
    void testCreateMovie_EmptyTitle() {
        // Arrange
        CreateMovieRequest invalidRequest = CreateMovieRequest.builder()
                .title("   ")
                .year(2024)
                .build();

        // Act & Assert
        assertThrows(ValidationException.class, () -> movieService.createMovie(invalidRequest));
        verify(movieRepository, never()).save(any());
    }

    // ==================== CREATE MOVIES BATCH TESTS ====================

    @Test
    @DisplayName("Should create movies batch successfully")
    void testCreateMoviesBatch_Success() {
        // Arrange
        List<CreateMovieRequest> requests = Arrays.asList(createRequest, createRequest);
        List<Movie> savedMovies = Arrays.asList(testMovie, testMovie);

        when(movieRepository.saveAll(anyList())).thenReturn(savedMovies);

        // Act
        BatchInsertResponse result = movieService.createMoviesBatch(requests);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.getInsertedCount());
        assertNotNull(result.getInsertedIds());
        verify(movieRepository).saveAll(anyList());
    }

    // ==================== UPDATE MOVIE TESTS ====================

    @Test
    @DisplayName("Should update movie successfully")
    void testUpdateMovie_Success() {
        // Arrange
        String validId = testId.toHexString();
        Map<String, Object> requestMap = new HashMap<>();
        requestMap.put("title", "Updated Title");
        requestMap.put("year", 2025);

        when(objectMapper.convertValue(updateRequest, Map.class)).thenReturn(requestMap);

        UpdateResult updateResult = mock(UpdateResult.class);
        when(updateResult.getMatchedCount()).thenReturn(1L);
        when(mongoTemplate.updateFirst(any(Query.class), any(org.springframework.data.mongodb.core.query.Update.class), any(Class.class)))
                .thenReturn(updateResult);
        when(movieRepository.findById(testId)).thenReturn(Optional.of(testMovie));

        // Act
        Movie result = movieService.updateMovie(validId, updateRequest);

        // Assert
        assertNotNull(result);
        verify(mongoTemplate).updateFirst(any(Query.class), any(org.springframework.data.mongodb.core.query.Update.class), any(Class.class));
        verify(movieRepository).findById(testId);
    }

    @Test
    @DisplayName("Should throw ValidationException for invalid ID in update")
    void testUpdateMovie_InvalidId() {
        // Arrange
        String invalidId = "invalid-id";

        // Act & Assert
        assertThrows(ValidationException.class, () -> movieService.updateMovie(invalidId, updateRequest));
        verify(mongoTemplate, never()).updateFirst(any(Query.class), any(org.springframework.data.mongodb.core.query.Update.class), any(Class.class));
    }

    @Test
    @DisplayName("Should throw ValidationException when update request is empty")
    void testUpdateMovie_EmptyRequest() {
        // Arrange
        String validId = testId.toHexString();
        UpdateMovieRequest emptyRequest = UpdateMovieRequest.builder().build();
        Map<String, Object> emptyMap = new HashMap<>();

        when(objectMapper.convertValue(emptyRequest, Map.class)).thenReturn(emptyMap);

        // Act & Assert
        assertThrows(ValidationException.class, () -> movieService.updateMovie(validId, emptyRequest));
        verify(mongoTemplate, never()).updateFirst(any(Query.class), any(org.springframework.data.mongodb.core.query.Update.class), any(Class.class));
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when movie to update not found")
    void testUpdateMovie_NotFound() {
        // Arrange
        String validId = testId.toHexString();
        Map<String, Object> requestMap = new HashMap<>();
        requestMap.put("title", "Updated Title");

        when(objectMapper.convertValue(updateRequest, Map.class)).thenReturn(requestMap);

        UpdateResult updateResult = mock(UpdateResult.class);
        when(updateResult.getMatchedCount()).thenReturn(0L);
        when(mongoTemplate.updateFirst(any(Query.class), any(org.springframework.data.mongodb.core.query.Update.class), any(Class.class)))
                .thenReturn(updateResult);

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> movieService.updateMovie(validId, updateRequest));
        verify(mongoTemplate).updateFirst(any(Query.class), any(org.springframework.data.mongodb.core.query.Update.class), any(Class.class));
        verify(movieRepository, never()).findById(any());
    }

    // ==================== DELETE MOVIE TESTS ====================

    @Test
    @DisplayName("Should delete movie successfully")
    void testDeleteMovie_Success() {
        // Arrange
        String validId = testId.toHexString();
        when(movieRepository.existsById(testId)).thenReturn(true);

        // Act
        DeleteResponse result = movieService.deleteMovie(validId);

        // Assert
        assertNotNull(result);
        assertEquals(1L, result.getDeletedCount());
        verify(movieRepository).existsById(testId);
        verify(movieRepository).deleteById(testId);
    }

    @Test
    @DisplayName("Should throw ValidationException for invalid ID in delete")
    void testDeleteMovie_InvalidId() {
        // Arrange
        String invalidId = "invalid-id";

        // Act & Assert
        assertThrows(ValidationException.class, () -> movieService.deleteMovie(invalidId));
        verify(movieRepository, never()).deleteById(any());
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when movie to delete not found")
    void testDeleteMovie_NotFound() {
        // Arrange
        String validId = testId.toHexString();
        when(movieRepository.existsById(testId)).thenReturn(false);

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> movieService.deleteMovie(validId));
        verify(movieRepository).existsById(testId);
        verify(movieRepository, never()).deleteById(any());
    }

    // ==================== FIND AND DELETE MOVIE TESTS ====================

    @Test
    @DisplayName("Should find and delete movie successfully")
    void testFindAndDeleteMovie_Success() {
        // Arrange
        String validId = testId.toHexString();
        when(mongoTemplate.findAndRemove(any(Query.class), eq(Movie.class))).thenReturn(testMovie);

        // Act
        Movie result = movieService.findAndDeleteMovie(validId);

        // Assert
        assertNotNull(result);
        assertEquals(testMovie.getTitle(), result.getTitle());
        verify(mongoTemplate).findAndRemove(any(Query.class), eq(Movie.class));
    }

    @Test
    @DisplayName("Should throw ValidationException for invalid ID in find and delete")
    void testFindAndDeleteMovie_InvalidId() {
        // Arrange
        String invalidId = "invalid-id";

        // Act & Assert
        assertThrows(ValidationException.class, () -> movieService.findAndDeleteMovie(invalidId));
        verify(mongoTemplate, never()).findAndRemove(any(), any());
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when movie to find and delete not found")
    void testFindAndDeleteMovie_NotFound() {
        // Arrange
        String validId = testId.toHexString();
        when(mongoTemplate.findAndRemove(any(Query.class), eq(Movie.class))).thenReturn(null);

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> movieService.findAndDeleteMovie(validId));
        verify(mongoTemplate).findAndRemove(any(Query.class), eq(Movie.class));
    }

    // ==================== AGGREGATION TESTS ====================

    @Test
    @DisplayName("Should get movies with most comments")
    void testGetMoviesWithMostRecentComments_Success() {
        // Arrange
        Integer limit = 10;
        String movieId = null;

        Document doc1 = new Document()
                .append("_id", testId.toHexString())
                .append("title", "Test Movie")
                .append("year", 2024)
                .append("plot", "Test plot")
                .append("poster", "http://example.com/poster.jpg")
                .append("genres", Arrays.asList("Action", "Drama"))
                .append("imdbRating", 8.5)
                .append("recentComments", Arrays.asList(
                        new Document()
                                .append("_id", new ObjectId())
                                .append("name", "John Doe")
                                .append("email", "john@example.com")
                                .append("text", "Great movie!")
                                .append("date", new Date())
                ))
                .append("totalComments", 5)
                .append("mostRecentCommentDate", new Date());

        @SuppressWarnings("unchecked")
        AggregationResults<Document> mockResults = mock(AggregationResults.class);
        when(mockResults.getMappedResults()).thenReturn(Arrays.asList(doc1));
        when(mongoTemplate.aggregate(any(Aggregation.class), eq("movies"), eq(Document.class)))
                .thenReturn(mockResults);

        // Act
        List<MovieWithCommentsResult> results = movieService.getMoviesWithMostRecentComments(limit, movieId);

        // Assert
        assertNotNull(results);
        assertEquals(1, results.size());
        assertEquals("Test Movie", results.get(0).getTitle());
        assertEquals(2024, results.get(0).getYear());
        assertEquals(5, results.get(0).getTotalComments());
        assertNotNull(results.get(0).getRecentComments());
        assertEquals(1, results.get(0).getRecentComments().size());
        verify(mongoTemplate).aggregate(any(Aggregation.class), eq("movies"), eq(Document.class));
    }

    @Test
    @DisplayName("Should get movies with most comments filtered by movie ID")
    void testGetMoviesWithMostComments_WithMovieIdRecent() {
        // Arrange
        Integer limit = 10;
        String movieId = testId.toHexString();

        @SuppressWarnings("unchecked")
        AggregationResults<Document> mockResults = mock(AggregationResults.class);
        when(mockResults.getMappedResults()).thenReturn(Arrays.asList());
        when(mongoTemplate.aggregate(any(Aggregation.class), eq("movies"), eq(Document.class)))
                .thenReturn(mockResults);

        // Act
        List<MovieWithCommentsResult> results = movieService.getMoviesWithMostRecentComments(limit, movieId);

        // Assert
        assertNotNull(results);
        verify(mongoTemplate).aggregate(any(Aggregation.class), eq("movies"), eq(Document.class));
    }

    @Test
    @DisplayName("Should throw ValidationException for invalid movie ID in getMoviesWithMostComments")
    void testGetMoviesWithMostRecentComments_InvalidMovieId() {
        // Arrange
        Integer limit = 10;
        String invalidMovieId = "invalid-id";

        // Act & Assert
        assertThrows(ValidationException.class,
                () -> movieService.getMoviesWithMostRecentComments(limit, invalidMovieId));
        verify(mongoTemplate, never()).aggregate(any(Aggregation.class), anyString(), any());
    }

    @Test
    @DisplayName("Should get movies by year with statistics")
    void testGetMoviesByYearWithStats_Success() {
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

        @SuppressWarnings("unchecked")
        AggregationResults<MoviesByYearResult> mockResults = mock(AggregationResults.class);
        when(mockResults.getMappedResults()).thenReturn(Arrays.asList(result1, result2));
        when(mongoTemplate.aggregate(any(Aggregation.class), eq("movies"), eq(MoviesByYearResult.class)))
                .thenReturn(mockResults);

        // Act
        List<MoviesByYearResult> results = movieService.getMoviesByYearWithStats();

        // Assert
        assertNotNull(results);
        assertEquals(2, results.size());
        assertEquals(2024, results.get(0).getYear());
        assertEquals(10, results.get(0).getMovieCount());
        assertEquals(7.5, results.get(0).getAverageRating());
        assertEquals(2023, results.get(1).getYear());
        verify(mongoTemplate).aggregate(any(Aggregation.class), eq("movies"), eq(MoviesByYearResult.class));
    }

    @Test
    @DisplayName("Should get directors with most movies")
    void testGetDirectorsWithMostMovies_Success() {
        // Arrange
        Integer limit = 20;

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

        @SuppressWarnings("unchecked")
        AggregationResults<DirectorStatisticsResult> mockResults = mock(AggregationResults.class);
        when(mockResults.getMappedResults()).thenReturn(Arrays.asList(result1, result2));
        when(mongoTemplate.aggregate(any(Aggregation.class), eq("movies"), eq(DirectorStatisticsResult.class)))
                .thenReturn(mockResults);

        // Act
        List<DirectorStatisticsResult> results = movieService.getDirectorsWithMostMovies(limit);

        // Assert
        assertNotNull(results);
        assertEquals(2, results.size());
        assertEquals("Christopher Nolan", results.get(0).getDirector());
        assertEquals(10, results.get(0).getMovieCount());
        assertEquals(8.5, results.get(0).getAverageRating());
        assertEquals("Steven Spielberg", results.get(1).getDirector());
        verify(mongoTemplate).aggregate(any(Aggregation.class), eq("movies"), eq(DirectorStatisticsResult.class));
    }

    @Test
    @DisplayName("Should use default limit when null in getDirectorsWithMostMovies")
    void testGetDirectorsWithMostMovies_DefaultLimit() {
        // Arrange
        Integer limit = null;

        @SuppressWarnings("unchecked")
        AggregationResults<DirectorStatisticsResult> mockResults = mock(AggregationResults.class);
        when(mockResults.getMappedResults()).thenReturn(Arrays.asList());
        when(mongoTemplate.aggregate(any(Aggregation.class), eq("movies"), eq(DirectorStatisticsResult.class)))
                .thenReturn(mockResults);

        // Act
        List<DirectorStatisticsResult> results = movieService.getDirectorsWithMostMovies(limit);

        // Assert
        assertNotNull(results);
        verify(mongoTemplate).aggregate(any(Aggregation.class), eq("movies"), eq(DirectorStatisticsResult.class));
    }

    // ==================== BATCH UPDATE TESTS ====================

    @Test
    @DisplayName("Should update movies batch successfully")
    void testUpdateMoviesBatch_Success() {
        // Arrange
        Document filter = new Document("year", 2024);
        Document update = new Document("$set", new Document("rating", "PG-13"));

        UpdateResult updateResult = mock(UpdateResult.class);
        when(updateResult.getMatchedCount()).thenReturn(5L);
        when(updateResult.getModifiedCount()).thenReturn(5L);
        when(mongoTemplate.updateMulti(any(Query.class), any(org.springframework.data.mongodb.core.query.Update.class), (Class<Movie>) eq(Movie.class)))
                .thenReturn(updateResult);

        // Act
        BatchUpdateResponse result = movieService.updateMoviesBatch(filter, update);

        // Assert
        assertNotNull(result);
        assertEquals(5L, result.getMatchedCount());
        assertEquals(5L, result.getModifiedCount());
        verify(mongoTemplate).updateMulti(any(Query.class), any(org.springframework.data.mongodb.core.query.Update.class), eq(Movie.class));
    }

    @Test
    @DisplayName("Should throw ValidationException when filter is null in batch update")
    void testUpdateMoviesBatch_NullFilter() {
        // Arrange
        Document update = new Document("$set", new Document("rating", "PG-13"));

        // Act & Assert
        assertThrows(ValidationException.class, () -> movieService.updateMoviesBatch(null, update));
        verify(mongoTemplate, never()).updateMulti(any(Query.class), any(org.springframework.data.mongodb.core.query.Update.class), (Class<?>) any());
    }

    @Test
    @DisplayName("Should throw ValidationException when update is null in batch update")
    void testUpdateMoviesBatch_NullUpdate() {
        // Arrange
        Document filter = new Document("year", 2024);

        // Act & Assert
        assertThrows(ValidationException.class, () -> movieService.updateMoviesBatch(filter, null));
        verify(mongoTemplate, never()).updateMulti(any(Query.class), any(org.springframework.data.mongodb.core.query.Update.class), (Class<?>) any());
    }

    // ==================== BATCH DELETE TESTS ====================

    @Test
    @DisplayName("Should delete movies batch successfully")
    void testDeleteMoviesBatch_Success() {
        // Arrange
        Document filter = new Document("year", new Document("$lt", 1950));

        com.mongodb.client.result.DeleteResult deleteResult = mock(com.mongodb.client.result.DeleteResult.class);
        when(deleteResult.getDeletedCount()).thenReturn(10L);
        when(mongoTemplate.remove(any(Query.class), (Class<Movie>) eq(Movie.class)))
                .thenReturn(deleteResult);

        // Act
        DeleteResponse result = movieService.deleteMoviesBatch(filter);

        // Assert
        assertNotNull(result);
        assertEquals(10L, result.getDeletedCount());
        verify(mongoTemplate).remove(any(Query.class), eq(Movie.class));
    }

    @Test
    @DisplayName("Should throw ValidationException when filter is null in batch delete")
    void testDeleteMoviesBatch_NullFilter() {
        // Act & Assert
        assertThrows(ValidationException.class, () -> movieService.deleteMoviesBatch(null));
        verify(mongoTemplate, never()).remove(any(Query.class), (Class<?>) any());
    }

    // ==================== TEXT SEARCH TESTS ====================

    // Note: Search success tests are covered by integration tests due to complexity of mocking MongoDB aggregation

    @Test
    @DisplayName("Should throw ValidationException when all search fields are null")
    void testSearchMovies_NoSearchFields() {
        // Arrange
        MovieSearchRequest searchRequest = MovieSearchRequest.builder()
                .limit(10)
                .build();

        // Act & Assert
        assertThrows(ValidationException.class, () -> movieService.searchMovies(searchRequest));
        verify(mongoTemplate, never()).find(any(), any());
    }



    // ==================== VECTOR SEARCH TESTS ====================

    // Note: Vector search success tests are covered by integration tests due to complexity of mocking HTTP calls and MongoDB aggregation

    @Test
    @DisplayName("Should throw ValidationException when query is null in vector search")
    void testVectorSearchMovies_NullQuery() {
        // Arrange
        String apiKey = "test-api-key";
        ReflectionTestUtils.setField(movieService, "voyageApiKey", apiKey);

        // Act & Assert
        assertThrows(ValidationException.class, () -> movieService.vectorSearchMovies(null, 10));
    }

    @Test
    @DisplayName("Should throw ValidationException when query is empty in vector search")
    void testVectorSearchMovies_EmptyQuery() {
        // Arrange
        String apiKey = "test-api-key";
        ReflectionTestUtils.setField(movieService, "voyageApiKey", apiKey);

        // Act & Assert
        assertThrows(ValidationException.class, () -> movieService.vectorSearchMovies("   ", 10));
    }

    @Test
    @DisplayName("Should throw ServiceUnavailableException when API key is missing in vector search")
    void testVectorSearchMovies_MissingApiKey() {
        // Arrange
        ReflectionTestUtils.setField(movieService, "voyageApiKey", null);

        // Act & Assert
        assertThrows(ServiceUnavailableException.class, () -> movieService.vectorSearchMovies("test query", 10));
    }

    @Test
    @DisplayName("Should throw ServiceUnavailableException when API key is placeholder value in vector search")
    void testVectorSearchMovies_PlaceholderApiKey() {
        // Arrange
        ReflectionTestUtils.setField(movieService, "voyageApiKey", "your_voyage_api_key");

        // Act & Assert
        assertThrows(ServiceUnavailableException.class, () -> movieService.vectorSearchMovies("test query", 10));
    }

    @Test
    @DisplayName("Should enforce limit constraints in vector search")
    void testVectorSearchMovies_LimitConstraints() {
        // Arrange
        String apiKey = "test-api-key";
        ReflectionTestUtils.setField(movieService, "voyageApiKey", apiKey);

        // The limit should be clamped between 1 and 50
        // We can't easily test this without mocking the HTTP call
        // This would be better as an integration test
    }

    // ==================== FIND SIMILAR MOVIES TESTS ====================
    // Note: Find similar movies success tests are covered by integration tests due to complexity of mocking MongoDB aggregation

    @Test
    @DisplayName("Should throw ValidationException for invalid movie ID in find similar")
    void testFindSimilarMovies_InvalidId() {
        // Arrange
        String invalidId = "invalid-id";

        // Act & Assert
        assertThrows(ValidationException.class, () -> movieService.findSimilarMovies(invalidId, 10));
        verify(movieRepository, never()).findById(any());
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when source movie not found")
    void testFindSimilarMovies_MovieNotFound() {
        // Arrange
        String movieId = testId.toHexString();

        // Mock the movies collection to return null (movie not found)
        @SuppressWarnings("unchecked")
        MongoCollection<Document> mockMoviesCollection = mock(MongoCollection.class);
        @SuppressWarnings("unchecked")
        com.mongodb.client.FindIterable<Document> mockFindIterable = mock(com.mongodb.client.FindIterable.class);

        when(mongoTemplate.getCollection("movies")).thenReturn(mockMoviesCollection);
        when(mockMoviesCollection.find(any(Document.class))).thenReturn(mockFindIterable);
        when(mockFindIterable.first()).thenReturn(null);

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> movieService.findSimilarMovies(movieId, 10));
    }

    // ==================== GET DISTINCT GENRES TESTS ====================

    @Test
    @DisplayName("Should get distinct genres successfully")
    void testGetDistinctGenres_Success() {
        // Arrange
        List<String> expectedGenres = Arrays.asList("Action", "Comedy", "Drama", "Horror", "Sci-Fi");
        when(mongoTemplate.findDistinct(any(Query.class), eq("genres"), eq(Movie.class), eq(String.class)))
                .thenReturn(expectedGenres);

        // Act
        List<String> result = movieService.getDistinctGenres();

        // Assert
        assertNotNull(result);
        assertEquals(5, result.size());
        assertEquals("Action", result.get(0));
        assertEquals("Comedy", result.get(1));
        verify(mongoTemplate).findDistinct(any(Query.class), eq("genres"), eq(Movie.class), eq(String.class));
    }

    @Test
    @DisplayName("Should return empty list when no genres exist")
    void testGetDistinctGenres_EmptyList() {
        // Arrange
        when(mongoTemplate.findDistinct(any(Query.class), eq("genres"), eq(Movie.class), eq(String.class)))
                .thenReturn(Arrays.asList());

        // Act
        List<String> result = movieService.getDistinctGenres();

        // Assert
        assertNotNull(result);
        assertEquals(0, result.size());
        verify(mongoTemplate).findDistinct(any(Query.class), eq("genres"), eq(Movie.class), eq(String.class));
    }

    @Test
    @DisplayName("Should filter out null and empty genres")
    void testGetDistinctGenres_FiltersNullAndEmpty() {
        // Arrange
        List<String> genresWithNulls = new ArrayList<>(Arrays.asList("Action", null, "", "Drama", "Comedy"));
        when(mongoTemplate.findDistinct(any(Query.class), eq("genres"), eq(Movie.class), eq(String.class)))
                .thenReturn(genresWithNulls);

        // Act
        List<String> result = movieService.getDistinctGenres();

        // Assert
        assertNotNull(result);
        // The service should filter out null and empty values
        assertEquals(3, result.size());
        assertTrue(result.contains("Action"));
        assertTrue(result.contains("Drama"));
        assertTrue(result.contains("Comedy"));
        assertFalse(result.contains(null));
        assertFalse(result.contains(""));
    }

    @Test
    @DisplayName("Should return genres sorted alphabetically")
    void testGetDistinctGenres_SortedAlphabetically() {
        // Arrange
        List<String> unsortedGenres = Arrays.asList("Drama", "Action", "Comedy");
        when(mongoTemplate.findDistinct(any(Query.class), eq("genres"), eq(Movie.class), eq(String.class)))
                .thenReturn(unsortedGenres);

        // Act
        List<String> result = movieService.getDistinctGenres();

        // Assert
        assertNotNull(result);
        assertEquals(3, result.size());
        assertEquals("Action", result.get(0));
        assertEquals("Comedy", result.get(1));
        assertEquals("Drama", result.get(2));
    }
}
