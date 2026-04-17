# MongoDB Search Integration Tests

This directory contains integration tests for MongoDB Search functionality.

## Overview

The `MongoDBSearchIntegrationTest` class tests the MongoDB Search endpoints with a real MongoDB instance. These tests verify that:

1. The MongoDB Search index is created correctly
2. The index becomes ready for use (using polling)
3. The `/search` endpoint returns correct results
4. Pagination works correctly
5. Empty results are handled properly
6. BSON DateTime values at midnight UTC round-trip correctly to `LocalDate` without date shift across JVM timezones

## Requirements

- **Docker** must be running (for the local Atlas container)
- Alternatively, set `MONGODB_URI` to use an external MongoDB Atlas cluster instead of Docker

By default, tests spin up a `MongoDBAtlasLocalContainer` via Testcontainers, which provides a local Atlas environment with full Search support. No external cluster or special environment variables are needed.

If neither Docker nor `MONGODB_URI` is available, the tests will fail.

## Running the Tests

### Default (local Atlas container via Docker)

Just run the tests — Docker handles the rest:

```bash
# Run all integration tests
./mvnw test -Dtest=MongoDBSearchIntegrationTest

# Run a specific test
./mvnw test -Dtest=MongoDBSearchIntegrationTest#testSearchMoviesByPlot_Success
```

### External Atlas Cluster (optional)

To run against an external MongoDB Atlas cluster instead of Docker, set the `MONGODB_URI` environment variable:

```bash
export MONGODB_URI="mongodb+srv://username:password@cluster.mongodb.net/sample_mflix?retryWrites=true&w=majority"
```

Or use a `.env` file in the `server/java-spring` directory:

```
MONGODB_URI="mongodb+srv://username:password@cluster.mongodb.net/sample_mflix?retryWrites=true&w=majority"
```

When `MONGODB_URI` is set, no Docker container is started.

## How the Tests Work

### Test Configuration

`MongoDBTestContainersConfig` is a `@TestConfiguration` that conditionally starts a `MongoDBAtlasLocalContainer`:

- If `spring.data.mongodb.uri` is empty or absent, it starts a local Atlas container and registers its connection string
- If `spring.data.mongodb.uri` is already set (via `MONGODB_URI` env var), no container is started

### Index Creation and Polling

The tests use a `@BeforeAll` method to:

1. Check if the `movieSearchIndex` already exists
2. If not, create it with the proper field mappings
3. Poll the index status every 5 seconds until it's "READY"
4. Wait up to 120 seconds (2 minutes) for the index to be ready
5. Throw an exception if the index doesn't become ready in time
6. Wait an additional 10 seconds for asynchronous document indexing

### Test Data Setup

The tests create temporary test movies with known plot content and `released` dates stored as BSON DateTime at midnight UTC:

| Title | Plot | Released (UTC) |
|---|---|---|
| Test Space Adventure | An epic space adventure across the galaxy | 2024-01-01 |
| Test Mystery Movie | A detective solves a mysterious crime | 2024-03-31 |
| Test Adventure Quest | Heroes embark on a dangerous adventure | 2024-12-31 |

These movies are used to verify both search functionality and correct `LocalDate` round-tripping of the `released` field.

### Test Cleanup

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

### testReleasedFieldRoundTrip_NoDateShift
Verifies that BSON DateTime values at midnight UTC are read as the correct `LocalDate` without date shift. The test temporarily switches the JVM default timezone through `America/New_York`, `America/Los_Angeles`, `Asia/Tokyo`, `Europe/London`, and `Pacific/Auckland` to ensure the native `LocalDateCodec` always interprets BSON DateTime in UTC.

## Troubleshooting

### Index Creation Timeout

If the tests fail with "Search index did not become ready within 120 seconds":

1. Ensure Docker is running and has enough resources
2. If using an external cluster, check that it has MongoDB Search enabled
3. Check the Atlas UI to see if the index is being created
4. Increase `MAX_INDEX_WAIT_SECONDS` if needed

### Connection Errors

If you get connection errors:

1. Verify Docker is running (`docker ps`)
2. If using an external Atlas cluster, verify your `MONGODB_URI` is correct
3. Check that your IP address is whitelisted in Atlas (for external clusters)

## Notes

- These tests use `@TestInstance(TestInstance.Lifecycle.PER_CLASS)` to allow `@BeforeAll` and `@AfterAll` methods to be non-static
- The tests use `@ActiveProfiles("test")` to load test-specific configuration from `application-test.properties`
- The search index is shared across all tests in the class
- Test movies are created once and cleaned up after all tests complete
