package org.example.CapstoneProject.dto;

// -------------------------------------------------------------------------
// Represents the user's water data returned by the REST API.
//
// This DTO preserves the same JSON structure currently expected
// by the Android client.
// -------------------------------------------------------------------------
@SuppressWarnings("unused")
public class WaterResponse {

    // Total water consumed today.
    private long todayWater;

    // Total water consumed yesterday.
    private long yesterdayWater;

    // ---------------------------------------------------------------------
    // Default constructor required for JSON serialization/deserialization.
    // ---------------------------------------------------------------------
    public WaterResponse() {
    }

    // ---------------------------------------------------------------------
    // Creates a complete water response.
    // ---------------------------------------------------------------------
    public WaterResponse(long todayWater, long yesterdayWater) {
        this.todayWater = todayWater;
        this.yesterdayWater = yesterdayWater;
    }

    // ---------------------------------------------------------------------
    // Returns today's water total.
    // ---------------------------------------------------------------------
    public long getTodayWater() {
        return todayWater;
    }

    // ---------------------------------------------------------------------
    // Sets today's water total.
    // ---------------------------------------------------------------------
    public void setTodayWater(long todayWater) {
        this.todayWater = todayWater;
    }

    // ---------------------------------------------------------------------
    // Returns yesterday's water total.
    // ---------------------------------------------------------------------
    public long getYesterdayWater() {
        return yesterdayWater;
    }

    // ---------------------------------------------------------------------
    // Sets yesterday's water total.
    // ---------------------------------------------------------------------
    public void setYesterdayWater(long yesterdayWater) {
        this.yesterdayWater = yesterdayWater;
    }
}