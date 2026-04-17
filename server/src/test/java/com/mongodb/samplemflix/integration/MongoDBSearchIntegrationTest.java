package com.mongodb.samplemflix.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mongodb.client.MongoCollection;
import com.mongodb.samplemflix.model.Movie;
import com.mongodb.samplemflix.service.MovieService;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.TimeZone;
import lombok.extern.slf4j.Slf4j;
import org.bson.BsonDateTime;
import org.bson.Document;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.test.context.ActiveProfiles;

/**
 * Integration tests for MongoDB Search functionality.
 *
 * <p>These tests verify the MongoDB Search endpoints work correctly with a real
 * MongoDB instance. By default, a MongoDBAtlasLocalContainer (Docker) is started
 * automatically, providing a local Atlas environment with Search support.
 *
 * <p>To run against an external MongoDB Atlas cluster instead, set the
 * {@code MONGODB_URI} environment variable. When set, no container is started.
 */
@SpringBootTest(classes = MongoDBTestContainersConfig.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ActiveProfiles("test")
@DisplayName("MongoDB Search Integration Tests")
@Slf4j
class   MongoDBSearchIntegrationTest {

    @Autowired
    private MovieService movieService;

    @Autowired
    private MongoTemplate mongoTemplate;

    private static final String TEST_COLLECTION = "movies_search_test";
    private static final String SEARCH_INDEX_NAME = "movieSearchIndex";
    private static final int MAX_INDEX_WAIT_SECONDS = 120;
    private static final int POLL_INTERVAL_SECONDS = 5;

    private List<String> testMovieIds = new ArrayList<>();

    @BeforeAll
    void setUp() throws Exception {
        log.info("Setting up MongoDB Search integration tests...");

        // Create test data
        createTestMovies();

        // Create Search index
        createSearchIndex();

        // Wait for index to be ready
        waitForSearchIndexReady();

        // Wait a bit for the newly created documents to be indexed
        // MongoDB Search indexes documents asynchronously
        log.info("Waiting for test documents to be indexed...");
        try {
            Thread.sleep(10000); // Wait 10 seconds for indexing
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        log.info("MongoDB Search index is ready for testing");
    }

    @AfterAll
    void tearDown() {
        log.info("Cleaning up MongoDB Search test data...");

        // Clean up test movies
        if (!testMovieIds.isEmpty()) {
            MongoCollection<Document> collection = mongoTemplate.getCollection("movies");
            testMovieIds.forEach(id -> {
                collection.deleteOne(new Document("_id", new org.bson.types.ObjectId(id)));
            });
        }

        // Note: We don't delete the search index as it may be used by other tests
        // and takes time to recreate
    }

    @Test
    @DisplayName("Should search movies by plot using MongoDB Search")
    void testSearchMoviesByPlot_Success() {
        // Act
        com.mongodb.samplemflix.model.dto.MovieSearchRequest searchRequest =
            com.mongodb.samplemflix.model.dto.MovieSearchRequest.builder()
                .plot("space adventure")
                .limit(10)
                .skip(0)
                .searchOperator("must")
                .build();
        List<Movie> results = movieService.searchMovies(searchRequest);

        // Assert
        assertNotNull(results, "Search results should not be null");
        assertFalse(results.isEmpty(), "Should find at least one movie with 'space adventure' in plot");

        // Verify the results contain our test movie
        boolean foundTestMovie = results.stream()
                .anyMatch(movie -> movie.getPlot() != null &&
                         movie.getPlot().contains("space adventure"));
        assertTrue(foundTestMovie, "Results should contain movie with 'space adventure' in plot");
    }

    @Test
    @DisplayName("Should return empty list when no movies match search query")
    void testSearchMoviesByPlot_NoResults() {
        // Act - search for something that definitely doesn't exist
        com.mongodb.samplemflix.model.dto.MovieSearchRequest searchRequest =
            com.mongodb.samplemflix.model.dto.MovieSearchRequest.builder()
                .plot("xyzabc123nonexistent")
                .limit(10)
                .skip(0)
                .searchOperator("must")
                .build();
        List<Movie> results = movieService.searchMovies(searchRequest);

        // Assert
        assertNotNull(results, "Search results should not be null");
        assertTrue(results.isEmpty(), "Should return empty list when no matches found");
    }

    @Test
    @DisplayName("Should respect limit parameter in search")
    void testSearchMoviesByPlot_WithLimit() {
        // Act
        com.mongodb.samplemflix.model.dto.MovieSearchRequest searchRequest =
            com.mongodb.samplemflix.model.dto.MovieSearchRequest.builder()
                .plot("adventure")
                .limit(2)
                .skip(0)
                .searchOperator("must")
                .build();
        List<Movie> results = movieService.searchMovies(searchRequest);

        // Assert
        assertNotNull(results, "Search results should not be null");
        assertTrue(results.size() <= 2, "Should respect limit parameter");
    }

    @Test
    @DisplayName("Should support pagination with skip parameter")
    void testSearchMoviesByPlot_WithPagination() {
        // Act - Get first page
        com.mongodb.samplemflix.model.dto.MovieSearchRequest firstPageRequest =
            com.mongodb.samplemflix.model.dto.MovieSearchRequest.builder()
                .plot("adventure")
                .limit(2)
                .skip(0)
                .searchOperator("must")
                .build();
        List<Movie> firstPage = movieService.searchMovies(firstPageRequest);

        // Act - Get second page
        com.mongodb.samplemflix.model.dto.MovieSearchRequest secondPageRequest =
            com.mongodb.samplemflix.model.dto.MovieSearchRequest.builder()
                .plot("adventure")
                .limit(2)
                .skip(2)
                .searchOperator("must")
                .build();
        List<Movie> secondPage = movieService.searchMovies(secondPageRequest);

        // Assert
        assertNotNull(firstPage, "First page should not be null");
        assertNotNull(secondPage, "Second page should not be null");

        // If we have enough results, verify pagination works
        if (firstPage.size() == 2 && !secondPage.isEmpty()) {
            // Verify different results on different pages
            String firstPageFirstId = firstPage.get(0).getId().toHexString();
            boolean isDifferent = secondPage.stream()
                    .noneMatch(movie -> movie.getId().toHexString().equals(firstPageFirstId));
            assertTrue(isDifferent, "Second page should contain different results");
        }
    }

    // ==================== RELEASED FIELD ROUND-TRIP TESTS ====================

    @Test
    @DisplayName("Should read BSON DateTime at midnight UTC as the correct LocalDate without date shift")
    void testReleasedFieldRoundTrip_NoDateShift() {
        // The test movies were inserted with known BSON DateTime values at midnight UTC:
        //   "Test Space Adventure"  -> 2024-01-01T00:00:00Z (1704067200000)
        //   "Test Mystery Movie"    -> 2024-03-31T00:00:00Z (1711843200000)
        //   "Test Adventure Quest"  -> 2024-12-31T00:00:00Z (1735603200000)
        // A JVM timezone west of UTC could shift these dates backward by one day
        // (e.g. 2024-01-01 -> 2023-12-31). Cycle through multiple timezones to
        // verify the native LocalDateCodec always interprets BSON DateTime in UTC.

        List<String> timezones = List.of(
                "America/New_York",
                "America/Los_Angeles",
                "Asia/Tokyo",
                "Europe/London",
                "Pacific/Auckland"
        );

        TimeZone originalTz = TimeZone.getDefault();
        try {
            for (String zoneId : timezones) {
                TimeZone.setDefault(TimeZone.getTimeZone(zoneId));

                Movie spaceAdventure = findTestMovieByTitle("Test Space Adventure");
                assertEquals(LocalDate.of(2024, 1, 1), spaceAdventure.getReleased(),
                        "2024-01-01T00:00:00Z shifted in " + zoneId);

                Movie mystery = findTestMovieByTitle("Test Mystery Movie");
                assertEquals(LocalDate.of(2024, 3, 31), mystery.getReleased(),
                        "2024-03-31T00:00:00Z shifted in " + zoneId);

                Movie adventureQuest = findTestMovieByTitle("Test Adventure Quest");
                assertEquals(LocalDate.of(2024, 12, 31), adventureQuest.getReleased(),
                        "2024-12-31T00:00:00Z shifted in " + zoneId);
            }
        } finally {
            TimeZone.setDefault(originalTz);
        }
    }

    private Movie findTestMovieByTitle(String title) {
        return mongoTemplate.findOne(new Query(Criteria.where("title").is(title)), Movie.class);
    }

    // ==================== HELPER METHODS ====================


    private void createTestMovies() {
        log.info("Creating test movies...");

        MongoCollection<Document> collection = mongoTemplate.getCollection("movies");

        List<Document> testMovies = Arrays.asList(
                new Document()
                        .append("title", "Test Space Adventure")
                        .append("year", 2024)
                        .append("released", new BsonDateTime(1704067200000L)) // 1st of Jan 2024 in UTC
                        .append("plot", "An epic space adventure across the galaxy")
                        .append("genres", Arrays.asList("Sci-Fi", "Adventure")),
                new Document()
                        .append("title", "Test Mystery Movie")
                        .append("year", 2024)
                        .append("released", new BsonDateTime(1711843200000L)) // 31 of March 2024 in UTC
                        .append("plot", "A detective solves a mysterious crime")
                        .append("genres", Arrays.asList("Mystery", "Thriller")),
                new Document()
                        .append("title", "Test Adventure Quest")
                        .append("year", 2024)
                        .append("released", new BsonDateTime(1735603200000L)) // 31 December 2024 in UTC
                        .append("plot", "Heroes embark on a dangerous adventure")
                        .append("genres", Arrays.asList("Adventure", "Fantasy"))
        );

        testMovies.forEach(movie -> {
            collection.insertOne(movie);
            testMovieIds.add(movie.getObjectId("_id").toHexString());
        });

        log.info("Created {} test movies", testMovieIds.size());
    }

    private void createSearchIndex() throws Exception {
        log.info("Creating Search index...");

        MongoCollection<Document> collection = mongoTemplate.getCollection("movies");

        // Check if index already exists
        List<Document> existingIndexes = new ArrayList<>();
        collection.listSearchIndexes().into(existingIndexes);

        boolean indexExists = existingIndexes.stream()
                .anyMatch(idx -> SEARCH_INDEX_NAME.equals(idx.getString("name")));

        if (indexExists) {
            log.info("Search index already exists");
            return;
        }

        // Create the search index definition
        Document indexDefinition = new Document("mappings", new Document()
                .append("dynamic", false)
                .append("fields", new Document()
                        .append("plot", new Document()
                                .append("type", "string")
                                .append("analyzer", "lucene.standard"))
                        .append("fullplot", new Document()
                                .append("type", "string")
                                .append("analyzer", "lucene.standard"))
                        .append("directors", new Document()
                                .append("type", "string")
                                .append("analyzer", "lucene.standard"))
                        .append("writers", new Document()
                                .append("type", "string")
                                .append("analyzer", "lucene.standard"))
                        .append("cast", new Document()
                                .append("type", "string")
                                .append("analyzer", "lucene.standard"))
                )
        );

        // Create the index using the createSearchIndexes command
        Document createIndexCommand = new Document("createSearchIndexes", "movies")
                .append("indexes", Collections.singletonList(
                        new Document("name", SEARCH_INDEX_NAME)
                                .append("definition", indexDefinition)
                ));

        try {
            mongoTemplate.getDb().runCommand(createIndexCommand);
            log.info("Search index creation initiated");
        } catch (Exception e) {
            log.error("Error creating search index: {}", e.getMessage());
            throw e;
        }
    }

    private void waitForSearchIndexReady() throws Exception {
        log.info("Waiting for search index to be ready...");

        MongoCollection<Document> collection = mongoTemplate.getCollection("movies");
        long startTime = System.currentTimeMillis();
        long maxWaitMillis = MAX_INDEX_WAIT_SECONDS * 1000L;

        while (System.currentTimeMillis() - startTime < maxWaitMillis) {
            List<Document> indexes = new ArrayList<>();
            collection.listSearchIndexes().into(indexes);

            Document searchIndex = indexes.stream()
                    .filter(idx -> SEARCH_INDEX_NAME.equals(idx.getString("name")))
                    .findFirst()
                    .orElse(null);

            if (searchIndex != null) {
                String status = searchIndex.getString("status");
                log.info("Index status: {}", status);

                if ("READY".equals(status)) {
                    log.info("Search index is ready!");
                    return;
                }
            }

            // Wait before polling again
            Thread.sleep(POLL_INTERVAL_SECONDS * 1000L);
        }

        throw new RuntimeException("Search index did not become ready within " + 
                                   MAX_INDEX_WAIT_SECONDS + " seconds");
    }
}
