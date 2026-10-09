@file:Suppress("PackageName", "DestructuringDeclaration")
package org.example.CapstoneProject.service
import org.example.CapstoneProject.repository.WaterRepository
import org.json.JSONObject
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.ZoneId
import java.util.Date
import java.util.concurrent.CompletableFuture

// Default water goal used by the application when no goal has been stored.
private const val DEFAULT_GOAL_ML = 3000
// Supported water-goal range.
private const val MIN_GOAL_ML = 500
private const val MAX_GOAL_ML = 10000

// -------------------------------------------------------------------------
// Contains business logic related to water operations.
//
// This service depends only on the WaterRepository interface and does not
// depend on a specific database or repository implementation.
//
// This layer is responsible for:
// - input validation
// - current-date decisions
// - daily totals
// - missing-day values
// - weekly buckets
// - weekly averages
// - default goal behavior
// - response construction
//
// Database-specific behavior such as MongoDB queries, transactions,
// ObjectId handling and document operations remains inside
// the repository implementation.
// -------------------------------------------------------------------------
@Service
class WaterService(
    // Repository used to access water-related data.
    private val waterRepository: WaterRepository
) {

    // Application timezone used when converting between calendar dates
    // and stored timestamps.
    private val zoneId: ZoneId = ZoneId.systemDefault()

    // ---------------------------------------------------------------------
    // Adds one water drink for the user.
    //
    // The service validates the amount and decides when the drink
    // was recorded.
    //
    // Transaction and concurrency handling are performed
    // by the repository implementation.
    //
    // Returns true when the update succeeded.
    // Returns false when the amount is invalid or the user was not found.
    // ---------------------------------------------------------------------
    fun updateWater(username: String, waterAmount: Int): CompletableFuture<Boolean> {
        // Reject invalid drink amounts.
        if (waterAmount <= 0) {
            return CompletableFuture.completedFuture(false)
        }
        // Decide the timestamp for this application operation.
        val recordedAt = Date()
        // Delegate persistence to the repository.
        return waterRepository.updateWater(username, waterAmount, recordedAt)
    }

    // ---------------------------------------------------------------------
    // Returns today's and yesterday's water totals for a user.
    //
    // The repository returns raw stored water records.
    // This service decides which records belong to today and yesterday
    // and calculates the final totals.
    //
    // Returns null when no matching user exists.
    // ---------------------------------------------------------------------
    fun getWater(username: String): CompletableFuture<JSONObject?> {
        // Determine today's calendar date.
        val today = LocalDate.now(zoneId)
        // Determine yesterday's calendar date.
        val yesterday = today.minusDays(1)

        // Read records starting from the beginning of yesterday.
        val start = startOfDay(yesterday)
        // Stop before the beginning of tomorrow.
        val end = startOfDay(today.plusDays(1))

        // Retrieve only the stored records from the repository.
        return waterRepository.getWaterRecords(username, start, end)
            .thenApply { records ->
            // No matching user exists.
            if (records == null) { null }
            else {
                var todayWater = 0L
                var yesterdayWater = 0L

                // Calculate today's and yesterday's totals.
                for (record in records) {
                    val recordDate = toLocalDate(record.recordedAt)

                    if (recordDate == today) {
                        todayWater += record.amountMl.toLong()
                    } else if (recordDate == yesterday) {
                        yesterdayWater += record.amountMl.toLong()
                    }
                }

                // Build the response expected by the existing application.
                JSONObject().apply {
                    put("todayWater", todayWater)
                    put("yesterdayWater", yesterdayWater)
                }
            }
        }
    }

    // ---------------------------------------------------------------------
    // Returns the user's water history for the requested number of days.
    //
    // Every requested day is included in the result.
    // Days without water records receive a value of zero.
    //
    // Returns null when no matching user exists.
    // ---------------------------------------------------------------------
    fun getWaterHistoryMap(username: String, days: Int): CompletableFuture<Map<String, Long>?> {
        // Determine today's calendar date.
        val today = LocalDate.now(zoneId)

        // Preserve the existing behavior for non-positive values.
        // An existing user receives an empty history.
        if (days <= 0) {
            val boundary = startOfDay(today)

            return waterRepository.getWaterRecords(username, boundary, boundary).thenApply { records ->
                if (records == null) { null }
                else { linkedMapOf() }
            }
        }

        // Determine the oldest requested date.
        val oldestDate = today.minusDays((days - 1).toLong())

        // Start at the beginning of the oldest requested day.
        val start = startOfDay(oldestDate)

        // Stop before the beginning of tomorrow.
        val end = startOfDay(today.plusDays(1))

        return waterRepository.getWaterRecords(username, start, end).thenApply { records ->
            // No matching user exists.
            if (records == null) { null }
            else {
                // LinkedHashMap preserves insertion order.
                val result = LinkedHashMap<String, Long>()

                // Pre-create every requested date with a default value of zero.
                for (i in 0 until days) {
                    val date = today.minusDays(i.toLong())
                    result[date.toString()] = 0L
                }

                // Add every stored drink to its calendar day.
                for (record in records) {
                    val date = toLocalDate(record.recordedAt)
                    val key = date.toString()

                    // Ignore records outside the requested application range.
                    if (result.containsKey(key)) {
                        result[key] = result.getValue(key) + record.amountMl.toLong()
                    }
                }

                result
            }
        }
    }

    // ---------------------------------------------------------------------
    // Returns the user's weekly water averages for the last four weeks.
    //
    // The last 28 days are divided into four seven-day buckets:
    //
    // Days 0-6   -> Week 4
    // Days 7-13  -> Week 3
    // Days 14-20 -> Week 2
    // Days 21-27 -> Week 1
    //
    // Days without water records are not included in the average
    // denominator.
    //
    // This preserves the existing application behavior.
    // ---------------------------------------------------------------------
    fun getWeeklyAverages(username: String): CompletableFuture<Map<String, Int>> {
        // Determine today's calendar date.
        val today = LocalDate.now(zoneId)
        // The last 28 days include today and the previous 27 days.
        val oldestDate = today.minusDays(27)

        // Start at the beginning of the oldest day.
        val start = startOfDay(oldestDate)
        // Stop before the beginning of tomorrow.
        val end = startOfDay(today.plusDays(1))

        return waterRepository.getWaterRecords(username, start, end
        ).thenApply { records ->

            // Preserve the existing behavior for a missing user.
            if (records == null) {
                emptyMap()
            } else {
                // Hold one accumulated total for every day
                // that contains water data.
                val dailyTotals = HashMap<LocalDate, Long>()

                // Calculate one total for each calendar day.
                for (record in records) {
                    val recordDate = toLocalDate(record.recordedAt)

                    val currentTotal =
                        dailyTotals.getOrDefault(recordDate, 0L)

                    dailyTotals[recordDate] =
                        currentTotal + record.amountMl.toLong()
                }

                // Hold total water per week bucket.
                val sums = LongArray(4)
                // Hold how many days actually contain water data.
                val counts = IntArray(4)

                // Visit each of the last 28 days.
                for (i in 0 until 28) {
                    // Calculate the date represented by this index.
                    val date = today.minusDays(i.toLong())

                    // Integer division creates groups of seven:
                    //
                    // 0 through 6   -> bucket 0
                    // 7 through 13  -> bucket 1
                    // 14 through 20 -> bucket 2
                    // 21 through 27 -> bucket 3
                    val weekIndex = i / 7

                    // Read the already calculated daily total.
                    val amount = dailyTotals[date]

                    // Missing days do not participate in the average.
                    if (amount != null) {
                        sums[weekIndex] += amount
                        counts[weekIndex]++
                    }
                }

                // Build the final response.
                val result = LinkedHashMap<String, Int>()

                // Convert the internal buckets into Week 4 ... Week 1.
                for (week in 0 until 4) {
                    // Preserve integer division from the existing behavior.
                    val average =
                        if (counts[week] > 0) {
                            (sums[week] / counts[week]).toInt()
                        } else {
                            0
                        }

                    // Bucket 0 becomes Week 4,
                    // bucket 1 becomes Week 3, etc.
                    result["Week ${4 - week}"] = average
                }

                result
            }
        }
    }

    // ---------------------------------------------------------------------
    // Returns the user's current daily water goal.
    //
    // The repository returns only the stored value.
    // This service applies the application default when no value
    // or matching user exists.
    // ---------------------------------------------------------------------
    fun getGoalMl(username: String): CompletableFuture<Int> {
        return waterRepository.getGoalMl(username).thenApply { storedGoal ->
                // Use the application default when no goal was stored.
                storedGoal ?: DEFAULT_GOAL_ML
            }
    }

    // ---------------------------------------------------------------------
    // Updates today's water goal.
    //
    // The service validates the goal value and decides which application
    // date should be updated.
    //
    // Transaction and concurrency handling are performed
    // by the repository implementation.
    //
    // Returns true when the update succeeded.
    // Returns false when the value is invalid or the user was not found.
    // ---------------------------------------------------------------------
    fun updateGoalMl(username: String, goalMl: Int): CompletableFuture<Boolean> {
        // Reject goal values outside the supported application range.
        if (goalMl !in MIN_GOAL_ML..MAX_GOAL_ML) {
            return CompletableFuture.completedFuture(false)
        }

        // Decide which date this goal belongs to.
        val today = LocalDate.now(zoneId)

        // Delegate persistence to the repository.
        return waterRepository.updateGoalMl(username, today, goalMl)
    }

    // ---------------------------------------------------------------------
    // Converts a LocalDate into a java.util.Date representing
    // the beginning of that day in the application's local timezone.
    // ---------------------------------------------------------------------
    private fun startOfDay(date: LocalDate): Date {
        return Date.from(date.atStartOfDay(zoneId).toInstant())
    }

    // ---------------------------------------------------------------------
    // Converts a stored timestamp into the corresponding LocalDate
    // using the application's local timezone.
    // ---------------------------------------------------------------------
    private fun toLocalDate(date: Date): LocalDate {
        return date.toInstant().atZone(zoneId).toLocalDate()
    }
}