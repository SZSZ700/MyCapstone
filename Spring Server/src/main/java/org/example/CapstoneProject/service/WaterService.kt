@file:Suppress("PackageName")
package org.example.CapstoneProject.service
import org.example.CapstoneProject.repository.WaterRepository
import org.json.JSONObject
import org.springframework.stereotype.Service
import java.util.concurrent.CompletableFuture

// -------------------------------------------------------------------------
// Contains business logic related to water operations.
//
// This service depends only on the WaterRepository interface and does not
// depend on a specific database or repository implementation.
//
// Database-specific behavior such as transactions and concurrency handling
// remains inside the repository implementation.
// -------------------------------------------------------------------------
@Service
class WaterService(
    // Repository used to access water-related data.
    private val waterRepository: WaterRepository
) {

    // ---------------------------------------------------------------------
    // Adds one water drink for the user.
    //
    // Transaction and concurrency handling are performed
    // by the repository implementation.
    //
    // Returns true when the update succeeded.
    // Returns false when the user was not found or the value is invalid.
    // ---------------------------------------------------------------------
    fun updateWater(username: String, waterAmount: Int): CompletableFuture<Boolean> {
        // Delegate the data operation to the repository layer.
        return waterRepository.updateWater(username, waterAmount)
    }

    // ---------------------------------------------------------------------
    // Returns today's and yesterday's water totals for a user.
    //
    // Returns null when no matching user exists.
    // ---------------------------------------------------------------------
    fun getWater(username: String): CompletableFuture<JSONObject?> {
        // Delegate the data operation to the repository layer.
        return waterRepository.getWater(username)
    }

    // ---------------------------------------------------------------------
    // Returns the user's water history for the requested number of days.
    //
    // Returns null when no matching user exists.
    // ---------------------------------------------------------------------
    fun getWaterHistoryMap(username: String, days: Int): CompletableFuture<Map<String, Long>?> {
        // Delegate the data operation to the repository layer.
        return waterRepository.getWaterHistoryMap(username, days)
    }

    // ---------------------------------------------------------------------
    // Returns the user's weekly water averages for the last four weeks.
    // ---------------------------------------------------------------------
    fun getWeeklyAverages(username: String): CompletableFuture<Map<String, Int>> {
        // Delegate the data operation to the repository layer.
        return waterRepository.getWeeklyAverages(username)
    }

    // ---------------------------------------------------------------------
    // Returns the user's current daily water goal.
    // ---------------------------------------------------------------------
    fun getGoalMl(username: String): CompletableFuture<Int> {
        // Delegate the data operation to the repository layer.
        return waterRepository.getGoalMl(username)
    }

    // ---------------------------------------------------------------------
    // Updates today's water goal.
    //
    // Transaction and concurrency handling are performed
    // by the repository implementation.
    //
    // Returns true when the update succeeded.
    // Returns false when the value is invalid or the user was not found.
    // ---------------------------------------------------------------------
    fun updateGoalMl(username: String, goalMl: Int): CompletableFuture<Boolean> {
        // Delegate the data operation to the repository layer.
        return waterRepository.updateGoalMl(username, goalMl)
    }
}