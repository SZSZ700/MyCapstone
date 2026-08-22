package org.example.CapstoneProject.service;

import org.example.CapstoneProject.repository.WaterRepository;
import org.json.JSONObject;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

// -------------------------------------------------------------------------
// Contains business logic related to water operations.
//
// This service depends on WaterRepository and does not know that Firebase
// is used as the database.
// -------------------------------------------------------------------------
@Service
public class WaterService {

    // Repository used to access water-related data.
    private final WaterRepository waterRepository;

    // ---------------------------------------------------------------------
    // Builds the service using constructor injection.
    // ---------------------------------------------------------------------
    public WaterService(WaterRepository waterRepository) {
        // Store the injected repository.
        this.waterRepository = waterRepository;
    }

    // ---------------------------------------------------------------------
    // Adds a water amount to the user's water log for today.
    //
    // Returns true when the update succeeded.
    // Returns false when the user was not found or the update failed.
    // ---------------------------------------------------------------------
    public CompletableFuture<Boolean> updateWater(String username, int waterAmount) {
        // Delegate the database operation to the repository.
        return waterRepository.updateWater(username, waterAmount);
    }

    // ---------------------------------------------------------------------
    // Returns today's and yesterday's water totals for a user.
    //
    // Returns null when no matching user exists.
    // ---------------------------------------------------------------------
    public CompletableFuture<JSONObject> getWater(String username) {
        // Delegate the database operation to the repository.
        return waterRepository.getWater(username);
    }

    // ---------------------------------------------------------------------
    // Returns the user's water history for the requested number of days.
    //
    // Returns null when no matching user exists.
    // ---------------------------------------------------------------------
    public CompletableFuture<Map<String, Long>> getWaterHistoryMap(String username, int days) {
        // Delegate the database operation to the repository.
        return waterRepository.getWaterHistoryMap(username, days);
    }

    // ---------------------------------------------------------------------
    // Returns the user's weekly water averages for the last four weeks.
    // ---------------------------------------------------------------------
    public CompletableFuture<Map<String, Integer>> getWeeklyAverages(String username) {
        // Delegate the database operation to the repository.
        return waterRepository.getWeeklyAverages(username);
    }

    // ---------------------------------------------------------------------
    // Returns the user's daily water goal.
    // ---------------------------------------------------------------------
    public CompletableFuture<Integer> getGoalMl(String username) {
        // Delegate the database operation to the repository.
        return waterRepository.getGoalMl(username);
    }

    // ---------------------------------------------------------------------
    // Updates the user's daily water goal.
    //
    // Returns true when the update succeeded.
    // Returns false when the value is invalid or the user was not found.
    // ---------------------------------------------------------------------
    public CompletableFuture<Boolean> updateGoalMl(String username, int goalMl) {
        // Delegate the database operation to the repository.
        return waterRepository.updateGoalMl(username, goalMl);
    }
}