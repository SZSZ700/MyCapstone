@file:Suppress("PackageName")
package org.example.CapstoneProject.repository
import java.time.LocalDate
import java.util.concurrent.CompletableFuture

// -------------------------------------------------------------------------
// Defines persistence operations related to calorie data.
//
// This interface is independent of the database implementation.
// Business validation and application date selection are handled
// by the service layer.
// -------------------------------------------------------------------------
interface CaloriesRepository {

    // ---------------------------------------------------------------------
    // Returns the calories value for a user on the supplied date.
    //
    // Returns null when:
    // - the user does not exist
    // - no calorie record exists for the supplied date
    // ---------------------------------------------------------------------
    fun getCalories(username: String, date: LocalDate): CompletableFuture<Int?>

    // ---------------------------------------------------------------------
    // Updates the calories value for a user on the supplied date.
    //
    // Returns true when the update succeeded.
    // Returns false when no matching user exists.
    //
    // Calories validation belongs to the service layer.
    // ---------------------------------------------------------------------
    fun updateCalories(username: String, date: LocalDate, calories: Int): CompletableFuture<Boolean>
}