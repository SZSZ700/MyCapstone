@file:Suppress("PackageName")
package org.example.CapstoneProject.service
import org.example.CapstoneProject.repository.UserRepository
import org.springframework.stereotype.Service
import java.util.concurrent.CompletableFuture

// -------------------------------------------------------------------------
// Contains business logic related to global application statistics.
//
// This service depends only on the UserRepository interface and does not
// depend on a specific database or repository implementation.
// -------------------------------------------------------------------------
@Service
class StatisticsService(
    // Repository used to access user data for statistical operations.
    private val userRepository: UserRepository
) {

    // ---------------------------------------------------------------------
    // Returns the global BMI distribution for all users.
    // The repository retrieves the stored BMI values.
    // This service applies the BMI classification business rules.
    // The result contains the number of users in each BMI category.
    // ---------------------------------------------------------------------
    fun getBmiDistribution(): CompletableFuture<Map<String, Int>> {
        // Retrieve the stored BMI values.
        return userRepository.findAllBmiValues().thenApply { bmiValues ->
            var underweight = 0
            var normal = 0
            var overweight = 0
            var obese = 0

            // Apply the BMI classification business rules.
            for (bmi in bmiValues) {
                when {
                    bmi < 18.5 -> underweight++
                    bmi < 25.0 -> normal++
                    bmi < 30.0 -> overweight++
                    else -> obese++
                }
            }

            // Preserve the desired response order.
            linkedMapOf(
                "Underweight" to underweight,
                "Normal" to normal,
                "Overweight" to overweight,
                "Obese" to obese
            )
        }
    }
}