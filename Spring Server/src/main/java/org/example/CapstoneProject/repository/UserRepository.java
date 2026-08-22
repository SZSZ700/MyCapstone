package org.example.CapstoneProject.repository;

import org.example.CapstoneProject.model.User;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

// -------------------------------------------------------------------------
// Defines the operations that can be performed on user data.
//
// This interface does not know that Firebase exists.
// The Firebase implementation will be created separately later.
// -------------------------------------------------------------------------
@SuppressWarnings("unused")
public interface UserRepository {

    // ---------------------------------------------------------------------
    // Finds a user by username.
    //
    // Returns the user when found.
    // Returns null when no matching user exists.
    // ---------------------------------------------------------------------
    CompletableFuture<User> findByUsername(String username);

    // ---------------------------------------------------------------------
    // Returns all users stored in the database.
    // ---------------------------------------------------------------------
    CompletableFuture<List<User>> findAll();

    // ---------------------------------------------------------------------
    // Checks whether a user exists by username.
    // ---------------------------------------------------------------------
    CompletableFuture<Boolean> existsByUsername(String username);

    // ---------------------------------------------------------------------
    // Deletes a user by username.
    //
    // Returns true when the user was found and deleted.
    // Returns false when no matching user exists.
    // ---------------------------------------------------------------------
    CompletableFuture<Boolean> deleteByUsername(String username);

    // -------------------------------------------------------------------------
    // Updates an existing user by username.
    //
    // Returns the updated user when the operation succeeds.
    // Returns null when no matching user exists.
    // -------------------------------------------------------------------------
    CompletableFuture<User> updateByUsername(String username, User updatedUser);

    // ---------------------------------------------------------------------
    // Partially updates an existing user by username.
    //
    // Returns the updated user when the user was found.
    // Returns null when no matching user exists.
    // ---------------------------------------------------------------------
    CompletableFuture<User> patchByUsername(String username, Map<String, Object> updates);

    // ---------------------------------------------------------------------
    // Creates a new user.
    //
    // Returns true when the user was created successfully.
    // Returns false when the username is invalid or already exists.
    // ---------------------------------------------------------------------
    CompletableFuture<Boolean> create(User user);

    // ---------------------------------------------------------------------
    // Inserts a new user into the database.
    //
    // Returns a success message when the user was inserted successfully.
    // Returns an error message when the database write failed.
    // ---------------------------------------------------------------------
    CompletableFuture<String> insert(User user);

    // ---------------------------------------------------------------------
    // Returns all users that match the provided username.
    // ---------------------------------------------------------------------
    CompletableFuture<List<User>> findAllByUsername(String username);

    // ---------------------------------------------------------------------
    // Updates the BMI value of a user.
    //
    // Returns true when the update succeeded.
    // Returns false when no matching user exists.
    // ---------------------------------------------------------------------
    CompletableFuture<Boolean> updateBmi(String username, double bmi);

    // ---------------------------------------------------------------------
    // Returns the calories value of a user.
    //
    // Returns zero when no value or matching user exists.
    // ---------------------------------------------------------------------
    CompletableFuture<Integer> getCalories(String username);

    // ---------------------------------------------------------------------
    // Updates the calories value of a user.
    //
    // Returns true when the update succeeded.
    // Returns false when the value is invalid or no matching user exists.
    // ---------------------------------------------------------------------
    CompletableFuture<Boolean> updateCalories(String username, int calories);

    // ---------------------------------------------------------------------
    // Returns the global BMI distribution for all users.
    //
    // The result contains the number of users in each BMI category.
    // ---------------------------------------------------------------------
    CompletableFuture<Map<String, Integer>> getBmiDistribution();
}