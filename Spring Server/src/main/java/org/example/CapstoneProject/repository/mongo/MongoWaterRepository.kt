@file:Suppress("PackageName", "IfThenToElvis")
package org.example.CapstoneProject.repository.mongo
import com.mongodb.client.ClientSession
import com.mongodb.client.MongoClient
import com.mongodb.client.MongoCollection
import com.mongodb.client.MongoDatabase
import com.mongodb.client.model.Accumulators.sum
import com.mongodb.client.model.Aggregates.group
import com.mongodb.client.model.Aggregates.match
import com.mongodb.client.model.Filters.and
import com.mongodb.client.model.Filters.eq
import com.mongodb.client.model.Filters.gte
import com.mongodb.client.model.Filters.lt
import com.mongodb.client.model.UpdateOptions
import com.mongodb.client.model.Updates.inc
import com.mongodb.client.model.Updates.set
import org.bson.Document
import org.bson.types.ObjectId
import org.example.CapstoneProject.repository.WaterRepository
import org.json.JSONObject
import org.springframework.stereotype.Repository
import java.time.LocalDate
import java.time.ZoneId
import java.util.Date
import java.util.concurrent.CompletableFuture

// Default water goal used when no goal has been stored.
private const val DEFAULT_GOAL_ML = 3000

// -------------------------------------------------------------------------
// Low-level MongoDB implementation of WaterRepository.
//
// Each water drink is stored as a separate document.
//
// Example water document:
//
// {
//     _id: ObjectId(...),
//     userId: ObjectId(...),
//     amountMl: 500,
//     recordedAt: ISODate(...)
// }
//
// Goal history is stored in a separate collection.
//
// MongoDB handles:
// - storage
// - filtering
// - insert operations
// - update operations
//
// Kotlin handles:
// - daily totals
// - missing-day values
// - weekly buckets
// - weekly averages
// - JSONObject response construction
// -------------------------------------------------------------------------
@Suppress("unused")
@Repository
class MongoWaterRepository(
    database: MongoDatabase,

    // MongoClient is used to create sessions for MongoDB transactions.
    private val mongoClient: MongoClient
) : WaterRepository {

    // Hold the users collection.
    private val users: MongoCollection<Document> = database.getCollection("users")

    // Hold every individual water drink.
    private val waterRecords: MongoCollection<Document> = database.getCollection("water_records")

    // Hold daily goal history.
    private val goals: MongoCollection<Document> = database.getCollection("goals")

    // ---------------------------------------------------------------------
    // Adds one water drink for the user using a MongoDB transaction.
    //
    // Every drink becomes a separate document.
    //
    // The transaction contains:
    //
    // 1. Find and lock the user.
    // 2. Insert one new water record.
    //
    // withTransaction() manages transaction start, commit, abort
    // and eligible transient transaction retries.
    //
    // Mongo raw:
    //
    // session.withTransaction(() -> {
    //
    //     db.users.findOneAndUpdate(
    //         { username: username },
    //         { $inc: { transactionVersion: 1 } }
    //     )
    //
    //     db.water_records.insertOne({
    //         userId: userId,
    //         amountMl: waterAmount,
    //         recordedAt: new Date()
    //     })
    // })
    //
    // Returns false when:
    // - waterAmount <= 0
    // - the user does not exist
    // ---------------------------------------------------------------------
    override fun updateWater(username: String, waterAmount: Int): CompletableFuture<Boolean> {
        // Run the synchronous MongoDB operations asynchronously.
        return CompletableFuture.supplyAsync {
            // Reject invalid drink amounts.
            if (waterAmount <= 0) {
                false
            } else {
                // Open a MongoDB client session.
                val session = mongoClient.startSession()

                session.use { session ->
                    // Execute the complete operation inside one transaction.
                    session.withTransaction {
                        // Find and lock the user inside the current transaction.
                        val userId = lockAndFindUserId(session, username)

                        // The user does not exist.
                        if (userId == null) {
                            false
                        } else {
                            // Insert one new water record inside the transaction.
                            waterRecords.insertOne(
                                session,
                                Document("userId", userId)
                                    .append("amountMl", waterAmount)
                                    .append("recordedAt", Date())
                            )

                            // Returning true allows withTransaction()
                            // to commit the transaction.
                            true
                        }
                    }
                }
            }
        }
    }

    // ---------------------------------------------------------------------
    // Returns today's and yesterday's total water amounts.
    //
    // Mongo raw:
    //
    // db.water_records.find({
    //     userId: userId,
    //     recordedAt: {
    //         $gte: start,
    //         $lt: end
    //     }
    // })
    //
    // The query reads all drinks from the beginning of yesterday
    // until the beginning of tomorrow.
    //
    // Kotlin then separates the documents into today and yesterday.
    //
    // Returns null when the user does not exist.
    // ---------------------------------------------------------------------
    override fun getWater(username: String): CompletableFuture<JSONObject?> {
        // Run the synchronous MongoDB operation asynchronously.
        return CompletableFuture.supplyAsync<JSONObject?> {
            // Resolve the user.
            val userId = findUserId(username)

            if (userId == null) {
                null
            } else {
                // Get today's date.
                val today = LocalDate.now()

                // Calculate yesterday.
                val yesterday = today.minusDays(1)

                // Start at 00:00 yesterday.
                val start = startOfDay(yesterday)

                // Stop before 00:00 tomorrow.
                val end = startOfDay(today.plusDays(1))

                // Hold the two calculated totals.
                var todayWater = 0L
                var yesterdayWater = 0L

                // Find only documents inside the required time range.
                for (document in waterRecords.find(
                    and(
                        eq("userId", userId),
                        gte("recordedAt", start),
                        lt("recordedAt", end)
                    )
                )) {
                    // Read when the drink was recorded.
                    val recordedAt = document.getDate("recordedAt")

                    // Read the drink amount.
                    val amount = document.get("amountMl", Number::class.java)

                    // Ignore malformed or incomplete records.
                    if (recordedAt == null || amount == null) {
                        continue
                    }

                    // Convert MongoDB's timestamp into LocalDate.
                    val recordDate = toLocalDate(recordedAt)

                    // Add the amount to today's total.
                    if (recordDate == today) {
                        todayWater += amount.toLong()
                    }
                    // Add the amount to yesterday's total.
                    else if (recordDate == yesterday) {
                        yesterdayWater += amount.toLong()
                    }
                }

                // Build the same JSON response used by the existing application.
                val result = JSONObject()

                result.put("todayWater", todayWater)
                result.put("yesterdayWater", yesterdayWater)

                result
            }
        }
    }

    // ---------------------------------------------------------------------
    // Returns the user's water history for the requested number of days.
    //
    // Mongo raw:
    //
    // db.water_records.aggregate([
    //     {
    //         $match: {
    //             userId: userId,
    //             recordedAt: {
    //                 $gte: start,
    //                 $lt: end
    //             }
    //         }
    //     },
    //     {
    //         $group: {
    //             _id: {
    //                 $dateToString: {
    //                     format: "%Y-%m-%d",
    //                     date: "$recordedAt",
    //                     timezone: "Asia/Jerusalem"
    //                 }
    //             },
    //             total: {
    //                 $sum: "$amountMl"
    //             }
    //         }
    //     }
    // ])
    //
    // MongoDB therefore performs the daily calculation directly
    // instead of returning every individual drink to Kotlin.
    // ---------------------------------------------------------------------
    override fun getWaterHistoryMap(
        username: String,
        days: Int
    ): CompletableFuture<Map<String, Long>?> {
        // Run the synchronous MongoDB operation asynchronously.
        return CompletableFuture.supplyAsync<Map<String, Long>?> {
            // Resolve the supplied username into MongoDB's ObjectId.
            val userId = findUserId(username)

            // If the user does not exist, there is no water history to return.
            if (userId == null) {
                null
            } else {
                // LinkedHashMap preserves insertion order.
                // The dates are inserted starting from today and moving backwards.
                val result = LinkedHashMap<String, Long>()

                // Get the current local calendar date.
                val today = LocalDate.now()

                // Pre-create every requested date with a default water total of 0.
                for (i in 0 until days) {
                    result[today.minusDays(i.toLong()).toString()] = 0L
                }

                // Only query MongoDB when a positive number of days was requested.
                if (days > 0) {
                    // Calculate the beginning of the oldest requested day.
                    val start = startOfDay(today.minusDays((days - 1).toLong()))

                    // The upper bound is the beginning of tomorrow.
                    // start <= recordedAt < tomorrow
                    val end = startOfDay(today.plusDays(1))

                    // Execute the aggregation directly in MongoDB.
                    for (document in waterRecords.aggregate(
                        listOf(
                            // $match reduces the records to only:
                            // - the requested user
                            // - the requested date range
                            match(and(
                                    eq("userId", userId),
                                    gte("recordedAt", start),
                                    lt("recordedAt", end)
                                )
                            ),

                            // $dateToString converts the BSON Date into a yyyy-MM-dd key.
                            // $group then groups those records by local date
                            // and sums amountMl for each date.
                            group(
                                Document(
                                    "\$dateToString",
                                    Document("format", "%Y-%m-%d")
                                        .append("date", "\$recordedAt")
                                        .append("timezone", "Asia/Jerusalem")
                                ),

                                // Sum all amountMl values belonging to the same date.
                                sum("total", "\$amountMl")
                            )
                        )
                    )) {
                        // The $group _id contains the formatted date.
                        val date = document.getString("_id")

                        // Read the daily total calculated by MongoDB.
                        val total = document.get("total", Number::class.java)

                        if (date != null && total != null) {
                            // Replace the default zero value created earlier
                            // with the total calculated by MongoDB.
                            result[date] = total.toLong()
                        }
                    }
                }

                // Return the complete ordered history.
                //
                // Every requested day is present:
                // - days containing records have their calculated MongoDB total
                // - days without records remain 0
                result
            }
        }
    }

    // ---------------------------------------------------------------------
    // Returns weekly water averages for the last 28 days.
    //
    // Mongo raw:
    //
    // db.water_records.find({
    //     userId: userId,
    //     recordedAt: {
    //         $gte: start,
    //         $lt: end
    //     }
    // })
    //
    // MongoDB returns individual drink documents.
    //
    // Kotlin first calculates one total per day.
    //
    // Kotlin then divides the last 28 days into four buckets:
    //
    // Days 0-6   -> Week 4
    // Days 7-13  -> Week 3
    // Days 14-20 -> Week 2
    // Days 21-27 -> Week 1
    //
    // Important:
    // Days without water records are NOT included in the average denominator.
    //
    // This preserves the previous behavior.
    // ---------------------------------------------------------------------
    override fun getWeeklyAverages(username: String): CompletableFuture<Map<String, Int>> {
        // Run the synchronous MongoDB operation asynchronously.
        return CompletableFuture.supplyAsync<Map<String, Int>> {
            // Resolve the user.
            val userId = findUserId(username)

            // Preserve the existing behavior for a missing user.
            if (userId == null) { emptyMap() }
            else {
                // Today's date.
                val today = LocalDate.now()
                // The last 28 days include today and the previous 27 days.
                val oldestDate = today.minusDays(27)
                // Start of the oldest day.
                val start = startOfDay(oldestDate)
                // Stop before tomorrow.
                val end = startOfDay(today.plusDays(1))
                // Hold one accumulated total for every day that has water data.
                val dailyTotals = HashMap<LocalDate, Long>()

                // Read all water documents from the last 28 days.
                for (document in waterRecords.find(
                    and(
                        eq("userId", userId),
                        gte("recordedAt", start),
                        lt("recordedAt", end)
                    )
                )) {
                    // Read the stored timestamp.
                    val recordedAt = document.getDate("recordedAt")
                    // Read the stored drink amount.
                    val amount = document.get("amountMl", Number::class.java)

                    // Ignore invalid records.
                    if (recordedAt == null || amount == null) { continue }

                    // Determine the LocalDate represented by this record.
                    val recordDate = toLocalDate(recordedAt)

                    // Read the current total for that day.
                    val currentTotal = dailyTotals.getOrDefault(recordDate, 0L)

                    // Add the current drink amount.
                    dailyTotals[recordDate] = currentTotal + amount.toLong()
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
                    // 0 / 7 through 6 / 7   -> 0
                    // 7 / 7 through 13 / 7  -> 1
                    // 14 / 7 through 20 / 7 -> 2
                    // 21 / 7 through 27 / 7 -> 3
                    val weekIndex = i / 7

                    // Read the already calculated daily total.
                    val amount = dailyTotals[date]

                    // Missing days must not participate in the average.
                    if (amount != null) {
                        sums[weekIndex] += amount
                        counts[weekIndex]++
                    }
                }

                // Build the final response.
                val result = LinkedHashMap<String, Int>()

                // Convert the four internal buckets into Week 4 ... Week 1.
                for (week in 0 until 4) {
                    // Preserve integer division from the previous implementation.
                    val average = if (counts[week] > 0) {
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
    // Returns the user's most recently stored daily water goal.
    //
    // Mongo raw:
    //
    // db.goals.find({
    //     userId: userId
    // })
    // .sort({
    //     recordDate: -1
    // })
    // .limit(1)
    //
    // recordDate is stored as yyyy-MM-dd.
    //
    // Because this format is year-month-day,
    // alphabetical order is also chronological order.
    //
    // Returns 3000 when:
    // - the user does not exist
    // - the user has no stored goal
    // ---------------------------------------------------------------------
    override fun getGoalMl(username: String): CompletableFuture<Int> {
        // Run the synchronous MongoDB operation asynchronously.
        return CompletableFuture.supplyAsync {
            // Resolve the user.
            val userId = findUserId(username)

            if (userId == null) {
                DEFAULT_GOAL_ML
            } else {
                // Find the newest goal for this user.
                val goalDocument = goals.find(eq("userId", userId))
                    .sort(Document("recordDate", -1))
                    .first()

                // Use the default when no goal exists.
                if (goalDocument == null) {
                    DEFAULT_GOAL_ML
                } else {
                    // Return the stored goal.
                    goalDocument.getInteger("goalMl", DEFAULT_GOAL_ML)
                }
            }
        }
    }

    // ---------------------------------------------------------------------
    // Updates today's water goal using a MongoDB transaction.
    //
    // The transaction contains:
    //
    // 1. Find and lock the user.
    // 2. Update today's goal document.
    // 3. Insert today's goal automatically when it does not exist.
    //
    // upsert(true) means:
    //
    // - If today's goal exists -> update it.
    // - If today's goal does not exist -> insert it.
    //
    // Older goal documents remain stored as history.
    //
    // withTransaction() manages transaction start, commit, abort
    // and eligible transient transaction retries.
    //
    // Mongo raw:
    //
    // session.withTransaction(() -> {
    //
    //     db.users.findOneAndUpdate(
    //         { username: username },
    //         { $inc: { transactionVersion: 1 } }
    //     )
    //
    //     db.goals.updateOne(
    //         {
    //             userId: userId,
    //             recordDate: today
    //         },
    //         {
    //             $set: {
    //                 goalMl: goalMl
    //             }
    //         },
    //         {
    //             upsert: true
    //         }
    //     )
    // })
    //
    // Returns false for invalid values or missing users.
    // ---------------------------------------------------------------------
    override fun updateGoalMl(username: String, goalMl: Int): CompletableFuture<Boolean> {
        // Run the synchronous MongoDB operations asynchronously.
        return CompletableFuture.supplyAsync {
            // Preserve the existing goal validation.
            if (goalMl !in 500..10000) {
                false
            } else {
                // Open a MongoDB client session.
                val session = mongoClient.startSession()

                session.use { session ->
                    // Execute the complete operation inside one transaction.
                    session.withTransaction {
                        // Find and lock the user inside the current transaction.
                        val userId = lockAndFindUserId(session, username)

                        // The user does not exist.
                        if (userId == null) { false }
                        else {
                            // Build today's yyyy-MM-dd key.
                            val today = LocalDate.now().toString()

                            // Update today's goal.
                            // If today's document does not exist,
                            // MongoDB creates it automatically.
                            goals.updateOne(
                                session,
                                and(
                                    eq("userId", userId),
                                    eq("recordDate", today)
                                ),
                                set("goalMl", goalMl),
                                UpdateOptions().upsert(true)
                            )

                            // Returning true allows withTransaction()
                            // to commit the transaction.
                            true
                        }
                    }
                }
            }
        }
    }

    // ---------------------------------------------------------------------
    // Finds the MongoDB ObjectId belonging to a username.
    //
    // Mongo raw:
    //
    // db.users.findOne({
    //     username: username
    // })
    //
    // Returns null when no matching user exists.
    // ---------------------------------------------------------------------
    private fun findUserId(username: String): ObjectId? {
        // Find the user document.
        val document = users.find(eq("username", username)
        ).first()

        // User does not exist.
        if (document == null) { return null }

        // Return MongoDB's generated _id.
        return document.getObjectId("_id")
    }

    // ---------------------------------------------------------------------
    // Converts a LocalDate into a java.util.Date representing
    // the beginning of that day in the application's local timezone.
    //
    // Example:
    //
    // 2026-09-13
    //
    // becomes approximately:
    //
    // 2026-09-13T00:00:00
    //
    // MongoDB stores this as a BSON Date.
    // ---------------------------------------------------------------------
    private fun startOfDay(date: LocalDate): Date {
        return Date.from(
            date.atStartOfDay(ZoneId.systemDefault()).toInstant()
        )
    }

    // ---------------------------------------------------------------------
    // Converts MongoDB's BSON Date back into LocalDate.
    //
    // The application's local timezone is used when converting
    // the timestamp into a calendar day.
    // ---------------------------------------------------------------------
    private fun toLocalDate(date: Date): LocalDate {
        return date.toInstant()
            .atZone(ZoneId.systemDefault())
            .toLocalDate()
    }

    // ---------------------------------------------------------------------
    // Finds the ObjectId belonging to a username inside a MongoDB session.
    //
    // Mongo raw:
    //
    // db.users.findOne({
    //     username: username
    // })
    //
    // Returns null when no matching user exists.
    // ---------------------------------------------------------------------
    private fun findUserId(
        session: ClientSession,
        username: String
    ): ObjectId? {
        // Find the matching user document using the current transaction session.
        val document = users.find(
            session,
            eq("username", username)
        ).first()

        if (document == null) { return null }

        // Return MongoDB's _id value.
        return document.getObjectId("_id")
    }

    // ---------------------------------------------------------------------
    // Finds the user's ObjectId and updates transactionVersion
    // inside the current MongoDB transaction.
    //
    // All transactions that modify data related to a user call this helper.
    //
    // Updating transactionVersion forces concurrent transactions
    // to write to the same user document.
    //
    // This helps prevent races between operations such as:
    //
    // deleteByUsername()
    // updateWater()
    // updateCalories()
    // updateGoalMl()
    //
    // Mongo raw:
    //
    // db.users.findOneAndUpdate(
    //     {
    //         username: username
    //     },
    //     {
    //         $inc: {
    //             transactionVersion: 1
    //         }
    //     }
    // )
    //
    // Returns the user's ObjectId.
    // Returns null when the user does not exist.
    // ---------------------------------------------------------------------
    private fun lockAndFindUserId(session: ClientSession, username: String): ObjectId? {
        // Find the user and increment transactionVersion
        // inside the current transaction.
        val document = users.findOneAndUpdate(
            session,
            eq("username", username),
            inc("transactionVersion", 1)
        )

        // The user does not exist.
        if (document == null) { return null }

        // Return MongoDB's _id value.
        return document.getObjectId("_id")
    }
}