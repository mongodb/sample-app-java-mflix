package com.mongodb.samplemflix.config;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import com.mongodb.samplemflix.model.Movie;
import jakarta.annotation.PostConstruct;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Database verification component that runs on application startup.
 *
 * <p>This component performs pre-flight checks to ensure the MongoDB database
 * is properly configured and contains the expected data and indexes.
 *
 * <p>Verification steps:
 * 1. Check if the movies collection exists
 * 2. Verify the collection contains documents
 * 3. Check for text search indexes on plot, title, and fullplot fields
 * 4. Create text search index if missing
 * 5. Verify embedded_movies collection for vector search
 * 6. Create vector search index if missing
 * <p>
 * This matches the behavior of the Express.js backend's verifyRequirements() function.
 * The verification is non-blocking - the application will start even if verification fails,
 * but warnings will be logged to help developers identify configuration issues.
 */
@Component
public class DatabaseVerification {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseVerification.class);

    private static final String MOVIES_COLLECTION = "movies";
    private static final String COMMENTS_COLLECTION = "comments";
    private static final String EMBEDDED_MOVIES_COLLECTION = "embedded_movies";
    private static final String TEXT_INDEX_NAME = "text_search_index";
    private static final String YEAR_INDEX_NAME = "year_index";
    private static final String MOVIE_ID_INDEX_NAME = "movie_id_index";
    private static final String VECTOR_INDEX_NAME = "vector_index";
    private static final String MONGODB_SEARCH_INDEX_NAME = "movieSearchIndex";

    private final MongoDatabase database;

    public DatabaseVerification(MongoDatabase database) {
        this.database = database;
    }

    /**
     * Runs database verification checks after the bean is constructed.
     *
     * <p>This method is called automatically by Spring after dependency injection
     * is complete. It performs all verification steps and logs the results.
     *
     * <p>The method catches all exceptions to prevent application startup failure,
     * but logs errors to help developers identify issues.
     */
    @PostConstruct
    public void verifyDatabase() {
        logger.info("Starting database verification for '{}'...", database.getName());

        try {
            // Verify movies collection exists and has data
            verifyMoviesCollection();

            // Verify comments collection and create indexes for aggregation performance
            verifyCommentsCollection();

            // Verify embedded_movies collection and create vector search index
            verifyEmbeddedMoviesCollection();

            logger.info("Database verification completed successfully");

        } catch (Exception e) {
            logger.error("Database verification failed: {}", e.getMessage(), e);
            // Don't throw exception - allow application to start even if verification fails
            // This allows developers to troubleshoot connection issues without preventing startup
        }
    }

    /**
     * Verifies the movies collection exists, contains data, and has required indexes.
     *
     * <p>This method:
* <ol>
*   <li>Checks if the movies collection exists (implicitly by accessing it)</li>
*   <li>Counts documents to verify sample data is loaded</li>
*   <li>Creates a text search index on plot, title, and fullplot fields</li>
* </ol>
     * <p>The text search index enables full-text search functionality across movie
     * descriptions and titles, which is used by the search endpoint.
     */
    private void verifyMoviesCollection() {
        MongoCollection<Document> moviesCollection = database.getCollection(MOVIES_COLLECTION);

        // Check if collection has documents
        // Using estimatedDocumentCount() for better performance (doesn't scan all documents)
        long count = moviesCollection.estimatedDocumentCount();

        logger.info("Movies collection found with {} documents", count);

        if (count == 0) {
            logger.warn(
                "Movies collection is empty. Please ensure sample_mflix data is loaded. " +
                "Visit https://www.mongodb.com/docs/atlas/sample-data/ for instructions."
            );
        }

        // Create text search index for full-text search functionality
        createTextSearchIndex(moviesCollection);

        // Create MongoDB Search index for advanced search functionality
        createMongoDBSearchIndex(moviesCollection);

        // Create year index for aggregation performance
        createYearIndex(moviesCollection);
    }

    /**
     * Creates a text search index on the movies collection if it doesn't already exist.
     *
     * <p>The index is created on three fields:
* <ul>
*   <li>plot: Short movie description</li>
*   <li>title: Movie title</li>
*   <li>fullplot: Full movie description</li>
* </ul>
     * <p>This enables the $text search operator to perform full-text search across
     * these fields, which is used by the search endpoint in the API.
     *
     * <p>The index is created in the background to avoid blocking other operations.
     * If the index already exists, this method will detect it and skip creation.
     *
     * @param moviesCollection the movies collection to create the index on
     */
    private void createTextSearchIndex(MongoCollection<Document> moviesCollection) {
        try {
            // Check if any text search index already exists
            // MongoDB only allows one text index per collection, so we check for any text index
            // not just one with our specific name
            boolean textIndexExists = false;
            String existingTextIndexName = null;

            for (Document index : moviesCollection.listIndexes()) {
                Document key = index.get("key", Document.class);
                if (key != null && key.containsKey("_fts")) {
                    // _fts is the internal field MongoDB uses for text indexes
                    textIndexExists = true;
                    existingTextIndexName = index.getString("name");
                    logger.info("Text search index '{}' already exists on movies collection", existingTextIndexName);
                    break;
                }
            }

            if (!textIndexExists) {
                // Create compound text index on plot, title, and fullplot fields
                // The background option allows the index to be built without blocking other operations
                IndexOptions indexOptions = new IndexOptions()
                        .name(TEXT_INDEX_NAME)
                        .background(true);

                // Create the text index using field name constants from Movie.Fields
                // This makes the coupling between Movie class and index creation explicit
                // and allows IDE "Find Usages" to track dependencies
                moviesCollection.createIndex(
                    Indexes.compoundIndex(
                        Indexes.text(Movie.Fields.PLOT),
                        Indexes.text(Movie.Fields.TITLE),
                        Indexes.text(Movie.Fields.FULLPLOT)
                    ),
                    indexOptions
                );

                logger.info("Text search index '{}' created successfully for movies collection", TEXT_INDEX_NAME);
            }

        } catch (Exception e) {
            // Log error but don't fail - the application can still function without the index
            // (though text search queries will fail)
            logger.error("Could not create text search index: {}", e.getMessage());
            logger.warn("Text search functionality may not work without the index");
        }
    }

    /**
     * Creates a MongoDB Search index on the movies collection if it doesn't already exist.
     *
     * <p>This index enables MongoDB Search functionality across multiple fields:
     * <ul>
     *   <li>plot: Short movie description (phrase matching)</li>
     *   <li>fullplot: Full movie description (phrase matching)</li>
     *   <li>directors: Director names (fuzzy text matching)</li>
     *   <li>writers: Writer names (fuzzy text matching)</li>
     *   <li>cast: Actor names (fuzzy text matching)</li>
     * </ul>
     *
     * <p>This is different from the text search index - MongoDB Search provides more advanced
     * search capabilities including fuzzy matching, phrase search, and compound queries.
     *
     * @param moviesCollection the movies collection to create the index on
     */
    private void createMongoDBSearchIndex(MongoCollection<Document> moviesCollection) {
        try {
            // Check if the Search index already exists
            boolean indexExists = false;
            for (Document index : moviesCollection.listSearchIndexes()) {
                if (MONGODB_SEARCH_INDEX_NAME.equals(index.getString("name"))) {
                    indexExists = true;
                    logger.info("MongoDB Search index '{}' already exists", MONGODB_SEARCH_INDEX_NAME);
                    break;
                }
            }

            if (!indexExists) {
                // Define the MongoDB Search index specification
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
                Document createIndexCommand = new Document("createSearchIndexes", MOVIES_COLLECTION)
                        .append("indexes", java.util.Collections.singletonList(
                                new Document("name", MONGODB_SEARCH_INDEX_NAME)
                                        .append("definition", indexDefinition)
                        ));

                database.runCommand(createIndexCommand);

                logger.info("MongoDB Search index '{}' created successfully. Index may take a few moments to build.", MONGODB_SEARCH_INDEX_NAME);
                logger.info("MongoDB Search is now ready to use on the '{}' collection", MOVIES_COLLECTION);
            }

        } catch (Exception e) {
            logger.warn("Could not create MongoDB Search index: {}", e.getMessage());
            logger.warn("If you're using Atlas, the index may already exist or there may be a permissions issue.");
            logger.warn("Search endpoint (/api/movies/search) will not work without this index.");
        }
    }

    /**
     * Creates an index on the year field for the movies collection if it doesn't already exist.
     *
     * <p>This index improves performance for aggregation queries that filter by year,
     * such as the movies with comments aggregation.
     *
     * @param moviesCollection the movies collection to create the index on
     */
    private void createYearIndex(MongoCollection<Document> moviesCollection) {
        try {
            // Check if the year index already exists
            boolean indexExists = false;
            for (Document index : moviesCollection.listIndexes()) {
                if (YEAR_INDEX_NAME.equals(index.getString("name"))) {
                    indexExists = true;
                    logger.info("Year index '{}' already exists", YEAR_INDEX_NAME);
                    break;
                }
            }

            if (!indexExists) {
                IndexOptions indexOptions = new IndexOptions()
                        .name(YEAR_INDEX_NAME)
                        .background(true);

                moviesCollection.createIndex(
                    Indexes.ascending(Movie.Fields.YEAR),
                    indexOptions
                );

                logger.info("Year index '{}' created successfully for movies collection", YEAR_INDEX_NAME);
            }

        } catch (Exception e) {
            logger.error("Could not create year index: {}", e.getMessage());
            logger.warn("Aggregation queries filtering by year may be slower without the index");
        }
    }

    /**
     * Verifies the comments collection and creates necessary indexes.
     *
     * <p>This method creates an index on the movie_id field to improve $lookup performance
     * when joining movies with comments in aggregation pipelines.
     */
    private void verifyCommentsCollection() {
        MongoCollection<Document> commentsCollection = database.getCollection(COMMENTS_COLLECTION);

        // Check if collection has documents
        long count = commentsCollection.estimatedDocumentCount();

        logger.info("Comments collection found with {} documents", count);

        if (count == 0) {
            logger.warn(
                "Comments collection is empty. Please ensure sample_mflix data is loaded."
            );
        }

        // Create movie_id index for $lookup performance
        createMovieIdIndex(commentsCollection);
    }

    /**
     * Creates an index on the movie_id field for the comments collection if it doesn't already exist.
     *
     * <p>This index is critical for $lookup performance when joining movies with comments.
     * Without this index, the $lookup operation will perform a collection scan for each movie,
     * which can cause timeouts on large datasets.
     *
     * @param commentsCollection the comments collection to create the index on
     */
    private void createMovieIdIndex(MongoCollection<Document> commentsCollection) {
        try {
            // Check if the movie_id index already exists
            boolean indexExists = false;
            for (Document index : commentsCollection.listIndexes()) {
                if (MOVIE_ID_INDEX_NAME.equals(index.getString("name"))) {
                    indexExists = true;
                    logger.info("Movie ID index '{}' already exists", MOVIE_ID_INDEX_NAME);
                    break;
                }
            }

            if (!indexExists) {
                IndexOptions indexOptions = new IndexOptions()
                        .name(MOVIE_ID_INDEX_NAME)
                        .background(true);

                commentsCollection.createIndex(
                    Indexes.ascending("movie_id"),
                    indexOptions
                );

                logger.info("Movie ID index '{}' created successfully for comments collection", MOVIE_ID_INDEX_NAME);
            }

        } catch (Exception e) {
            logger.error("Could not create movie_id index: {}", e.getMessage());
            logger.warn("$lookup aggregations joining movies with comments may timeout without the index");
        }
    }

    /**
     * Verifies the embedded_movies collection and creates the vector search index.
     *
     * <p>The embedded_movies collection contains movie documents with plot embeddings
     * generated by the Voyage AI model. This method checks if the collection exists
     * and creates a vector search index for semantic similarity search.
     */
    private void verifyEmbeddedMoviesCollection() {
        MongoCollection<Document> embeddedMoviesCollection = database.getCollection(EMBEDDED_MOVIES_COLLECTION);

        // Check if collection has documents
        long count = embeddedMoviesCollection.estimatedDocumentCount();

        if (count == 0) {
            logger.warn(
                "Embedded movies collection is empty. Vector search functionality will not work. " +
                "Please ensure the embedded_movies collection is populated with plot embeddings."
            );
            return;
        }

        logger.info("Embedded movies collection found with {} documents", count);

        // Check if documents have the required embedding field
        Document sampleDoc = embeddedMoviesCollection.find().first();
        if (sampleDoc != null && !sampleDoc.containsKey("plot_embedding_voyage_3_large")) {
            logger.warn(
                "Documents in embedded_movies collection do not have 'plot_embedding_voyage_3_large' field. " +
                "Vector search functionality will not work. Please ensure the embedded_movies collection is populated with plot embeddings in the 'plot_embedding_voyage_3_large' field."
            );
            return;
        }

        // Create vector search index programmatically
        createVectorSearchIndex(embeddedMoviesCollection);
    }

    /**
     * Creates a vector search index on the embedded_movies collection if it doesn't already exist.
     *
     * <p>This method creates a vector search index named 'vector_index' for the
     * plot_embedding_voyage_3_large field with 2048 dimensions and cosine similarity.
     *
     * @param embeddedMoviesCollection the embedded_movies collection to create the index on
     */
    private void createVectorSearchIndex(MongoCollection<Document> embeddedMoviesCollection) {
        try {
            // Check if the vector search index already exists
            boolean indexExists = false;
            for (Document index : embeddedMoviesCollection.listSearchIndexes()) {
                if (VECTOR_INDEX_NAME.equals(index.getString("name"))) {
                    indexExists = true;
                    logger.info("Vector search index '{}' already exists", VECTOR_INDEX_NAME);
                    break;
                }
            }

            if (!indexExists) {
                // Define the vector search index specification
                // For vectorSearch type, use fields as an array with path, type, numDimensions, and similarity
                Document vectorFieldDefinition = new Document()
                        .append("type", "vector")
                        .append("path", "plot_embedding_voyage_3_large")
                        .append("numDimensions", 2048)
                        .append("similarity", "cosine");

                Document indexDefinition = new Document()
                        .append("fields", java.util.Collections.singletonList(vectorFieldDefinition));

                // Use the createSearchIndexes command
                Document createIndexCommand = new Document("createSearchIndexes", EMBEDDED_MOVIES_COLLECTION)
                        .append("indexes", java.util.Collections.singletonList(
                                new Document("name", VECTOR_INDEX_NAME)
                                        .append("type", "vectorSearch")
                                        .append("definition", indexDefinition)
                        ));

                // Execute the command
                database.runCommand(createIndexCommand);

                logger.info("Vector search index '{}' created successfully. Index may take a few moments to build.", VECTOR_INDEX_NAME);
                logger.info("Vector search is now ready to use on the '{}' collection", EMBEDDED_MOVIES_COLLECTION);
            }

        } catch (Exception e) {
            logger.error("Failed to create vector search index: {}", e.getMessage());
            logger.warn(
                "To manually create the vector search index, visit the Atlas UI and create an index named '{}' with:\n" +
                "  - Field: plot_embedding_voyage_3_large\n" +
                "  - Dimensions: 2048\n" +
                "  - Similarity: cosine\n" +
                "Visit: https://www.mongodb.com/docs/atlas/atlas-vector-search/create-index/",
                VECTOR_INDEX_NAME
            );
        }
    }
}
