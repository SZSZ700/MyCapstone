@file:Suppress("PackageName", "IfThenToElvis", "FoldInitializerAndIfToElvis")
package org.example.CapstoneProject.repository.mongo
import com.mongodb.client.ClientSession
import com.mongodb.client.MongoClient
import com.mongodb.client.MongoCollection
import com.mongodb.client.MongoDatabase
import com.mongodb.client.model.Filters.and
import com.mongodb.client.model.Filters.eq
import com.mongodb.client.model.Filters.gte
import com.mongodb.client.model.Filters.lt
import com.mongodb.client.model.UpdateOptions
import com.mongodb.client.model.Updates.inc
import com.mongodb.client.model.Updates.set
import org.bson.Document
import org.bson.types.ObjectId
import org.example.CapstoneProject.repository.WaterRecordData
import org.example.CapstoneProject.repository.WaterRepository
import org.springframework.stereotype.Repository
import java.time.LocalDate
import java.util.Date
import java.util.concurrent.CompletableFuture

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
// This repository handles:
// - MongoDB storage
// - MongoDB filtering
// - insert operations
// - update operations
// - transactions
// - ObjectId resolution
// - MongoDB document mapping
//
// Business rules and statistical calculations are handled
// by the service layer.
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
    // Stores one water drink for the user using a MongoDB transaction.
    //
    // The service layer has already:
    // - validated the amount
    // - selected the timestamp
    //
    // Every drink becomes a separate MongoDB document.
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
    //         recordedAt: recordedAt
    //     })
    // })
    //
    // Returns true when the record was inserted.
    // Returns false when the user does not exist.
    // ---------------------------------------------------------------------
    override fun updateWater(username: String, waterAmount: Int, recordedAt: Date): CompletableFuture<Boolean> {
        // Run the synchronous MongoDB operations asynchronously.
        return CompletableFuture.supplyAsync {
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
                        // Insert one new water record inside the transaction.
                        waterRecords.insertOne(
                            session,
                            Document("userId", userId)
                                .append("amountMl", waterAmount)
                                .append("recordedAt", recordedAt)
                        )

                        // Returning true allows withTransaction()
                        // to commit the transaction.
                        true
                    }
                }
            }
        }
    }

    // ---------------------------------------------------------------------
    // Returns stored water records inside the supplied time range.
    //
    // Mongo raw:
    //
    // db.water_records.find({
    //     userId: userId,
    //     recordedAt: {
    //         $gte: startInclusive,
    //         $lt: endExclusive
    //     }
    // })
    //
    // This method does not:
    // - calculate daily totals
    // - insert missing days
    // - calculate weekly averages
    // - decide what today means
    //
    // Those responsibilities belong to the service layer.
    //
    // Returns null when the user does not exist.
    // ---------------------------------------------------------------------
    override fun getWaterRecords(username: String, startInclusive: Date, endExclusive: Date): CompletableFuture<List<WaterRecordData>?> {
        // Run the synchronous MongoDB operation asynchronously.
        return CompletableFuture.supplyAsync<List<WaterRecordData>?> {
            // Resolve the supplied username into MongoDB's ObjectId.
            val userId = findUserId(username)

            // No matching user exists.
            if (userId == null) { null }
            else {
                // Hold the mapped repository result.
                val result = ArrayList<WaterRecordData>()

                // Read only records inside the supplied range.
                for (document in waterRecords.find(
                    and(eq("userId", userId),
                        gte("recordedAt", startInclusive),
                        lt("recordedAt", endExclusive)
                    )
                )) {

                    // Read the stored timestamp.
                    val recordedAt = document.getDate("recordedAt")

                    // Read the stored water amount.
                    val amount =
                        document.get("amountMl", Number::class.java)

                    // Ignore malformed or incomplete database records.
                    if (recordedAt == null || amount == null) { continue }

                    // Convert the MongoDB document into a
                    // database-independent repository result.
                    result.add(WaterRecordData(recordedAt = recordedAt,
                            amountMl = amount.toInt())
                    )
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
    // Because this format is year-month-day,
    // alphabetical order is also chronological order.
    //
    // Returns null when:
    // - the user does not exist
    // - no goal has been stored
    // - the stored goal value is missing
    //
    // The application default is handled by the service layer.
    // ---------------------------------------------------------------------
    override fun getGoalMl(username: String): CompletableFuture<Int?> {
        // Run the synchronous MongoDB operation asynchronously.
        return CompletableFuture.supplyAsync<Int?> {
            // Resolve the user.
            val userId = findUserId(username)
            // No matching user exists.
            if (userId == null) { null }
            else {
                // Find the newest goal for this user.
                val goalDocument = goals.find(eq("userId", userId))
                        .sort(Document("recordDate", -1))
                        .first()

                // Return null when no goal document exists.
                if (goalDocument == null) { null }
                else {
                    // Read the stored goal without applying
                    // any application default.
                    goalDocument.get("goalMl", Number::class.java)?.toInt()
                }
            }
        }
    }

    // ---------------------------------------------------------------------
    // Updates the user's water goal for the supplied date
    // using a MongoDB transaction.
    //
    // The service layer has already:
    // - validated the goal value
    // - selected the application date
    //
    // The transaction contains:
    //
    // 1. Find and lock the user.
    // 2. Update the goal document for the supplied date.
    // 3. Insert the goal automatically when it does not exist.
    //
    // upsert(true) means:
    // - If the goal document exists -> update it.
    // - If the goal document does not exist -> insert it.
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
    //             recordDate: recordDate
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
    // Returns true when the update succeeded.
    // Returns false when no matching user exists.
    // ---------------------------------------------------------------------
    override fun updateGoalMl(username: String, recordDate: LocalDate, goalMl: Int): CompletableFuture<Boolean> {
        // Run the synchronous MongoDB operations asynchronously.
        return CompletableFuture.supplyAsync {
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
                        // Convert the application date into the
                        // yyyy-MM-dd format stored in MongoDB.
                        val storedDate = recordDate.toString()

                        // Update the goal for the supplied date.
                        // MongoDB creates the document when it does not exist.
                        goals.updateOne(session,
                            and(eq("userId", userId),
                                eq("recordDate", storedDate)
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
        val document = users.find(eq("username", username)).first()
        // User does not exist.
        if (document == null) { return null }
        // Return MongoDB's generated _id.
        return document.getObjectId("_id")
    }

    // ---------------------------------------------------------------------
    // Finds the user's ObjectId and updates transactionVersion
    // inside the current MongoDB transaction.
    //
    // Transactions that modify water-related data call this helper.
    //
    // Updating transactionVersion forces concurrent transactions
    // affecting the same user to write to the same user document.
    //
    // This helps coordinate operations such as:
    // - updateWater()
    // - updateGoalMl()
    // - user deletion
    // - other user-related transactions
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