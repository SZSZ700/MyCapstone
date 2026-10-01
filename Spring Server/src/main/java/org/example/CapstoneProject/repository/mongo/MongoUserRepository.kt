@file:Suppress("PackageName", "IfThenToElvis")
package org.example.CapstoneProject.repository.mongo
import com.mongodb.MongoWriteException
import com.mongodb.client.ClientSession
import com.mongodb.client.MongoClient
import com.mongodb.client.MongoCollection
import com.mongodb.client.MongoDatabase
import com.mongodb.client.model.UpdateOptions
import org.bson.Document
import org.bson.conversions.Bson
import org.bson.types.ObjectId
import org.example.CapstoneProject.model.User
import org.example.CapstoneProject.repository.UserRepository
import org.springframework.stereotype.Repository
import java.time.LocalDate
import java.util.concurrent.CompletableFuture
import com.mongodb.client.model.Filters.and
import com.mongodb.client.model.Filters.eq
import com.mongodb.client.model.Updates.combine
import com.mongodb.client.model.Updates.inc
import com.mongodb.client.model.Updates.set

// -------------------------------------------------------------------------
// Low-level MongoDB implementation of UserRepository.
//
// This repository works directly with the MongoDB Java driver.
//
// It uses:
// - MongoDatabase
// - MongoCollection<Document>
// - Document
// - Filters
// - Updates
//
// It does NOT use:
// - MongoRepository
// - MongoTemplate
// - JPA
// - Hibernate
//
// The service layer continues to depend only on UserRepository.
// -------------------------------------------------------------------------
@Suppress("unused")
@Repository
class MongoUserRepository(
    database: MongoDatabase,

    // MongoClient is used to create sessions for MongoDB transactions.
    private val mongoClient: MongoClient
) : UserRepository {

    // Hold the users collection.
    private val users: MongoCollection<Document> = database.getCollection("users")

    // Hold the calories collection.
    private val calories: MongoCollection<Document> = database.getCollection("calories")

    // Hold the goals collection.
    private val goals: MongoCollection<Document> = database.getCollection("goals")

    // Hold the water records collection.
    private val waterRecords: MongoCollection<Document> = database.getCollection("water_records")

    // ---------------------------------------------------------------------
    // Finds a user by username.
    //
    // Mongo raw:
    // db.users.findOne({
    //     username: username
    // })
    //
    // Returns the matching user.
    // Returns null when no matching user exists.
    // ---------------------------------------------------------------------
    override fun findByUsername(username: String): CompletableFuture<User?> {
        // Run the synchronous MongoDB operation asynchronously.
        return CompletableFuture.supplyAsync<User?> {
            // Find the first document whose username matches.
            val document = users.find(
                eq("username", username)
            ).first()

            // Return null when no document was found.
            if (document == null) { null }
            // Convert the MongoDB document into the existing User model.
            else { mapUser(document) }
        }
    }

    // ---------------------------------------------------------------------
    // Returns all users stored in MongoDB.
    //
    // Mongo raw:
    // db.users.find({})
    // ---------------------------------------------------------------------
    override fun findAll(): CompletableFuture<List<User>> {
        // Run the synchronous MongoDB operation asynchronously.
        return CompletableFuture.supplyAsync {
            // Create the result list.
            val result = ArrayList<User>()

            // MongoDB returns every document in the users collection.
            for (document in users.find()) {
                // Convert each document into a User object.
                result.add(mapUser(document))
            }

            result
        }
    }

    // ---------------------------------------------------------------------
    // Checks whether a user exists by username.
    //
    // Mongo raw:
    //
    // db.users.countDocuments({
    //     username: username
    // })
    //
    // Returns true when at least one matching document exists.
    // ---------------------------------------------------------------------
    override fun existsByUsername(username: String): CompletableFuture<Boolean> {
        // Run the synchronous MongoDB operation asynchronously.
        return CompletableFuture.supplyAsync {
            // Count documents matching the supplied username.
            val count = users.countDocuments(
                eq("username", username)
            )

            // A count greater than zero means the user exists.
            count > 0
        }
    }

    // ---------------------------------------------------------------------
    // Deletes a user by username using a MongoDB transaction.
    //
    // The transaction deletes all data related to the user from:
    //
    // 1. calories
    // 2. goals
    // 3. water_records
    // 4. users
    //
    // All delete operations must succeed together.
    //
    // withTransaction() manages:
    // - starting the transaction
    // - committing the transaction
    // - aborting the transaction when an exception occurs
    // - retrying eligible transient transaction errors
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
    //     db.calories.deleteMany({
    //         userId: userId
    //     })
    //
    //     db.goals.deleteMany({
    //         userId: userId
    //     })
    //
    //     db.water_records.deleteMany({
    //         userId: userId
    //     })
    //
    //     db.users.deleteOne({
    //         _id: userId
    //     })
    // })
    //
    // Returns true when the user and all related data were deleted.
    // Returns false when no matching user exists.
    // ---------------------------------------------------------------------
    override fun deleteByUsername(username: String): CompletableFuture<Boolean> {
        // Run the synchronous MongoDB operations asynchronously.
        return CompletableFuture.supplyAsync {
            // Open a MongoDB client session.
            val session = mongoClient.startSession()

            session.use { session ->
                // Execute the complete delete operation inside one transaction.
                session.withTransaction {
                    // Find and lock the user document by incrementing
                    // transactionVersion inside the current transaction.
                    val userId = lockAndFindUserId(session, username)

                    // The user does not exist.
                    if (userId == null) { false }
                    else {
                        // Delete all calorie records belonging to the user.
                        calories.deleteMany(session,
                            eq("userId", userId))

                        // Delete all goal records belonging to the user.
                        goals.deleteMany(session,
                            eq("userId", userId))

                        // Delete all water records belonging to the user.
                        waterRecords.deleteMany(session,
                            eq("userId", userId))

                        // Delete the user document itself.
                        val result = users.deleteOne(session,
                            eq("_id", userId)
                        )

                        // If the user document was not deleted,
                        // abort the transaction so previous deletions are rolled back.
                        if (result.deletedCount == 0L) {
                            session.abortTransaction()
                            false
                        }
                        // Returning true allows withTransaction()
                        // to commit the complete transaction.
                        else { true }
                    }
                }
            }
        }
    }

    // ---------------------------------------------------------------------
    // Partially updates an existing user.
    //
    // PATCH is dynamic, so the update fields depend on what the client sent.
    // Username changes are intentionally ignored.
    //
    // Example Mongo raw:
    //
    // db.users.updateOne(
    //     { username: username },
    //     { $set: { fullName: "New Name", age: 30 } }
    // )
    //
    // Returns the updated user.
    // Returns null when no matching user exists.
    // ---------------------------------------------------------------------
    override fun patchByUsername(username: String, updates: MutableMap<String, Any>
    ): CompletableFuture<User?> {
        // Run the synchronous MongoDB operation asynchronously.
        return CompletableFuture.supplyAsync<User?> {
            // Build only the update operations that were actually requested.
            val mongoUpdates = ArrayList<Bson>()

            // Convert the API field "password"
            // into the MongoDB field "passwordHash".
            if (updates.containsKey("password")) {
                mongoUpdates.add(set("passwordHash", updates["password"]))
            }

            // Add fullName when supplied.
            if (updates.containsKey("fullName")) {
                mongoUpdates.add(set("fullName", updates["fullName"]))
            }

            // Add age when supplied.
            if (updates.containsKey("age")) {
                val age = updates["age"] as Number
                mongoUpdates.add(set("age", age.toInt()))
            }

            // Add BMI when supplied.
            if (updates.containsKey("bmi")) {
                val bmi = updates["bmi"] as Number
                mongoUpdates.add(set("bmi", bmi.toDouble()))
            }

            // Username is never added to mongoUpdates,
            // therefore PATCH cannot change it.

            // When no supported fields were supplied,
            // simply return the current user.
            if (mongoUpdates.isEmpty()) {
                // Mongo raw:
                // db.users.findOne({
                //     username: username
                // })

                val currentDocument = users.find(
                    eq("username", username)
                ).first()

                if (currentDocument == null) { null }
                else { mapUser(currentDocument) }
            } else {
                // Mongo raw:
                // db.users.updateOne(
                //     { username: username },
                //     { $set: { dynamic fields here } }
                // )

                val result = users.updateOne(
                    eq("username", username),
                    combine(mongoUpdates)
                )

                // No matching user exists.
                if (result.matchedCount == 0L) { null }
                else {
                    // Read the updated document again.
                    val updatedDocument = users
                        .find(eq("username", username))
                        .first()

                    if (updatedDocument == null) { null }
                    else { mapUser(updatedDocument) }
                }
            }
        }
    }

    // ---------------------------------------------------------------------
    // Creates a new user.
    //
    // Mongo raw:
    //
    // db.users.insertOne({
    //     username: userName,
    //     passwordHash: password,
    //     fullName: fullName,
    //     age: age,
    //     bmi: bmi,
    //     transactionVersion: 0
    // })
    //
    // Returns false when:
    // - username is null
    // - username is blank
    // - username already exists
    //
    // A UNIQUE index on username still protects against concurrent
    // duplicate signup requests.
    // ---------------------------------------------------------------------
    override fun create(user: User): CompletableFuture<Boolean> {
        // Run the synchronous MongoDB operation asynchronously.
        return CompletableFuture.supplyAsync {
            // Extract the username safely.
            val username = user.userName

            // Reject invalid usernames before contacting MongoDB.
            if (username.isNullOrBlank()) {
                false
            } else {
                try {
                    // Insert the new user document.
                    users.insertOne(userToDocument(user))
                    true
                }
                catch (e: MongoWriteException) {
                    // Duplicate key error caused by the UNIQUE username index.
                    // This protects against concurrent duplicate signup requests.
                    if (e.error.code == 11000) { false }
                    // Re-throw every other MongoDB write error.
                    else { throw e }
                }
            }
        }
    }

    // ---------------------------------------------------------------------
    // Updates only the user's BMI value.
    //
    // Mongo raw:
    //
    // db.users.updateOne(
    //     {
    //         username: username
    //     },
    //     {
    //         $set: {
    //             bmi: bmi
    //         }
    //     }
    // )
    //
    // Returns true when the user exists.
    // Returns false when no matching user exists.
    // ---------------------------------------------------------------------
    override fun updateBmi(username: String, bmi: Double): CompletableFuture<Boolean> {
        // Run the synchronous MongoDB operation asynchronously.
        return CompletableFuture.supplyAsync {
            // Update only the bmi field.
            val result = users.updateOne(
                eq("username", username),
                set("bmi", bmi)
            )

            // matchedCount tells us whether MongoDB found the user.
            result.matchedCount > 0
        }
    }

    // ---------------------------------------------------------------------
    // Returns today's calories value.
    //
    // Mongo raw:
    //
    // db.calories.findOne({
    //     userId: userId,
    //     recordDate: today
    // })
    //
    // Returns zero when:
    // - the user does not exist
    // - no calories document exists for today
    // ---------------------------------------------------------------------
    override fun getCalories(username: String): CompletableFuture<Int> {
        // Run the synchronous MongoDB operation asynchronously.
        return CompletableFuture.supplyAsync {
            // Resolve the username into the user's ObjectId.
            val userId = findUserId(username)

            // Missing user behaves like the original implementation.
            if (userId == null) { 0 }
            else {
                // LocalDate.toString() produces yyyy-MM-dd.
                val today = LocalDate.now().toString()

                // Find today's calories document.
                val document = calories.find(
                    and(
                        eq("userId", userId),
                        eq("recordDate", today)
                    )
                ).first()

                // No calories entry exists for today.
                if (document == null) {
                    0
                } else {
                    // Return the stored calories value.
                    document.getInteger("calories", 0)
                }
            }
        }
    }

    // ---------------------------------------------------------------------
    // Updates today's calories value using a MongoDB transaction.
    //
    // The transaction contains:
    //
    // 1. Find and lock the user.
    // 2. Update today's calories document.
    // 3. Insert today's document automatically when it does not exist.
    //
    // upsert(true) means:
    //
    // - If today's document exists -> update it.
    // - If today's document does not exist -> insert it.
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
    //     db.calories.updateOne(
    //         {
    //             userId: userId,
    //             recordDate: today
    //         },
    //         {
    //             $set: {
    //                 calories: caloriesValue
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
    override fun updateCalories(username: String, calories: Int): CompletableFuture<Boolean> {
        // Run the synchronous MongoDB operations asynchronously.
        return CompletableFuture.supplyAsync {
            // Preserve the existing calorie validation.
            if (calories !in 0..20000) { false }
            else {
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
                            // Build today's yyyy-MM-dd key.
                            val today = LocalDate.now().toString()

                            // Update today's calories document.
                            // If it does not exist, MongoDB creates it automatically.
                            this@MongoUserRepository.calories.updateOne(
                                session,
                                and(
                                    eq("userId", userId),
                                    eq("recordDate", today)
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
    }

    // ---------------------------------------------------------------------
    // Returns the global BMI distribution.
    //
    // Mongo raw:
    //
    // db.users.find({})
    //
    // MongoDB only retrieves the stored BMI values.
    //
    // The BMI category business logic remains in Kotlin:
    // - Underweight
    // - Normal
    // - Overweight
    // - Obese
    // ---------------------------------------------------------------------
    override fun getBmiDistribution(): CompletableFuture<Map<String, Int>> {
        // Run the synchronous MongoDB operation asynchronously.
        return CompletableFuture.supplyAsync {
            // Initialize all counters.
            var underweight = 0
            var normal = 0
            var overweight = 0
            var obese = 0

            // Read all users.
            for (document in users.find()) {
                // Read the BMI as Number because BSON numeric values
                // may be represented by different JVM numeric types.
                val bmiNumber = document.get("bmi", Number::class.java) ?: continue

                // Convert to Double for comparison.
                val bmi = bmiNumber.toDouble()

                // Apply the same BMI classification used before.
                if (bmi < 18.5) { underweight++ }
                else if (bmi in 18.5..<25.0) { normal++ }
                else if (bmi in 25.0..<30.0) { overweight++ }
                else if (bmi >= 30.0) { obese++ }
            }

            // LinkedHashMap preserves the desired output order.
            val distribution = LinkedHashMap<String, Int>()
            distribution["Underweight"] = underweight
            distribution["Normal"] = normal
            distribution["Overweight"] = overweight
            distribution["Obese"] = obese

            // return the distribution map
            distribution
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
        val document = users.find(
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

    // ---------------------------------------------------------------------
    // Converts a MongoDB user document into the existing User model.
    //
    // MongoDB field names:
    // username
    // passwordHash
    // fullName
    // age
    // bmi
    // ---------------------------------------------------------------------
    private fun mapUser(document: Document): User {
        // Create an empty User object.
        val user = User()

        // Copy String fields.
        user.userName = document.getString("username")
        user.password = document.getString("passwordHash")
        user.fullName = document.getString("fullName")

        // Read numeric fields safely.
        val age = document.get("age", Number::class.java)
        val bmi = document.get("bmi", Number::class.java)

        // Use safe defaults when a value is missing.
        user.age = if (age == null) 0 else age.toInt()
        user.bmi = if (bmi == null) 0.0 else bmi.toDouble()

        return user
    }

    // ---------------------------------------------------------------------
    // Converts the existing User model into a raw MongoDB document.
    //
    // Mongo raw shape:
    //
    // {
    //     username: "...",
    //     passwordHash: "...",
    //     fullName: "...",
    //     age: 25,
    //     bmi: 22.5,
    //     transactionVersion: 0
    // }
    // ---------------------------------------------------------------------
    private fun userToDocument(user: User): Document {
        return Document("username", user.userName)
            .append("passwordHash", user.password)
            .append("fullName", user.fullName)
            .append("age", user.age)
            .append("bmi", user.bmi)
            .append("transactionVersion", 0)
    }
}