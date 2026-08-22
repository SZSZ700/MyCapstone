package org.example.CapstoneProject.dto;

// -------------------------------------------------------------------------
// Represents the user's daily water goal returned by the REST API.
//
// This DTO preserves the same JSON structure currently expected
// by the Android client.
// -------------------------------------------------------------------------
@SuppressWarnings("unused")
public class GoalResponse {

    // Daily water goal in milliliters.
    private int goalMl;

    // ---------------------------------------------------------------------
    // Default constructor required for JSON serialization/deserialization.
    // ---------------------------------------------------------------------
    public GoalResponse() {
    }

    // ---------------------------------------------------------------------
    // Creates a goal response with the provided value.
    // ---------------------------------------------------------------------
    public GoalResponse(int goalMl) {
        this.goalMl = goalMl;
    }

    // ---------------------------------------------------------------------
    // Returns the daily water goal.
    // ---------------------------------------------------------------------
    public int getGoalMl() {
        return goalMl;
    }

    // ---------------------------------------------------------------------
    // Sets the daily water goal.
    // ---------------------------------------------------------------------
    public void setGoalMl(int goalMl) {
        this.goalMl = goalMl;
    }
}