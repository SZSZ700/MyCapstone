@file:Suppress("PackageName", "unused")
package org.example.CapstoneProject.repository
import org.json.JSONObject
import java.util.concurrent.CompletableFuture

// -------------------------------------------------------------------------
// Defines the operations that can be performed on water-related data.
//
// This interface is independent of the database implementation.
// The MongoDB implementation is provided separately.
// -------------------------------------------------------------------------
interface WaterRepository {
    // ---------------------------------------------------------------------
    // Adds a water amount to the user's water log for today.
    //
    // Returns true when the update succeeded.
    // Returns false when the user was not found or the update failed.
    // ---------------------------------------------------------------------
    fun updateWater(username: String, waterAmount: Int): CompletableFuture<Boolean>

    // ---------------------------------------------------------------------
    // Returns today's and yesterday's water totals for a user.
    //
    // Returns null when no matching user exists.
    // ---------------------------------------------------------------------
    fun getWater(username: String): CompletableFuture<JSONObject?>

    // ---------------------------------------------------------------------
    // Returns the user's water history for the requested number of days.
    //
    // Returns null when no matching user exists.
    // ---------------------------------------------------------------------
    fun getWaterHistoryMap(username: String, days: Int): CompletableFuture<Map<String, Long>?>

    // ---------------------------------------------------------------------
    // Returns the user's weekly water averages for the last four weeks.
    // ---------------------------------------------------------------------
    fun getWeeklyAverages(username: String): CompletableFuture<Map<String, Int>>

    // ---------------------------------------------------------------------
    // Returns the user's daily water goal.
    //
    // Returns the default goal when no value or user is found.
    // ---------------------------------------------------------------------
    fun getGoalMl(username: String): CompletableFuture<Int>

    // ---------------------------------------------------------------------
    // Updates the user's daily water goal.
    //
    // Returns true when the update succeeded.
    // Returns false when the value is invalid or the user was not found.
    // ---------------------------------------------------------------------
    fun updateGoalMl(username: String, goalMl: Int): CompletableFuture<Boolean>
}