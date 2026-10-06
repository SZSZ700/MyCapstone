@file:Suppress("PackageName")
package org.example.CapstoneProject.dto

// -------------------------------------------------------------------------
// Represents the user's water data returned by the REST API.
// This DTO preserves the same JSON structure currently expected
// by the Android client.
// -------------------------------------------------------------------------
@Suppress("unused")
data class WaterResponse(
    // Total water consumed today.
    var todayWater: Long = 0,
    // Total water consumed yesterday.
    var yesterdayWater: Long = 0
)