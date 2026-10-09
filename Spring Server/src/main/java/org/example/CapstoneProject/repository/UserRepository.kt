@file:Suppress("PackageName")
package org.example.CapstoneProject.repository
import org.example.CapstoneProject.model.User
import java.time.LocalDate
import java.util.concurrent.CompletableFuture

// -------------------------------------------------------------------------
// Defines the operations that can be performed on user data.
// This interface is independent of the database implementation.
// The MongoDB implementation is provided separately.
// -------------------------------------------------------------------------
@Suppress("unused")
interface UserRepository {
    // ---------------------------------------------------------------------
    // Finds a user by username.
    // Returns the user when found.
    // Returns null when no matching user exists.
    // ---------------------------------------------------------------------
    fun findByUsername(username: String): CompletableFuture<User?>

    // ---------------------------------------------------------------------
    // Returns all users stored in the database.
    // ---------------------------------------------------------------------
    fun findAll(): CompletableFuture<List<User>>

    // ---------------------------------------------------------------------
    // Checks whether a user exists by username.
    // ---------------------------------------------------------------------
    fun existsByUsername(username: String): CompletableFuture<Boolean>

    // ---------------------------------------------------------------------
    // Deletes a user by username.
    // Returns true when the user was found and deleted.
    // Returns false when no matching user exists.
    // ---------------------------------------------------------------------
    fun deleteByUsername(username: String): CompletableFuture<Boolean>

    // ---------------------------------------------------------------------
    // Partially updates an existing user by username.
    // MutableMap is used here to preserve the exact Java signature
    // Map<String, Object> used by the existing repository implementation.
    // Returns the updated user when the user was found.
    // Returns null when no matching user exists.
    // ---------------------------------------------------------------------
    fun patchByUsername(username: String, updates: MutableMap<String, Any>): CompletableFuture<User?>

    // ---------------------------------------------------------------------
    // Creates a new user.
    // Returns true when the user was created successfully.
    // Returns false when MongoDB rejects the creation because the username
    // already exists.
    // Input validation belongs to the service layer.
    // ---------------------------------------------------------------------
    fun create(user: User): CompletableFuture<Boolean>

    // ---------------------------------------------------------------------
    // Updates the BMI value of a user.
    // Returns true when the update succeeded.
    // Returns false when no matching user exists.
    // ---------------------------------------------------------------------
    fun updateBmi(username: String, bmi: Double): CompletableFuture<Boolean>

    // ---------------------------------------------------------------------
    // Returns the calories value for a user on the supplied date.
    // Returns null when the user or calorie record does not exist.
    // ---------------------------------------------------------------------
    fun getCalories(username: String, date: LocalDate): CompletableFuture<Int?>

    // ---------------------------------------------------------------------
    // Updates the calories value for a user on the supplied date.
    // Returns true when the update succeeded.
    // Returns false when no matching user exists.
    // ---------------------------------------------------------------------
    fun updateCalories(username: String, date: LocalDate, calories: Int): CompletableFuture<Boolean>

    // ---------------------------------------------------------------------
    // Returns all stored BMI values.
    // The repository does not classify the values into BMI categories.
    // ---------------------------------------------------------------------
    fun findAllBmiValues(): CompletableFuture<List<Double>>
}