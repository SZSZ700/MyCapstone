@file:Suppress("PackageName", "IfThenToElvis", "FoldInitializerAndIfToElvis")
package org.example.CapstoneProject.repository.mongo
import com.mongodb.client.ClientSession
import com.mongodb.client.MongoClient
import com.mongodb.client.MongoCollection
import com.mongodb.client.MongoDatabase
import com.mongodb.client.model.Filters.and
import com.mongodb.client.model.Filters.eq
import com.mongodb.client.model.UpdateOptions
import com.mongodb.client.model.Updates.inc
import com.mongodb.client.model.Updates.set
import org.bson.Document
import org.bson.types.ObjectId
import org.example.CapstoneProject.repository.CaloriesRepository
import org.springframework.stereotype.Repository
import java.time.LocalDate
import java.util.concurrent.CompletableFuture

// -------------------------------------------------------------------------
// Low-level MongoDB implementation of CaloriesRepository.
//
// This repository handles:
// - calorie document queries
// - calorie document updates
// - MongoDB transactions
// - MongoDB upsert operations
// - user ObjectId resolution
//
// Business validation and application date selection are handled
// by the service layer.
// -------------------------------------------------------------------------
@Suppress("unused")
@Repository
class MongoCaloriesRepository(
    database: MongoDatabase,
    // MongoClient is used to create sessions for MongoDB transactions.
    private val mongoClient: MongoClient
) : CaloriesRepository {
    // Hold the users collection.
    private val users: MongoCollection<Document> = database.getCollection("users")

    // Hold calorie records.
    private val calories: MongoCollection<Document> = database.getCollection("calories")

    // ---------------------------------------------------------------------
    // Returns the calories value for a user on the supplied date.
    //
    // Mongo raw:
    //
    // db.calories.findOne({
    //     userId: userId,
    //     recordDate: date
    // })
    //
    // Returns null when:
    // - the user does not exist
    // - no calorie record exists for the supplied date
    // - the calories field is missing
    // ---------------------------------------------------------------------
    override fun getCalories(username: String, date: LocalDate): CompletableFuture<Int?> {
        // Run the synchronous MongoDB operation asynchronously.
        return CompletableFuture.supplyAsync<Int?> {
            // Resolve the username into the user's ObjectId.
            val userId = findUserId(username)

            if (userId == null) { null }
            else {
                // Convert the supplied date into the yyyy-MM-dd
                // format stored in MongoDB.
                val recordDate = date.toString()

                // Find the calorie document for the supplied date.
                val document = calories.find(
                    and(eq("userId", userId),
                        eq("recordDate", recordDate)
                    )
                ).first()

                if (document == null) { null }
                else {
                    // Return the stored value without applying
                    // an application-level fallback.
                    document.get("calories", Number::class.java)?.toInt()
                }
            }
        }
    }

    // ---------------------------------------------------------------------
    // Updates the calories value for a user on the supplied date
    // using a MongoDB transaction.
    //
    // The transaction contains:
    //
    // 1. Find and lock the user.
    // 2. Update the calorie document for the supplied date.
    // 3. Insert the document automatically when it does not exist.
    //
    // upsert(true) means:
    // - If the document exists -> update it.
    // - If the document does not exist -> insert it.
    //
    // Calories validation and date selection are handled
    // by the service layer.
    //
    // Returns true when the update succeeded.
    // Returns false when no matching user exists.
    // ---------------------------------------------------------------------
    override fun updateCalories(username: String, date: LocalDate, calories: Int): CompletableFuture<Boolean> {
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
                        // Convert the supplied date into the
                        // yyyy-MM-dd format stored in MongoDB.
                        val recordDate = date.toString()

                        // Update the calorie document.
                        // MongoDB inserts the document when it does not exist.
                        this@MongoCaloriesRepository.calories.updateOne(
                            session,
                            and(eq("userId", userId),
                                eq("recordDate", recordDate)
                            ),
                            set("calories", calories),
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
    // Finds the ObjectId belonging to a username.
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

        // Find the matching user document.
        val document = users.find(eq("username", username)
        ).first()

        if (document == null) { return null }

        // Return MongoDB's generated _id.
        return document.getObjectId("_id")
    }

    // ---------------------------------------------------------------------
    // Finds the user's ObjectId and updates transactionVersion
    // inside the current MongoDB transaction.
    //
    // Updating transactionVersion forces concurrent transactions
    // affecting the same user to write to the same user document.
    //
    // Returns the user's ObjectId.
    // Returns null when the user does not exist.
    // ---------------------------------------------------------------------
    private fun lockAndFindUserId(session: ClientSession, username: String): ObjectId? {
        // Find the user and increment transactionVersion
        // inside the current transaction.
        val document = users.findOneAndUpdate(
                session, eq("username", username),
                inc("transactionVersion", 1)
            )

        if (document == null) { return null }

        // Return MongoDB's _id value.
        return document.getObjectId("_id")
    }
}