package org.example.CapstoneProject.dto;

// -------------------------------------------------------------------------
// Represents the result of updating a user's daily water goal.
//
// This DTO preserves the same JSON structure currently returned
// by the REST API and expected by the Android client.
// -------------------------------------------------------------------------
@SuppressWarnings("unused")
public class GoalUpdateResponse {

    // Status describing the result of the update operation.
    private String status;

    // ---------------------------------------------------------------------
    // Default constructor required for JSON serialization/deserialization.
    // ---------------------------------------------------------------------
    public GoalUpdateResponse() {
    }

    // ---------------------------------------------------------------------
    // Creates a goal update response with the provided status.
    // ---------------------------------------------------------------------
    public GoalUpdateResponse(String status) {
        this.status = status;
    }

    // ---------------------------------------------------------------------
    // Returns the update status.
    // ---------------------------------------------------------------------
    public String getStatus() {
        return status;
    }

    // ---------------------------------------------------------------------
    // Sets the update status.
    // ---------------------------------------------------------------------
    public void setStatus(String status) {
        this.status = status;
    }
}