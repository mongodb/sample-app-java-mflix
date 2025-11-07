package com.mongodb.samplemflix.repository;

import com.mongodb.samplemflix.model.Movie;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

/**
 * Spring Data MongoDB repository for movie data access.
 *
 * <p>This repository extends MongoRepository which provides:
 * - Basic CRUD operations (save, findById, findAll, delete, etc.)
 * - Pagination and sorting support
 * - Query derivation from method names
 * - Custom query support via @Query annotation
 *
 * <p>For complex queries not supported by Spring Data, you can inject MongoTemplate
 * in the service layer.
 */
@Repository
public interface MovieRepository extends MongoRepository<Movie, ObjectId> {

    // Spring Data MongoDB provides these methods automatically:
    // - save(Movie movie) - insert or update
    // - saveAll(Iterable<Movie> movies) - batch insert/update
    // - findById(ObjectId id) - find by ID
    // - findAll() - find all documents
    // - findAll(Pageable pageable) - find with pagination
    // - deleteById(ObjectId id) - delete by ID
    // - delete(Movie movie) - delete entity
    // - count() - count all documents
    // - existsById(ObjectId id) - check if exists

    // Custom query methods can be added here using method name conventions:
    // Example: List<Movie> findByGenresContaining(String genre);
    // Example: List<Movie> findByYearBetween(Integer startYear, Integer endYear);
}
