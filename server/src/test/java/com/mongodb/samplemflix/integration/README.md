# MongoDB Search Integration Tests

This directory contains integration tests for MongoDB Search functionality.

## Overview

The `MongoDBSearchIntegrationTest` class tests the MongoDB Search endpoints with a real MongoDB Atlas instance. These tests verify that:

1. The MongoDB Search index is created correctly
2. The index becomes ready for use (using polling)
3. The `/search` endpoint returns correct results
4. Pagination works correctly
5. Empty results are handled properly

## Requirements

These tests require:

- **MongoDB Atlas cluster** (not local MongoDB or Testcontainers)
- **MongoDB Search capability** enabled on the cluster
- **MONGODB_URI** environment variable pointing to your Atlas cluster
- **ENABLE_SEARCH_TESTS=true** environment variable to enable the tests

## Running the Tests

### Enable the Tests

By default, these tests are **disabled** to prevent accidental runs against production databases. To enable them:

```bash
export ENABLE_SEARCH_TESTS=true
```

### Set MongoDB URI

Make sure your `MONGODB_URI` environment variable points to a MongoDB Atlas cluster:

```bash
export MONGODB_URI="mongodb+srv://username:password@cluster.mongodb.net/sample_mflix?retryWrites=true&w=majority"
```

Or use a `.env` file in the `server/java-spring` directory:

```
MONGODB_URI=mongodb+srv://username:password@cluster.mongodb.net/sample_mflix?retryWrites=true&w=majority
```

### Run the Tests

```bash
# Run all integration tests
./mvnw test -Dtest=MongoDBSearchIntegrationTest

# Run a specific test
./mvnw test -Dtest=MongoDBSearchIntegrationTest#testSearchMoviesByPlot_Success
```

## How the Tests Work

### 1. Index Creation and Polling

The tests use a `@BeforeAll` method to:

1. Check if the `movieSearchIndex` already exists
2. If not, create it with the proper field mappings
3. Poll the index status every 5 seconds until it's "READY"
4. Wait up to 120 seconds (2 minutes) for the index to be ready
5. Throw an exception if the index doesn't become ready in time

### 2. Test Data Setup

The tests create temporary test movies with known plot content:

- "An epic space adventure across the galaxy"
- "A detective solves a mysterious crime"
- "Heroes embark on a dangerous adventure"

These movies are used to verify search functionality.

### 3. Test Cleanup

The `@AfterAll` method removes the test movies after all tests complete. The search index is **not** deleted because:

- It may be used by other tests
- It takes time to recreate
- It's safe to leave in the database

## Test Cases

### testSearchMoviesByPlot_Success
Verifies that searching for "space adventure" returns movies with that phrase in the plot.

### testSearchMoviesByPlot_NoResults
Verifies that searching for a non-existent phrase returns an empty list.

### testSearchMoviesByPlot_WithLimit
Verifies that the `limit` parameter correctly limits the number of results.

### testSearchMoviesByPlot_WithPagination
Verifies that the `skip` parameter works for pagination and returns different results on different pages.

## Troubleshooting

### Tests are Skipped

If you see "Skipping Search tests - ENABLE_SEARCH_TESTS not set", make sure you've set the environment variable:

```bash
export ENABLE_SEARCH_TESTS=true
```

### Index Creation Timeout

If the tests fail with "Search index did not become ready within 120 seconds":

1. Check that your cluster has MongoDB Search enabled
2. Verify you're using a MongoDB Atlas cluster (not local MongoDB)
3. Check the Atlas UI to see if the index is being created
4. Increase `MAX_INDEX_WAIT_SECONDS` if needed

### Connection Errors

If you get connection errors:

1. Verify your `MONGODB_URI` is correct
2. Check that your IP address is whitelisted in Atlas
3. Verify your database user credentials are correct

## Notes

- These tests use `@TestInstance(TestInstance.Lifecycle.PER_CLASS)` to allow `@BeforeAll` and `@AfterAll` methods to be non-static
- The tests use `@ActiveProfiles("test")` to load test-specific configuration from `application-test.properties`
- The search index is shared across all tests in the class
- Test movies are created once and cleaned up after all tests complete
