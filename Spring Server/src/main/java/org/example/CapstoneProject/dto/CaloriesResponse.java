package org.example.CapstoneProject.dto;

// -------------------------------------------------------------------------
// Represents the user's calories value returned by the REST API.
//
// This DTO preserves the same JSON structure currently expected
// by the Android client.
// -------------------------------------------------------------------------
@SuppressWarnings("unused")
public class CaloriesResponse {

    // Current calories value.
    private int calories;

    // ---------------------------------------------------------------------
    // Default constructor required for JSON serialization/deserialization.
    // ---------------------------------------------------------------------
    public CaloriesResponse() {
    }

    // ---------------------------------------------------------------------
    // Creates a calories response with the provided value.
    // ---------------------------------------------------------------------
    public CaloriesResponse(int calories) {
        this.calories = calories;
    }

    // ---------------------------------------------------------------------
    // Returns the calories value.
    // ---------------------------------------------------------------------
    public int getCalories() {
        return calories;
    }

    // ---------------------------------------------------------------------
    // Sets the calories value.
    // ---------------------------------------------------------------------
    public void setCalories(int calories) {
        this.calories = calories;
    }
}