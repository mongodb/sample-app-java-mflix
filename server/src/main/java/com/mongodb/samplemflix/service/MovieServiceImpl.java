package com.mongodb.samplemflix.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.UpdateResult;
import com.mongodb.samplemflix.exception.DatabaseOperationException;
import com.mongodb.samplemflix.exception.ResourceNotFoundException;
import com.mongodb.samplemflix.exception.ValidationException;
import com.mongodb.samplemflix.model.Movie;
import com.mongodb.samplemflix.model.dto.*;
import com.mongodb.samplemflix.repository.MovieRepository;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.bson.BsonValue;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.aggregation.ArrayOperators;
import org.springframework.data.mongodb.core.aggregation.ConditionalOperators;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.TextCriteria;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

/**
 * Service layer for movie business logic using Spring Data MongoDB.
 *
 * <p>This service handles:
 * <pre>
 * - Business logic and validation
 * - Query construction using Spring Data MongoDB Query API
 * - Data transformation between DTOs and entities
 * - Error handling and exception throwing
 * </pre>
 * Uses both:
 * <pre>
 * - MovieRepository (Spring Data) for simple CRUD operations
 * - MongoTemplate for complex queries and batch operations
 * </pre>
 */
@Service
public class MovieServiceImpl implements MovieService {

    private final MovieRepository movieRepository;
    private final MongoTemplate mongoTemplate;
    private final ObjectMapper objectMapper;

    @Value("${voyage.api.key:#{null}}")
    private String voyageApiKey;

    public MovieServiceImpl(MovieRepository movieRepository, MongoTemplate mongoTemplate, ObjectMapper objectMapper) {
        this.movieRepository = movieRepository;
        this.mongoTemplate = mongoTemplate;
        this.objectMapper = objectMapper;
    }
    
    @Override
    public List<Movie> getAllMovies(MovieSearchQuery query) {
        Query mongoQuery = buildQuery(query);

        int limit = Math.clamp(query.getLimit() != null ? query.getLimit() : 20, 1, 100);
        int skip = Math.max(query.getSkip() != null ? query.getSkip() : 0, 0);

        mongoQuery.skip(skip).limit(limit);
        mongoQuery.with(buildSort(query.getSortBy(), query.getSortOrder()));

        return mongoTemplate.find(mongoQuery, Movie.class);
    }
    
    @Override
    public Movie getMovieById(String id) {
        if (!ObjectId.isValid(id)) {
            throw new ValidationException("Invalid movie ID format");
        }
        
        return movieRepository.findById(new ObjectId(id))
                .orElseThrow(() -> new ResourceNotFoundException("Movie not found"));
    }
    
    @Override
    public Movie createMovie(CreateMovieRequest request) {
        if (request.getTitle() == null || request.getTitle().trim().isEmpty()) {
            throw new ValidationException("Title is required");
        }

        Movie movie = Movie.builder()
                .title(request.getTitle())
                .year(request.getYear())
                .plot(request.getPlot())
                .fullplot(request.getFullplot())
                .genres(request.getGenres())
                .directors(request.getDirectors())
                .writers(request.getWriters())
                .cast(request.getCast())
                .countries(request.getCountries())
                .languages(request.getLanguages())
                .rated(request.getRated())
                .runtime(request.getRuntime())
                .poster(request.getPoster())
                .build();

        // Spring Data MongoDB's save() method inserts or updates
        return movieRepository.save(movie);
    }
    
    @Override
    public BatchInsertResponse createMoviesBatch(List<CreateMovieRequest> requests) {
        if (requests == null || requests.isEmpty()) {
            throw new ValidationException("Request body must be a non-empty array of movie objects");
        }

        for (int i = 0; i < requests.size(); i++) {
            CreateMovieRequest request = requests.get(i);
            if (request.getTitle() == null || request.getTitle().trim().isEmpty()) {
                throw new ValidationException("Movie at index " + i + ": Title is required");
            }
        }

        List<Movie> movies = requests.stream()
                .map(request -> Movie.builder()
                        .title(request.getTitle())
                        .year(request.getYear())
                        .plot(request.getPlot())
                        .fullplot(request.getFullplot())
                        .genres(request.getGenres())
                        .directors(request.getDirectors())
                        .writers(request.getWriters())
                        .cast(request.getCast())
                        .countries(request.getCountries())
                        .languages(request.getLanguages())
                        .rated(request.getRated())
                        .runtime(request.getRuntime())
                        .poster(request.getPoster())
                        .build())
                .toList();

        // Spring Data MongoDB's saveAll() method for batch insert
        List<Movie> savedMovies = movieRepository.saveAll(movies);

        // Extract IDs from saved movies
        Collection<BsonValue> insertedIds = savedMovies.stream()
                .map(movie -> new org.bson.BsonObjectId(movie.getId()))
                .collect(Collectors.toList());

        return new BatchInsertResponse(
                savedMovies.size(),
                insertedIds
        );
    }
    
    @Override
    public Movie updateMovie(String id, UpdateMovieRequest request) {
        if (!ObjectId.isValid(id)) {
            throw new ValidationException("Invalid movie ID format");
        }

        if (request == null || isUpdateRequestEmpty(request)) {
            throw new ValidationException("No update data provided");
        }

        ObjectId objectId = new ObjectId(id);

        // Build Spring Data MongoDB Update object
        Update update = buildUpdate(request);

        // Use MongoTemplate for update operation
        Query query = new Query(Criteria.where("_id").is(objectId));
        UpdateResult result = mongoTemplate.updateFirst(query, update, Movie.class);

        if (result.getMatchedCount() == 0) {
            throw new ResourceNotFoundException("Movie not found");
        }

        return movieRepository.findById(objectId)
                .orElseThrow(() -> new DatabaseOperationException("Failed to retrieve updated movie"));
    }
    
    @Override
    public BatchUpdateResponse updateMoviesBatch(Document filter, Document update) {
        if (filter == null || update == null) {
            throw new ValidationException("Both filter and update objects are required");
        }

        if (update.isEmpty()) {
            throw new ValidationException("Update object cannot be empty");
        }

        // Convert Document filter to Spring Data Query
        Query query = new Query();
        filter.forEach((key, value) -> {
            Criteria criteria = buildCriteriaFromValue(key, value);
            query.addCriteria(criteria);
        });

        // Convert Document update to Spring Data Update
        Update mongoUpdate = new Update();
        update.forEach(mongoUpdate::set);

        UpdateResult result = mongoTemplate.updateMulti(query, mongoUpdate, Movie.class);

        return new BatchUpdateResponse(
                result.getMatchedCount(),
                result.getModifiedCount()
        );
    }
    
    @Override
    public DeleteResponse deleteMovie(String id) {
        if (!ObjectId.isValid(id)) {
            throw new ValidationException("Invalid movie ID format");
        }

        ObjectId objectId = new ObjectId(id);

        // Check if movie exists before deleting
        if (!movieRepository.existsById(objectId)) {
            throw new ResourceNotFoundException("Movie not found");
        }

        movieRepository.deleteById(objectId);

        return new DeleteResponse(1L);
    }
    
    @Override
    public DeleteResponse deleteMoviesBatch(Document filter) {
        if (filter == null || filter.isEmpty()) {
            throw new ValidationException("Filter object is required and cannot be empty. This prevents accidental deletion of all documents.");
        }

        // Convert Document filter to Spring Data Query
        Query query = new Query();
        filter.forEach((key, value) -> {
            Criteria criteria = buildCriteriaFromValue(key, value);
            query.addCriteria(criteria);
        });

        DeleteResult result = mongoTemplate.remove(query, Movie.class);

        return new DeleteResponse(result.getDeletedCount());
    }
    
    @Override
    public Movie findAndDeleteMovie(String id) {
        if (!ObjectId.isValid(id)) {
            throw new ValidationException("Invalid movie ID format");
        }

        ObjectId objectId = new ObjectId(id);
        Query query = new Query(Criteria.where("_id").is(objectId));

        Movie movie = mongoTemplate.findAndRemove(query, Movie.class);

        if (movie == null) {
            throw new ResourceNotFoundException("Movie not found");
        }

        return movie;
    }
    
    /**
     * Builds a Spring Data MongoDB Query from the search parameters.
     */
    private Query buildQuery(MovieSearchQuery query) {
        Query mongoQuery = new Query();

        // Text search
        if (query.getQ() != null && !query.getQ().trim().isEmpty()) {
            TextCriteria textCriteria = TextCriteria.forDefaultLanguage().matching(query.getQ());
            mongoQuery.addCriteria(textCriteria);
        }

        // Genre filter (case-insensitive regex)
        if (query.getGenre() != null && !query.getGenre().trim().isEmpty()) {
            mongoQuery.addCriteria(Criteria.where(Movie.Fields.GENRES)
                    .regex(Pattern.compile(query.getGenre(), Pattern.CASE_INSENSITIVE)));
        }

        // Year filter
        if (query.getYear() != null) {
            mongoQuery.addCriteria(Criteria.where(Movie.Fields.YEAR).is(query.getYear()));
        }

        // Rating range filter
        if (query.getMinRating() != null || query.getMaxRating() != null) {
            Criteria ratingCriteria = Criteria.where(Movie.Fields.IMDB_RATING);
            if (query.getMinRating() != null) {
                ratingCriteria = ratingCriteria.gte(query.getMinRating());
            }
            if (query.getMaxRating() != null) {
                ratingCriteria = ratingCriteria.lte(query.getMaxRating());
            }
            mongoQuery.addCriteria(ratingCriteria);
        }

        return mongoQuery;
    }

    /**
     * Builds a Spring Data Sort object from sort parameters.
     */
    private Sort buildSort(String sortBy, String sortOrder) {
        String field = sortBy != null && !sortBy.trim().isEmpty() ? sortBy : Movie.Fields.TITLE;
        Sort.Direction direction = "desc".equalsIgnoreCase(sortOrder) ? Sort.Direction.DESC : Sort.Direction.ASC;
        return Sort.by(direction, field);
    }
    
    /**
     * Checks if the update request has any non-null fields.
     */
    private boolean isUpdateRequestEmpty(UpdateMovieRequest request) {
        @SuppressWarnings("unchecked")
        Map<String, Object> requestMap = objectMapper.convertValue(request, Map.class);
        return requestMap.values().stream().allMatch(java.util.Objects::isNull);
    }

    /**
     * Builds a Spring Data MongoDB Update object from the update request.
     */
    private Update buildUpdate(UpdateMovieRequest request) {
        @SuppressWarnings("unchecked")
        Map<String, Object> requestMap = objectMapper.convertValue(request, Map.class);

        Update update = new Update();
        requestMap.forEach((key, value) -> {
            if (value != null) {
                update.set(key, value);
            }
        });

        return update;
    }

    // Aggregation methods for reporting

    @Override
    public List<MovieWithCommentsResult> getMoviesWithMostRecentComments(Integer limit, String movieId) {
        // Validate and set default limit
        int resultLimit = Math.clamp(limit != null ? limit : 10, 1, 50);

        // Build match criteria
        Criteria matchCriteria = Criteria.where(Movie.Fields.YEAR).type(16).gte(1800).lte(2030);

        // Add movie ID filter if provided
        if (movieId != null && !movieId.trim().isEmpty()) {
            if (!ObjectId.isValid(movieId)) {
                throw new ValidationException("Invalid movie ID format");
            }
            matchCriteria = matchCriteria.and(Movie.Fields.ID).is(new ObjectId(movieId));
        }

        // Use the validated limit from the request parameter
        // The limit has already been validated and clamped to 1-50 range
        int finalLimit = resultLimit;

        // Build aggregation pipeline
        // This demonstrates $lookup (join), $addFields, $sort, and $project operations
        // Note: We perform $lookup on all matching movies, then sort and limit
        // This ensures we get the movies with the MOST RECENT comments, not just the first N movies
        Aggregation aggregation = Aggregation.newAggregation(
                // STAGE 1: Match movies with valid year data (and optional movie ID filter)
                // Tip: Use $match early in the pipeline to reduce the dataset size
                Aggregation.match(matchCriteria),

                // STAGE 2: Lookup (join) with comments collection
                // This performs a left outer join, giving each movie a 'comments' array
                Aggregation.lookup("comments", "_id", "movie_id", "comments"),

                // STAGE 3: Filter to only movies that have comments
                // This converts the left join to an inner join
                Aggregation.match(Criteria.where("comments").ne(List.of())),

                // STAGE 4: Add computed fields
                // Calculate totalComments and mostRecentCommentDate for sorting
                Aggregation.project()
                        .and(Movie.Fields.ID).as("_id")
                        .and(Movie.Fields.TITLE).as("title")
                        .and(Movie.Fields.YEAR).as("year")
                        .and(Movie.Fields.PLOT).as("plot")
                        .and(Movie.Fields.POSTER).as("poster")
                        .and(Movie.Fields.GENRES).as("genres")
                        .and(Movie.Fields.IMDB).as("imdb")
                        .and("comments").as("comments")
                        .and(ArrayOperators.Size.lengthOfArray("comments")).as("totalComments")
                        .and(ArrayOperators.ArrayElemAt.arrayOf("comments.date").elementAt(0)).as("mostRecentCommentDate"),

                // STAGE 5: Sort by most recent comment date (descending)
                // This ensures we get movies with the MOST RECENT comment activity
                Aggregation.sort(Sort.Direction.DESC, "mostRecentCommentDate"),

                // STAGE 6: Limit results
                // Apply limit AFTER sorting to get the correct top N movies by recent comment activity
                // Uses the limit from the request parameter (default: 10, max: 50)
                Aggregation.limit(finalLimit),

                // STAGE 7: Project final output with recent comments slice
                // Shape the response and include only the 5 most recent comments per movie
                Aggregation.project()
                        .and(ConditionalOperators.ifNull("_id").then("")).as("_id")
                        .and("title").as("title")
                        .and("year").as("year")
                        .and("plot").as("plot")
                        .and("poster").as("poster")
                        .and("genres").as("genres")
                        .and("imdb.rating").as("imdbRating")
                        .and(ArrayOperators.Slice.sliceArrayOf("comments").itemCount(5)).as("recentComments")
                        .and("totalComments").as("totalComments")
                        .and("mostRecentCommentDate").as("mostRecentCommentDate")
        );

        AggregationResults<Document> results = mongoTemplate.aggregate(
                aggregation, "movies", Document.class);

        // Convert Document results to DTOs
        return results.getMappedResults().stream()
                .map(this::mapToMovieWithCommentsResult)
                .collect(Collectors.toList());
    }

    @Override
    public List<MoviesByYearResult> getMoviesByYearWithStats() {
        // Build aggregation pipeline
        // This demonstrates $group with statistical operators and $project for data shaping
        Aggregation aggregation = Aggregation.newAggregation(
                // STAGE 1: Match movies with valid year data
                Aggregation.match(
                        Criteria.where(Movie.Fields.YEAR).type(16).gte(1800).lte(2030)
                ),

                // STAGE 2: Group by year and calculate statistics
                Aggregation.group(Movie.Fields.YEAR)
                        .count().as("movieCount")
                        .avg(Movie.Fields.IMDB_RATING).as("averageRating")
                        .max(Movie.Fields.IMDB_RATING).as("highestRating")
                        .min(Movie.Fields.IMDB_RATING).as("lowestRating")
                        .sum("imdb.votes").as("totalVotes"),

                // STAGE 3: Project final output with renamed fields
                Aggregation.project()
                        .and("_id").as("year")
                        .and("movieCount").as("movieCount")
                        .and("averageRating").as("averageRating")
                        .and("highestRating").as("highestRating")
                        .and("lowestRating").as("lowestRating")
                        .and("totalVotes").as("totalVotes")
                        .andExclude("_id"),

                // STAGE 4: Sort by year (descending)
                Aggregation.sort(Sort.Direction.DESC, "year")
        );

        AggregationResults<MoviesByYearResult> results = mongoTemplate.aggregate(
                aggregation, "movies", MoviesByYearResult.class);

        // Round average rating to 2 decimal places
        return results.getMappedResults().stream()
                .peek(result -> {
                    if (result.getAverageRating() != null) {
                        result.setAverageRating(
                                Math.round(result.getAverageRating() * 100.0) / 100.0
                        );
                    }
                })
                .collect(Collectors.toList());
    }

    @Override
    public List<DirectorStatisticsResult> getDirectorsWithMostMovies(Integer limit) {
        // Validate and set default limit
        int resultLimit = Math.clamp(limit != null ? limit : 20, 1, 100);

        // Build aggregation pipeline
        // This demonstrates $unwind for array flattening and $group for aggregation
        Aggregation aggregation = Aggregation.newAggregation(
                // STAGE 1: Match movies with directors and valid year
                Aggregation.match(
                        Criteria.where(Movie.Fields.DIRECTORS).exists(true).ne(null).ne(List.of())
                                .and(Movie.Fields.YEAR).type(16).gte(1800).lte(2030)
                ),

                // STAGE 2: Unwind directors array
                Aggregation.unwind(Movie.Fields.DIRECTORS),

                // STAGE 3: Filter out null/empty director names
                Aggregation.match(
                        Criteria.where(Movie.Fields.DIRECTORS).ne(null).ne("")
                ),

                // STAGE 4: Group by director and calculate statistics
                Aggregation.group(Movie.Fields.DIRECTORS)
                        .count().as("movieCount")
                        .avg(Movie.Fields.IMDB_RATING).as("averageRating"),

                // STAGE 5: Sort by movie count (descending)
                Aggregation.sort(Sort.Direction.DESC, "movieCount"),

                // STAGE 6: Limit results
                Aggregation.limit(resultLimit),

                // STAGE 7: Project final output
                Aggregation.project()
                        .and("_id").as("director")
                        .and("movieCount").as("movieCount")
                        .and("averageRating").as("averageRating")
                        .andExclude("_id")
        );

        AggregationResults<DirectorStatisticsResult> results = mongoTemplate.aggregate(
                aggregation, "movies", DirectorStatisticsResult.class);

        // Round average rating to 2 decimal places
        return results.getMappedResults().stream()
                .peek(result -> {
                    if (result.getAverageRating() != null) {
                        result.setAverageRating(
                                Math.round(result.getAverageRating() * 100.0) / 100.0
                        );
                    }
                })
                .collect(Collectors.toList());
    }

    /**
     * Helper method to map Document to MovieWithCommentsResult.
     */
    private MovieWithCommentsResult mapToMovieWithCommentsResult(Document doc) {
        // Extract IMDB rating (just the number)
        Double imdbRating = doc.getDouble("imdbRating");

        // Extract recent comments
        List<MovieWithCommentsResult.CommentInfo> recentComments = null;
        @SuppressWarnings("unchecked")
        List<Document> commentsDoc = (List<Document>) doc.get("recentComments");
        if (commentsDoc != null) {
            recentComments = commentsDoc.stream()
                    .map(commentDoc -> MovieWithCommentsResult.CommentInfo.builder()
                            .id(commentDoc.getObjectId("_id") != null ?
                                    commentDoc.getObjectId("_id").toHexString() : null)
                            .name(commentDoc.getString("name"))
                            .email(commentDoc.getString("email"))
                            .text(commentDoc.getString("text"))
                            .date(commentDoc.getDate("date"))
                            .build())
                    .collect(Collectors.toList());
        }

        // Extract movie ID - handle both String and ObjectId types
        String movieId = null;
        Object idObj = doc.get("_id");
        if (idObj instanceof String) {
            movieId = (String) idObj;
        } else if (idObj instanceof ObjectId) {
            movieId = ((ObjectId) idObj).toHexString();
        }

        return MovieWithCommentsResult.builder()
                ._id(movieId)
                .title(doc.getString("title"))
                .year(doc.getInteger("year"))
                .plot(doc.getString("plot"))
                .poster(doc.getString("poster"))
                .genres(doc.getList("genres", String.class))
                .imdbRating(imdbRating)
                .recentComments(recentComments)
                .totalComments(doc.getInteger("totalComments"))
                .mostRecentCommentDate(doc.getDate("mostRecentCommentDate"))
                .build();
    }

    // MongoDB Search methods

    @Override
    public List<Movie> searchMovies(MovieSearchRequest searchRequest) {
        // Validate that at least one search field is provided
        if (!searchRequest.hasSearchFields()) {
            throw new ValidationException("At least one search parameter must be provided");
        }

        // Validate search operator
        String operator = searchRequest.getSearchOperator() != null ?
                searchRequest.getSearchOperator() : "must";

        if (!operator.equals("must") && !operator.equals("should") &&
            !operator.equals("mustNot") && !operator.equals("filter")) {
            throw new ValidationException(
                "Invalid search_operator '" + operator + "'. " +
                "The search_operator must be one of: must, should, mustNot, filter"
            );
        }

        // Validate and set defaults for pagination
        int resultLimit = Math.clamp(
            searchRequest.getLimit() != null ? searchRequest.getLimit() : 20, 1, 100
        );
        int resultSkip = Math.max(
            searchRequest.getSkip() != null ? searchRequest.getSkip() : 0, 0
        );

        // Build search phrases list
        java.util.List<Document> searchPhrases = new java.util.ArrayList<>();

        // Add plot search if provided (using phrase operator)
        if (searchRequest.getPlot() != null && !searchRequest.getPlot().trim().isEmpty()) {
            searchPhrases.add(new Document("phrase", new Document()
                    .append("query", searchRequest.getPlot().trim())
                    .append("path", Movie.Fields.PLOT)
            ));
        }

        // Add fullplot search if provided (using phrase operator)
        if (searchRequest.getFullplot() != null && !searchRequest.getFullplot().trim().isEmpty()) {
            searchPhrases.add(new Document("phrase", new Document()
                    .append("query", searchRequest.getFullplot().trim())
                    .append("path", Movie.Fields.FULLPLOT)
            ));
        }

        // Add directors search if provided (using text operator with fuzzy matching)
        if (searchRequest.getDirectors() != null && !searchRequest.getDirectors().trim().isEmpty()) {
            searchPhrases.add(new Document("text", new Document()
                    .append("query", searchRequest.getDirectors().trim())
                    .append("path", Movie.Fields.DIRECTORS)
                    .append("fuzzy", new Document()
                            .append("maxEdits", 1)
                            .append("prefixLength", 5)
                    )
            ));
        }

        // Add writers search if provided (using text operator with fuzzy matching)
        if (searchRequest.getWriters() != null && !searchRequest.getWriters().trim().isEmpty()) {
            searchPhrases.add(new Document("text", new Document()
                    .append("query", searchRequest.getWriters().trim())
                    .append("path", Movie.Fields.WRITERS)
                    .append("fuzzy", new Document()
                            .append("maxEdits", 1)
                            .append("prefixLength", 5)
                    )
            ));
        }

        // Add cast search if provided (using text operator with fuzzy matching)
        if (searchRequest.getCast() != null && !searchRequest.getCast().trim().isEmpty()) {
            searchPhrases.add(new Document("text", new Document()
                    .append("query", searchRequest.getCast().trim())
                    .append("path", Movie.Fields.CAST)
                    .append("fuzzy", new Document()
                            .append("maxEdits", 1)
                            .append("prefixLength", 5)
                    )
            ));
        }

        // Build the $search aggregation stage with compound operator
        Document searchStage = new Document("$search", new Document()
                .append("index", "movieSearchIndex")
                .append("compound", new Document(operator, searchPhrases))
        );

        Document skipStage = new Document("$skip", resultSkip);
        Document limitStage = new Document("$limit", resultLimit);

        // Project only the fields needed in the response
        Document projectStage = new Document("$project", new Document()
                .append(Movie.Fields.ID, 1)
                .append(Movie.Fields.TITLE, 1)
                .append(Movie.Fields.YEAR, 1)
                .append(Movie.Fields.PLOT, 1)
                .append(Movie.Fields.FULLPLOT, 1)
                .append(Movie.Fields.RELEASED, 1)
                .append(Movie.Fields.RUNTIME, 1)
                .append(Movie.Fields.POSTER, 1)
                .append(Movie.Fields.GENRES, 1)
                .append(Movie.Fields.DIRECTORS, 1)
                .append(Movie.Fields.WRITERS, 1)
                .append(Movie.Fields.CAST, 1)
                .append(Movie.Fields.COUNTRIES, 1)
                .append(Movie.Fields.LANGUAGES, 1)
                .append(Movie.Fields.RATED, 1)
                .append(Movie.Fields.AWARDS, 1)
                .append(Movie.Fields.IMDB, 1)
        );

        // Execute the aggregation pipeline
        try {
            java.util.List<Document> aggregationPipeline = java.util.List.of(
                searchStage, skipStage, limitStage, projectStage
            );

            return mongoTemplate.getCollection("movies")
                    .aggregate(aggregationPipeline)
                    .map(doc -> mongoTemplate.getConverter().read(Movie.class, doc))
                    .into(new java.util.ArrayList<>());
        } catch (Exception e) {
            throw new DatabaseOperationException("Error performing MongoDB Search: " + e.getMessage());
        }
    }

    @Override
    public List<Movie> findSimilarMovies(String movieId, Integer limit) {
        // Validate movie ID
        if (movieId == null || movieId.trim().isEmpty()) {
            throw new ValidationException("Movie ID is required");
        }

        if (!ObjectId.isValid(movieId)) {
            throw new ValidationException("Invalid movie ID format");
        }

        // Validate and set default limit
        int resultLimit = Math.clamp(limit != null ? limit : 10, 1, 50);

        // First, get the movie to retrieve its plot_embedding
        ObjectId objectId = new ObjectId(movieId);
        Document movie = mongoTemplate.getCollection("movies")
                .find(new Document(Movie.Fields.ID, objectId))
                .first();

        if (movie == null) {
            throw new ResourceNotFoundException("Movie not found");
        }

        // Check if plot_embedding exists
        if (!movie.containsKey("plot_embedding")) {
            throw new ValidationException("Movie does not have plot embeddings for vector search");
        }

        @SuppressWarnings("unchecked")
        List<Double> plotEmbedding = (List<Double>) movie.get("plot_embedding");

        // Build the $vectorSearch aggregation stage
        // Note: This requires MongoDB Atlas with a vector search index configured
        Document vectorSearchStage = new Document("$vectorSearch", new Document()
                .append("index", "plotEmbeddingIndex")
                .append("path", "plot_embedding")
                .append("queryVector", plotEmbedding)
                .append("numCandidates", resultLimit * 20) // We recommend searching 20 times higher than the limit to improve result relevance
                .append("limit", resultLimit + 1) // +1 to exclude the source movie
        );

        // Filter out the source movie
        Document matchStage = new Document("$match",
                new Document(Movie.Fields.ID, new Document("$ne", objectId))
        );

        // Limit to final result count
        Document limitStage = new Document("$limit", resultLimit);

        // Project only the fields needed in the response
        Document projectStage = new Document("$project", new Document()
                .append(Movie.Fields.ID, 1)
                .append(Movie.Fields.TITLE, 1)
                .append(Movie.Fields.YEAR, 1)
                .append(Movie.Fields.PLOT, 1)
                .append(Movie.Fields.FULLPLOT, 1)
                .append(Movie.Fields.RELEASED, 1)
                .append(Movie.Fields.RUNTIME, 1)
                .append(Movie.Fields.POSTER, 1)
                .append(Movie.Fields.GENRES, 1)
                .append(Movie.Fields.DIRECTORS, 1)
                .append(Movie.Fields.WRITERS, 1)
                .append(Movie.Fields.CAST, 1)
                .append(Movie.Fields.COUNTRIES, 1)
                .append(Movie.Fields.LANGUAGES, 1)
                .append(Movie.Fields.RATED, 1)
                .append(Movie.Fields.AWARDS, 1)
                .append(Movie.Fields.IMDB, 1)
                .append("score", new Document("$meta", "vectorSearchScore"))
        );

        // Execute the aggregation pipeline
        try {
            List<Document> aggregationPipeline = List.of(
                    vectorSearchStage, matchStage, limitStage, projectStage
            );

            return mongoTemplate.getCollection("movies")
                    .aggregate(aggregationPipeline)
                    .map(doc -> mongoTemplate.getConverter().read(Movie.class, doc))
                    .into(new java.util.ArrayList<>());
        } catch (Exception e) {
            throw new DatabaseOperationException("Error performing vector search: " + e.getMessage());
        }
    }

    /**
     * Performs vector search on movie plots using MongoDB Vector Search.
     * 
     * This method uses a two-step process:
     * 1. Query the embedded_movies collection (which has vector embeddings) to get movie IDs and similarity scores
     * 2. Fetch complete movie data from the movies collection using those IDs
     * 
     * This approach ensures that:
     * - Vector search works correctly with the embedded data
     * - Returned movie objects are compatible with CRUD operations on the movies collection
     * - Complete movie metadata is available in the response
     */
    @Override
    public List<VectorSearchResult> vectorSearchMovies(String query, Integer limit) {
        // Validate query parameter
        if (query == null || query.trim().isEmpty()) {
            throw new ValidationException("Search query is required");
        }

        // Check if Voyage API key is configured
        if (voyageApiKey == null || voyageApiKey.trim().isEmpty() ||
            voyageApiKey.equals("your_voyage_api_key")) {
            throw new ValidationException(
                "Vector search unavailable: VOYAGE_API_KEY not configured. Please add your Voyage AI API key to the .env file"
            );
        }

        // Validate and set default limit
        int resultLimit = Math.clamp(limit != null ? limit : 10, 1, 50);

        try {
            // Generate embedding using Voyage AI REST API
            // We call the API directly to specify output_dimension=2048
            List<Double> queryVector = generateVoyageEmbedding(query, voyageApiKey);

            // Build the $vectorSearch aggregation stage
            Document vectorSearchStage = new Document("$vectorSearch", new Document()
                    .append("index", "vector_index")
                    .append("path", "plot_embedding_voyage_3_large")
                    .append("queryVector", queryVector)
                    .append("numCandidates", resultLimit * 20)  // We recommend searching 20 times higher than the limit to improve result relevance
                    .append("limit", resultLimit)
            );

            // Project only the fields we need from embedded_movies: _id and score
            Document projectStage = new Document("$project", new Document()
                    .append("_id", 1)
                    .append("score", new Document("$meta", "vectorSearchScore"))
            );

            // Execute the aggregation pipeline on the embedded_movies collection
            List<Document> aggregationPipeline = List.of(vectorSearchStage, projectStage);

            // Step 1: Get movie IDs and scores from embedded_movies (which has the vector embeddings)
            List<ObjectId> movieIds = new ArrayList<>();
            Map<String, Double> scoreMap = new HashMap<>();

            mongoTemplate.getCollection("embedded_movies")
                    .aggregate(aggregationPipeline)
                    .forEach(doc -> {
                        ObjectId movieId = doc.getObjectId("_id");
                        movieIds.add(movieId);
                        scoreMap.put(movieId.toString(), doc.getDouble("score"));
                    });

            // Step 2: Fetch complete movie data from the movies collection (for CRUD compatibility)
            // Use aggregation to safely handle dirty data in the year field
            List<VectorSearchResult> results = new ArrayList<>();

            if (!movieIds.isEmpty()) {
                // Build aggregation pipeline to safely convert year field
                Document matchStage = new Document("$match", new Document("_id", new Document("$in", movieIds)));

                // Project stage to safely convert year to integer, handling dirty data
                Document projectStage2 = new Document("$project", new Document()
                        .append("_id", 1)
                        .append("title", 1)
                        .append("plot", 1)
                        .append("poster", 1)
                        .append("genres", 1)
                        .append("directors", 1)
                        .append("cast", 1)
                        // Safely convert year to integer, handling strings and dirty data
                        .append("year", new Document("$cond", new Document()
                                .append("if", new Document("$and", java.util.Arrays.asList(
                                        new Document("$ne", java.util.Arrays.asList("$year", null)),
                                        new Document("$eq", java.util.Arrays.asList(new Document("$type", "$year"), "int"))
                                )))
                                .append("then", "$year")
                                .append("else", null)
                        ))
                );

                List<Document> moviePipeline = List.of(matchStage, projectStage2);

                // Execute aggregation and manually build VectorSearchResult objects
                mongoTemplate.getCollection("movies").aggregate(moviePipeline)
                        .forEach(doc -> {
                            ObjectId movieIdObj = doc.getObjectId("_id");
                            if (movieIdObj == null) {
                                return;
                            }

                            String movieIdStr = movieIdObj.toString();
                            Double score = scoreMap.get(movieIdStr);

                            if (score != null) {  // Only include movies that have vector scores
                                // Safely get list fields, defaulting to null if not present
                                List<String> genres = doc.getList("genres", String.class);
                                List<String> directors = doc.getList("directors", String.class);
                                List<String> cast = doc.getList("cast", String.class);

                                VectorSearchResult result = VectorSearchResult.builder()
                                        .id(movieIdStr)
                                        .title(doc.getString("title"))
                                        .plot(doc.getString("plot"))
                                        .poster(doc.getString("poster"))
                                        .year(doc.getInteger("year"))  // Will be null for dirty data
                                        .genres(genres)
                                        .directors(directors)
                                        .cast(cast)
                                        .score(score)
                                        .build();
                                results.add(result);
                            }
                        });
            }

            return results;

        } catch (IOException e) {
            // Handle Voyage AI API errors
            String errorMsg = e.getMessage() != null ? e.getMessage() : "Network error calling Voyage AI API";
            throw new DatabaseOperationException("Error performing vector search: " + errorMsg);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new DatabaseOperationException("Vector search was interrupted");
        } catch (Exception e) {
            // Handle other errors (e.g., MongoDB errors, parsing errors)
            String errorMsg = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
            throw new DatabaseOperationException("Error performing vector search: " + errorMsg);
        }
    }

    /**
     * Generates a vector embedding using the Voyage AI REST API.
     *
     * <p>This method calls the Voyage AI API directly to generate embeddings with 2048 dimensions.
     * The voyage-3-large model supports multiple dimensions (256, 512, 1024, 2048), and we explicitly
     * request 2048 to match the vector search index configuration.
     *
     * @param text The text to generate an embedding for
     * @param apiKey The Voyage AI API key
     * @return List of doubles representing the embedding vector
     * @throws IOException if the HTTP request fails
     * @throws InterruptedException if the HTTP request is interrupted
     */
    private List<Double> generateVoyageEmbedding(String text, String apiKey) throws IOException, InterruptedException {
        // Create HTTP client
        HttpClient client = HttpClient.newHttpClient();

        // Build the request body with output_dimension set to 2048
        String requestBody = String.format(
                "{\"input\": [\"%s\"], \"model\": \"voyage-3-large\", \"output_dimension\": 2048, \"input_type\": \"query\"}",
                text.replace("\"", "\\\"").replace("\n", "\\n")
        );

        // Create the HTTP request
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.voyageai.com/v1/embeddings"))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        // Send the request and get the response
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        // Check for successful response
        if (response.statusCode() != 200) {
            // Handle authentication errors specifically
            if (response.statusCode() == 401) {
                throw new IOException("Invalid Voyage AI API key. Please check your VOYAGE_API_KEY in the .env file");
            }
            throw new IOException("Voyage AI API returned status code " + response.statusCode() + ": " + response.body());
        }

        // Parse the JSON response to extract the embedding
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(response.body());

        // Validate response structure
        if (!root.has("data")) {
            throw new IOException("Invalid Voyage AI API response: missing 'data' field. Response: " + response.body());
        }

        JsonNode dataNode = root.get("data");
        if (dataNode == null || !dataNode.isArray() || dataNode.size() == 0) {
            throw new IOException("Invalid Voyage AI API response: 'data' field is empty or not an array. Response: " + response.body());
        }

        JsonNode firstElement = dataNode.get(0);
        if (firstElement == null || !firstElement.has("embedding")) {
            throw new IOException("Invalid Voyage AI API response: missing 'embedding' field. Response: " + response.body());
        }

        JsonNode embeddingNode = firstElement.get("embedding");
        if (embeddingNode == null || !embeddingNode.isArray()) {
            throw new IOException("Invalid Voyage AI API response: 'embedding' is not an array. Response: " + response.body());
        }

        // Convert the embedding to a List<Double>
        List<Double> embedding = new ArrayList<>();
        for (JsonNode value : embeddingNode) {
            embedding.add(value.asDouble());
        }

        return embedding;
    }

    /**
     * Builds a Spring Data Criteria from a filter key-value pair.
     * Handles MongoDB query operators like $in, $gt, $lt, etc.
     *
     * @param key The field name (e.g., "_id")
     * @param value The filter value (can be a simple value or a Document with operators)
     * @return Criteria object for the query
     */
    @SuppressWarnings("unchecked")
    private Criteria buildCriteriaFromValue(String key, Object value) {
        Criteria criteria = Criteria.where(key);

        // If value is a Document (Map), it might contain MongoDB operators
        if (value instanceof Map) {
            Map<String, Object> operatorMap = (Map<String, Object>) value;

            // Handle each MongoDB operator
            for (Map.Entry<String, Object> entry : operatorMap.entrySet()) {
                String operator = entry.getKey();
                Object operatorValue = entry.getValue();

                switch (operator) {
                    case "$in":
                        // Convert string IDs to ObjectIds if the field is _id
                        if ("_id".equals(key) && operatorValue instanceof List) {
                            List<?> values = (List<?>) operatorValue;
                            List<ObjectId> objectIds = values.stream()
                                    .map(id -> new ObjectId(id.toString()))
                                    .collect(Collectors.toList());
                            criteria = criteria.in(objectIds);
                        } else {
                            criteria = criteria.in((List<?>) operatorValue);
                        }
                        break;
                    case "$nin":
                        criteria = criteria.nin((List<?>) operatorValue);
                        break;
                    case "$gt":
                        criteria = criteria.gt(operatorValue);
                        break;
                    case "$gte":
                        criteria = criteria.gte(operatorValue);
                        break;
                    case "$lt":
                        criteria = criteria.lt(operatorValue);
                        break;
                    case "$lte":
                        criteria = criteria.lte(operatorValue);
                        break;
                    case "$ne":
                        criteria = criteria.ne(operatorValue);
                        break;
                    case "$regex":
                        criteria = criteria.regex(operatorValue.toString());
                        break;
                    case "$exists":
                        criteria = criteria.exists((Boolean) operatorValue);
                        break;
                    default:
                        // For unknown operators, use the value as-is
                        criteria = criteria.is(value);
                        break;
                }
            }
        } else {
            // Simple equality check
            // Convert string ID to ObjectId if the field is _id
            if ("_id".equals(key) && value instanceof String) {
                criteria = criteria.is(new ObjectId(value.toString()));
            } else {
                criteria = criteria.is(value);
            }
        }

        return criteria;
    }
}
