@file:Suppress("PackageName", "IfThenToElvis", "FoldInitializerAndIfToElvis")
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
class MongoUserRepository(database: MongoDatabase,
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
    // Mongo raw:
    // db.users.findOne({
    //     username: username
    // })
    // Returns the matching user.
    // Returns null when no matching user exists.
    // ---------------------------------------------------------------------
    override fun findByUsername(username: String): CompletableFuture<User?> {
        // Run the synchronous MongoDB operation asynchronously.
        return CompletableFuture.supplyAsync<User?> {
            // Find the first document whose username matches.
            val document = users.find(eq("username", username)).first()

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
            // A count greater than zero means the user exists.
            users.countDocuments(eq("username", username)) > 0
        }
    }

    // ---------------------------------------------------------------------
    // Deletes a user by username using a MongoDB transaction.
    // The transaction deletes all data related to the user from:
    // 1. calories
    // 2. goals
    // 3. water_records
    // 4. users
    // All delete operations must succeed together.
    // withTransaction() manages:
    // - starting the transaction
    // - committing the transaction
    // - aborting the transaction when an exception occurs
    // - retrying eligible transient transaction errors
    //
    // Mongo raw:
    // session.withTransaction(() -> {
    //     db.users.findOneAndUpdate({ username: username }, { $inc: { transactionVersion: 1 } } )
    //     db.calories.deleteMany({ userId: userId })
    //     db.goals.deleteMany({ userId: userId })
    //     db.water_records.deleteMany({ userId: userId })
    //     db.users.deleteOne({ _id: userId })
    // })
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
                        calories.deleteMany(session, eq("userId", userId))
                        // Delete all goal records belonging to the user.
                        goals.deleteMany(session, eq("userId", userId))
                        // Delete all water records belonging to the user.
                        waterRecords.deleteMany(session, eq("userId", userId))
                        // Delete the user document itself.
                        val result = users.deleteOne(session, eq("_id", userId))

                        // If the user document was not deleted,
                        // abort the transaction so previous deletions are rolled back.
                        if (result.deletedCount == 0L) { session.abortTransaction()
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
    // Field validation, type validation and password encoding are handled
    // by the service layer before this method is called.
    //
    // This repository is responsible only for translating the prepared
    // fields into MongoDB update operations and executing the update.
    //
    // The API field "password" is stored in MongoDB as "passwordHash".
    //
    // Returns the updated user when the user was found.
    // Returns null when no matching user exists.
    // ---------------------------------------------------------------------
    override fun patchByUsername(username: String, updates: MutableMap<String, Any>): CompletableFuture<User?> {

        // Run the synchronous MongoDB operation asynchronously.
        return CompletableFuture.supplyAsync<User?> {
            // Build MongoDB update operations from the prepared fields.
            val mongoUpdates = ArrayList<Bson>()

            // Map the service field "password" to the MongoDB field "passwordHash".
            if (updates.containsKey("password")) {
                mongoUpdates.add(set("passwordHash", updates["password"]))
            }

            // Add the full name when supplied.
            if (updates.containsKey("fullName")) {
                mongoUpdates.add(set("fullName", updates["fullName"]))
            }

            // Add the already validated and normalized age.
            if (updates.containsKey("age")) {
                mongoUpdates.add(set("age", updates["age"]))
            }

            // Add the already validated and normalized BMI.
            if (updates.containsKey("bmi")) {
                mongoUpdates.add(set("bmi", updates["bmi"]))
            }

            // When no supported fields were supplied,
            // return the current user without performing an update.
            if (mongoUpdates.isEmpty()) {
                val currentDocument = users.find(eq("username", username)).first()

                if (currentDocument == null) { null }
                else { mapUser(currentDocument) }
            } else {
                // Apply all requested fields in a single MongoDB update.
                val result = users.updateOne(eq("username", username),
                    combine(mongoUpdates)
                )

                // Return null when the user does not exist.
                if (result.matchedCount == 0L) { null }
                else {

                    // Read and return the updated user.
                    val updatedDocument =
                        users.find(
                            eq("username", username)
                        ).first()

                    if (updatedDocument == null) { null }
                    else { mapUser(updatedDocument) }
                }
            }
        }
    }

    // ---------------------------------------------------------------------
    // Creates a new user.
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
    // Input validation is handled by the service layer.
    //
    // A UNIQUE index on username protects against duplicate usernames,
    // including concurrent signup requests.
    //
    // Returns true when the user was created successfully.
    // Returns false when MongoDB rejects the insert because the username
    // already exists.
    // ---------------------------------------------------------------------
    override fun create(user: User): CompletableFuture<Boolean> {
        // Run the synchronous MongoDB operation asynchronously.
        return CompletableFuture.supplyAsync {
            try {
                // Insert the new user document.
                users.insertOne(userToDocument(user))
                true
            } catch (e: MongoWriteException) {
                // MongoDB duplicate-key error caused by the UNIQUE username index.
                if (e.error.code == 11000) { false }
                // Re-throw every other MongoDB write error.
                else { throw e }
            }
        }
    }

    // ---------------------------------------------------------------------
    // Updates only the user's BMI value.
    // Mongo raw:
    // db.users.updateOne({username: username}, {$set: {bmi: bmi}})
    // Returns true when the user exists.
    // Returns false when no matching user exists.
    // ---------------------------------------------------------------------
    override fun updateBmi(username: String, bmi: Double): CompletableFuture<Boolean> {
        // Run the synchronous MongoDB operation asynchronously.
        return CompletableFuture.supplyAsync {
            // Update only the bmi field.
            val result = users.updateOne(eq("username", username), set("bmi", bmi))
            // matchedCount tells us whether MongoDB found the user.
            result.matchedCount > 0
        }
    }

    // ---------------------------------------------------------------------
    // Returns the calories value for a user on the supplied date.
    //
    // Mongo raw:
    // db.calories.findOne({
    //     userId: userId,
    //     recordDate: date
    // })
    //
    // Returns null when:
    // - the user does not exist
    // - no calories document exists for the supplied date
    // - the calories field is missing
    // ---------------------------------------------------------------------
    override fun getCalories(username: String, date: LocalDate): CompletableFuture<Int?> {
        // Run the synchronous MongoDB operation asynchronously.
        return CompletableFuture.supplyAsync<Int?> {
            // Resolve the username into the user's ObjectId.
            val userId = findUserId(username)

            if (userId == null) { null }
            else {
                // Convert the date to the yyyy-MM-dd format stored in MongoDB.
                val recordDate = date.toString()
                // Find the calories document for the supplied date.
                val document = calories.find(
                    and(
                        eq("userId", userId),
                        eq("recordDate", recordDate)
                    )
                ).first()

                if (document == null) { null }
                else {
                    // Return the stored value without applying an application fallback.
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
    // 1. Find and lock the user.
    // 2. Update the calories document for the supplied date.
    // 3. Insert the document automatically when it does not exist.
    //
    // upsert(true) means:
    // - If the document exists -> update it.
    // - If the document does not exist -> insert it.
    //
    // Calories validation and date selection are handled by the service layer.
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

                    if (userId == null) { false }
                    else {
                        // Convert the date to the yyyy-MM-dd format stored in MongoDB.
                        val recordDate = date.toString()

                        // Update the calories document.
                        // MongoDB inserts the document when it does not exist.
                        this@MongoUserRepository.calories.updateOne(
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
    // Returns all BMI values stored in the users collection.
    //
    // Mongo raw:
    // db.users.find({}, {bmi: 1})
    //
    // BMI classification is handled by the service layer.
    // ---------------------------------------------------------------------
    override fun findAllBmiValues(): CompletableFuture<List<Double>> {
        // Run the synchronous MongoDB operation asynchronously.
        return CompletableFuture.supplyAsync {
            val bmiValues = ArrayList<Double>()

            // Read the BMI value from every user document.
            for (document in users.find()) {
                val bmiNumber =
                    document.get("bmi", Number::class.java) ?: continue

                bmiValues.add(bmiNumber.toDouble())
            }

            bmiValues
        }
    }

    // ---------------------------------------------------------------------
    // Finds the ObjectId belonging to a username.
    // Mongo raw:
    // db.users.findOne({username: username})
    // Returns null when no matching user exists.
    // ---------------------------------------------------------------------
    private fun findUserId(username: String): ObjectId? {
        // Find the matching user document.
        val document = users.find(eq("username", username)).first()
        if (document == null) { return null }
        // Return MongoDB's _id value.
        return document.getObjectId("_id")
    }

    // ---------------------------------------------------------------------
    // Finds the user's ObjectId and updates transactionVersion
    // inside the current MongoDB transaction.
    // All transactions that modify data related to a user call this helper.
    // Updating transactionVersion forces concurrent transactions
    // to write to the same user document.
    // This helps prevent races between operations such as:
    // deleteByUsername()
    // updateWater()
    // updateCalories()
    // updateGoalMl()
    // Mongo raw:
    // db.users.findOneAndUpdate({username: username}, {$inc: {transactionVersion: 1}})
    // Returns the user's ObjectId.
    // Returns null when the user does not exist.
    // ---------------------------------------------------------------------
    private fun lockAndFindUserId(session: ClientSession, username: String): ObjectId? {
        // Find the user and increment transactionVersion
        // inside the current transaction.
        val document = users.findOneAndUpdate(
            session, eq("username", username), inc("transactionVersion", 1))

        // The user does not exist.
        if (document == null) { return null }

        // Return MongoDB's _id value.
        return document.getObjectId("_id")
    }

    // ---------------------------------------------------------------------
    // Converts a MongoDB user document into the existing User model.
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
    // Mongo raw shape:
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
        return Document("username", user.userName).append("passwordHash", user.password).append("fullName", user.fullName).append("age", user.age).append("bmi", user.bmi).append("transactionVersion", 0)
    }
}