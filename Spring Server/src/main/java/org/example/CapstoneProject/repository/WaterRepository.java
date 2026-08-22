package org.example.CapstoneProject.repository;
import org.json.JSONObject;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

// -------------------------------------------------------------------------
// Defines the operations that can be performed on water-related data.
//
// This interface does not know that Firebase exists.
// The Firebase implementation will be created separately.
// -------------------------------------------------------------------------
public interface WaterRepository {
    // ---------------------------------------------------------------------
    // Adds a water amount to the user's water log for today.
    //
    // Returns true when the update succeeded.
    // Returns false when the user was not found or the update failed.
    // ---------------------------------------------------------------------
    CompletableFuture<Boolean> updateWater(String username, int waterAmount);

    // ---------------------------------------------------------------------
    // Returns today's and yesterday's water totals for a user.
    //
    // Returns null when no matching user exists.
    // ---------------------------------------------------------------------
    CompletableFuture<JSONObject> getWater(String username);

    // ---------------------------------------------------------------------
    // Returns the user's water history for the requested number of days.
    //
    // Returns null when no matching user exists.
    // ---------------------------------------------------------------------
    CompletableFuture<Map<String, Long>> getWaterHistoryMap(String username, int days);

    // ---------------------------------------------------------------------
    // Returns the user's weekly water averages for the last four weeks.
    // ---------------------------------------------------------------------
    CompletableFuture<Map<String, Integer>> getWeeklyAverages(String username);

    // ---------------------------------------------------------------------
    // Returns the user's daily water goal.
    //
    // Returns the default goal when no value or user is found.
    // ---------------------------------------------------------------------
    CompletableFuture<Integer> getGoalMl(String username);

    // ---------------------------------------------------------------------
    // Updates the user's daily water goal.
    //
    // Returns true when the update succeeded.
    // Returns false when the value is invalid or the user was not found.
    // ---------------------------------------------------------------------
    CompletableFuture<Boolean> updateGoalMl(String username, int goalMl);
}