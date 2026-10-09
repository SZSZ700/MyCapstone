@file:Suppress("PackageName")
package org.example.CapstoneProject.service
import org.example.CapstoneProject.repository.UserRepository
import org.springframework.stereotype.Service
import java.time.LocalDate
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
    // Returns today's calories value for a user.
    // Returns zero when no value or matching user exists.
    // ---------------------------------------------------------------------
    fun getCalories(username: String): CompletableFuture<Int> {
        // Determine the date for the application operation.
        val today = LocalDate.now()
        // Convert a missing stored value into the application's default value.
        return userRepository.getCalories(username, today)
            .thenApply { calories -> calories ?: 0 }
    }

    // ---------------------------------------------------------------------
    // Updates today's calories value of a user.
    // Validates the calorie value before persistence.
    // Returns true when the update succeeded.
    // Returns false when the calorie value is invalid or the user does not exist.
    // ---------------------------------------------------------------------
    fun updateCalories(username: String, calories: Int): CompletableFuture<Boolean> {
        // Reject calorie values outside the supported range.
        if (calories !in 0..20000) {
            return CompletableFuture.completedFuture(false)
        }

        // Determine the date for the application operation.
        val today = LocalDate.now()

        // Delegate persistence to the repository.
        return userRepository.updateCalories(username, today, calories)
    }
}