@file:Suppress("PackageName", "unused")
package org.example.CapstoneProject.repository
import java.time.LocalDate
import java.util.Date
import java.util.concurrent.CompletableFuture

// -------------------------------------------------------------------------
// Represents one water record returned by the repository.
//
// This is a database-independent representation used by the service layer.
// MongoDB-specific types such as Document and ObjectId do not leave
// the repository implementation.
// -------------------------------------------------------------------------
data class WaterRecordData(
    val recordedAt: Date,
    val amountMl: Int
)

// -------------------------------------------------------------------------
// Defines the persistence operations that can be performed
// on water-related data.
//
// This interface is independent of the database implementation.
// The MongoDB implementation is provided separately.
//
// Business rules such as:
// - water amount validation
// - current-date selection
// - daily totals
// - missing-day values
// - weekly averages
// - default goal values
// - goal validation
//
// are handled by the service layer.
// -------------------------------------------------------------------------
interface WaterRepository {

    // ---------------------------------------------------------------------
    // Stores one water drink for a user at the supplied timestamp.
    //
    // The service layer validates the amount and decides the timestamp.
    //
    // Returns true when the record was stored successfully.
    // Returns false when no matching user exists.
    // ---------------------------------------------------------------------
    fun updateWater(username: String, waterAmount: Int, recordedAt: Date): CompletableFuture<Boolean>

    // ---------------------------------------------------------------------
    // Returns the user's stored water records inside the supplied
    // time range.
    //
    // startInclusive <= recordedAt < endExclusive
    //
    // The repository returns only stored records.
    // Daily totals, missing-day values and statistical calculations
    // are handled by the service layer.
    //
    // Returns null when no matching user exists.
    // ---------------------------------------------------------------------
    fun getWaterRecords(username: String, startInclusive: Date, endExclusive: Date): CompletableFuture<List<WaterRecordData>?>

    // ---------------------------------------------------------------------
    // Returns the user's most recently stored daily water goal.
    //
    // Returns null when:
    // - no matching user exists
    // - no goal has been stored
    //
    // The application default is handled by the service layer.
    // ---------------------------------------------------------------------
    fun getGoalMl(username: String): CompletableFuture<Int?>

    // ---------------------------------------------------------------------
    // Stores the user's water goal for the supplied date.
    //
    // The service layer validates the goal and decides which date
    // the operation belongs to.
    //
    // Returns true when the update succeeded.
    // Returns false when no matching user exists.
    // ---------------------------------------------------------------------
    fun updateGoalMl(username: String, recordDate: LocalDate, goalMl: Int): CompletableFuture<Boolean>
}