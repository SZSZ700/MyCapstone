@file:Suppress("PackageName")
package org.example.CapstoneProject.dto

// -------------------------------------------------------------------------
// Represents the result of updating a user's daily water goal.
// This DTO preserves the same JSON structure currently returned
// by the REST API and expected by the Android client.
// -------------------------------------------------------------------------
@Suppress("unused")
data class GoalUpdateResponse(
    // Status describing the result of the update operation.
    var status: String = ""
)