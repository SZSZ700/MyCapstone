@file:Suppress("PackageName")
package org.example.CapstoneProject.dto

// -------------------------------------------------------------------------
// Represents the user's calories value returned by the REST API.
//
// This DTO preserves the same JSON structure currently expected
// by the Android client.
// -------------------------------------------------------------------------
@Suppress("unused")
data class CaloriesResponse(
    // Current calories value.
    var calories: Int = 0
)