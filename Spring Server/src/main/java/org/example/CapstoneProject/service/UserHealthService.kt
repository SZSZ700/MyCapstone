@file:Suppress("PackageName")
package org.example.CapstoneProject.service
import org.example.CapstoneProject.repository.UserRepository
import org.springframework.stereotype.Service
import java.util.concurrent.CompletableFuture

// -------------------------------------------------------------------------
// Contains business logic related to user health data.
// This service handles BMI and calorie-related operations.
// It depends only on the UserRepository interface and does not depend
// on a specific database or repository implementation.
// -------------------------------------------------------------------------
@Service
class UserHealthService(
    // Repository used to access user data.
    private val userRepository: UserRepository
) {

    // ---------------------------------------------------------------------
    // Updates the BMI value of a user.
    // ---------------------------------------------------------------------
    fun updateBmi(username: String, bmi: Double): CompletableFuture<Boolean> {
        // Delegate the database operation to the repository.
        return userRepository.updateBmi(username, bmi)
    }

    // ---------------------------------------------------------------------
    // Returns the calories value of a user.
    // ---------------------------------------------------------------------
    fun getCalories(username: String): CompletableFuture<Int> {
        // Delegate the database operation to the repository.
        return userRepository.getCalories(username)
    }

    // ---------------------------------------------------------------------
    // Updates the calories value of a user.
    // ---------------------------------------------------------------------
    fun updateCalories(username: String, calories: Int): CompletableFuture<Boolean> {
        // Delegate the database operation to the repository.
        return userRepository.updateCalories(username, calories)
    }
}