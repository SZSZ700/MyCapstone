@file:Suppress("PackageName")
package org.example.CapstoneProject.dto

// -------------------------------------------------------------------------
// Represents the user's daily water goal returned by the REST API.
// This DTO preserves the same JSON structure currently expected
// by the Android client.
// -------------------------------------------------------------------------
@Suppress("unused")
data class GoalResponse(
    // Daily water goal in milliliters.
    var goalMl: Int = 0
)