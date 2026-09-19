package org.example.CapstoneProject.repository.mongo;
import com.mongodb.MongoWriteException;
import com.mongodb.client.ClientSession;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.UpdateOptions;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;
import org.example.CapstoneProject.model.User;
import org.example.CapstoneProject.repository.UserRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Updates.*;

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
@SuppressWarnings({"unused", "FieldCanBeLocal"})
@Repository
public class MongoUserRepository implements UserRepository {

    // Hold the users collection.
    private final MongoCollection<Document> users;

    // Hold the calories collection.
    private final MongoCollection<Document> calories;

    // Hold the goals collection.
    private final MongoCollection<Document> goals;

    // Hold the water records collection.
    private final MongoCollection<Document> waterRecords;

    private final MongoClient mongoClient;
    // ---------------------------------------------------------------------
    // Builds the repository using the raw MongoDatabase object.
    //
    // Each collection is retrieved directly from MongoDB.
    // ---------------------------------------------------------------------
    public MongoUserRepository(MongoDatabase database, MongoClient mongoClient) {
        this.mongoClient = mongoClient;
        this.users = database.getCollection("users");
        this.calories = database.getCollection("calories");
        this.goals = database.getCollection("goals");
        this.waterRecords = database.getCollection("water_records");
    }

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
    @Override
    public CompletableFuture<User> findByUsername(String username) {
        // Run the synchronous MongoDB operation asynchronously.
        return CompletableFuture.supplyAsync(() -> {

            // Find the first document whose username matches.
            Document document = users.find(
                    eq("username", username)
            ).first();

            // Return null when no document was found.
            if (document == null) {
                return null;
            }

            // Convert the MongoDB document into the existing User model.
            return mapUser(document);
        });
    }

    // ---------------------------------------------------------------------
    // Returns all users stored in MongoDB.
    // Mongo raw:
    // db.users.find({})
    // ---------------------------------------------------------------------
    @Override
    public CompletableFuture<List<User>> findAll() {
        // Run the synchronous MongoDB operation asynchronously.
        return CompletableFuture.supplyAsync(() -> {
            // Create the result list.
            var result = new ArrayList<User>();

            // MongoDB returns every document in the users collection.
            for (Document document : users.find()) {
                // Convert each document into a User object.
                result.add(mapUser(document));
            }

            return result;
        });
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
    @Override
    public CompletableFuture<Boolean> existsByUsername(String username) {
        // Run the synchronous MongoDB operation asynchronously.
        return CompletableFuture.supplyAsync(() -> {
            // Count documents matching the supplied username.
            long count = users.countDocuments(
                    eq("username", username)
            );

            // A count greater than zero means the user exists.
            return count > 0;
        });
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
    @Override
    public CompletableFuture<Boolean> deleteByUsername(String username) {
        // Run the synchronous MongoDB operations asynchronously.
        return CompletableFuture.supplyAsync(() -> {
            // Open a MongoDB client session.
            try (ClientSession session = mongoClient.startSession()) {
                // Execute the complete delete operation inside one transaction.
                return session.withTransaction(() -> {
                    // Find and lock the user document by incrementing
                    // transactionVersion inside the current transaction.
                    ObjectId userId = lockAndFindUserId(session, username);
                    // The user does not exist.
                    if (userId == null) { return false; }

                    // Delete all calorie records belonging to the user.
                    calories.deleteMany(session, eq("userId", userId));
                    // Delete all goal records belonging to the user.
                    goals.deleteMany(session, eq("userId", userId));
                    // Delete all water records belonging to the user.
                    waterRecords.deleteMany(session, eq("userId", userId));
                    // Delete the user document itself.
                    var result = users.deleteOne(session, eq("_id", userId));

                    // If the user document was not deleted,
                    // abort the transaction so previous deletions are rolled back.
                    if (result.getDeletedCount() == 0) {
                        session.abortTransaction();
                        return false;
                    }

                    // Returning true allows withTransaction()
                    // to commit the complete transaction.
                    return true;
                });
            }
        });
    }

    // ---------------------------------------------------------------------
    // Updates the editable fields of an existing user.
    // The username is used only to locate the user.
    // The username itself is not changed.
    // BMI and all historical data remain unchanged.
    // Mongo raw:
    // db.users.updateOne({ username: username},
    //     {
    //         $set: {
    //             passwordHash: updatedUser.password,
    //             fullName: updatedUser.fullName,
    //             age: updatedUser.age
    //         }
    //     }
    // )
    // Returns the updated user.
    // Returns null when no matching user exists.
    // ---------------------------------------------------------------------
    @Override
    public CompletableFuture<User> updateByUsername(String username, User updatedUser) {
        // Run the synchronous MongoDB operation asynchronously.
        return CompletableFuture.supplyAsync(() -> {
            // Update only the editable fields.
            var result = users.updateOne(eq("username", username),
                    combine(
                            set("passwordHash", updatedUser.getPassword()),
                            set("fullName", updatedUser.getFullName()),
                            set("age", updatedUser.getAge())
                    )
            );

            // matchedCount is zero when no user matched the username.
            if (result.getMatchedCount() == 0) { return null; }

            // Mongo raw:
            // db.users.findOne({
            //     username: username
            // })
            // Read the updated user again so the method can return
            // the complete current user object.
            Document updatedDocument = users.find(
                    eq("username", username)
            ).first();

            if (updatedDocument == null) { return null; }

            return mapUser(updatedDocument);
        });
    }

    // ---------------------------------------------------------------------
    // Partially updates an existing user.
    // PATCH is dynamic, so the update fields depend on what the client sent.
    // Username changes are intentionally ignored.
    //
    // Example Mongo raw:
    // db.users.updateOne(
    //     {username: username},
    //     {$set: {fullName: "New Name",age: 30}}
    // )
    // Returns the updated user.
    // Returns null when no matching user exists.
    // ---------------------------------------------------------------------
    @Override
    public CompletableFuture<User> patchByUsername(String username, Map<String, Object> updates) {
        // Run the synchronous MongoDB operation asynchronously.
        return CompletableFuture.supplyAsync(() -> {
            // Build only the update operations that were actually requested.
            var mongoUpdates = new ArrayList<Bson>();

            // Convert the API field "password"
            // into the MongoDB field "passwordHash".
            if (updates.containsKey("password")) {
                mongoUpdates.add(set("passwordHash", updates.get("password")));
            }

            // Add fullName when supplied.
            if (updates.containsKey("fullName")) {
                mongoUpdates.add(set("fullName", updates.get("fullName")));
            }

            // Add age when supplied.
            if (updates.containsKey("age")) {
                mongoUpdates.add(set("age", ((Number) updates.get("age")).intValue()));
            }

            // Add BMI when supplied.
            if (updates.containsKey("bmi")) {
                mongoUpdates.add(set("bmi", ((Number) updates.get("bmi")).doubleValue()));
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
                Document currentDocument = users.find(
                        eq("username", username)
                ).first();

                if (currentDocument == null) { return null; }

                return mapUser(currentDocument);
            }

            // Mongo raw:
            // db.users.updateOne(
            //     { username: username },
            //     { $set: { dynamic fields here } }
            // )
            var result = users.updateOne(
                    eq("username", username),
                    combine(mongoUpdates)
            );

            // No matching user exists.
            if (result.getMatchedCount() == 0) { return null; }

            // Read the updated document again.
            Document updatedDocument = users
                    .find(eq("username", username))
                    .first();

            if (updatedDocument == null) { return null; }

            return mapUser(updatedDocument);
        });
    }

    // ---------------------------------------------------------------------
    // Creates a new user.
    // Mongo raw:
    // db.users.countDocuments({
    //     username: username
    // })
    //
    // db.users.insertOne({
    //     username: userName,
    //     passwordHash: password,
    //     fullName: fullName,
    //     age: age,
    //     bmi: bmi
    // })
    //
    // Returns false when:
    // - user is null
    // - username is null
    // - username is blank
    // - username already exists
    //
    // A UNIQUE index on username should still exist in MongoDB.
    // ---------------------------------------------------------------------
    @Override
    public CompletableFuture<Boolean> create(User user) {
        // Run the synchronous MongoDB operation asynchronously.
        return CompletableFuture.supplyAsync(() -> {
            // Extract the username safely.
            var username = user != null ? user.getUserName() : null;

            // Reject invalid usernames before contacting MongoDB.
            if (username == null || username.isBlank()) { return false; }

            // Check whether the username already exists.
            if (users.countDocuments(eq("username", username)) > 0) {
                return false;
            }

            try {
                // Insert the new user document.
                users.insertOne(userToDocument(user));
                return true;
            } catch (MongoWriteException e) {

                // MongoDB duplicate key error.
                //
                // This protects against race conditions when two requests
                // try to create the same username at almost the same time.
                if (e.getError().getCode() == 11000) { return false; }

                // Re-throw every other MongoDB write error.
                throw e;
            }
        });
    }

    // ---------------------------------------------------------------------
    // Inserts a new user.
    // Mongo raw:
    // db.users.insertOne({
    //     username: userName,
    //     passwordHash: password,
    //     fullName: fullName,
    //     age: age,
    //     bmi: bmi
    // })
    // This method preserves the original String-based response contract.
    // ---------------------------------------------------------------------
    @Override
    public CompletableFuture<String> insert(User user) {

        return CompletableFuture.supplyAsync(() -> {

            try {
                // Insert the user document.
                users.insertOne(
                        userToDocument(user)
                );

                return "User created successfully";

            } catch (Exception e) {

                // Preserve the original method behavior:
                // return the error as a String instead of failing the Future.
                return "Error: " + e.getMessage();
            }
        });
    }

    // ---------------------------------------------------------------------
    // Returns all users matching the supplied username.
    //
    // Mongo raw:
    //
    // db.users.find({
    //     username: username
    // })
    //
    // With a UNIQUE username index this normally returns
    // either zero users or one user.
    // ---------------------------------------------------------------------
    @Override
    public CompletableFuture<List<User>> findAllByUsername(String username) {
        // Run the synchronous MongoDB operation asynchronously.
        return CompletableFuture.supplyAsync(() -> {

            var result = new ArrayList<User>();

            // Read all matching MongoDB documents.
            for (Document document : users.find(
                    eq("username", username)
            )) {

                // Convert each matching document into a User.
                result.add(mapUser(document));
            }

            return result;
        });
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
    @Override
    public CompletableFuture<Boolean> updateBmi(String username, double bmi) {
        // Run the synchronous MongoDB operation asynchronously.
        return CompletableFuture.supplyAsync(() -> {
            // Update only the bmi field.
            var result = users.updateOne(
                    eq("username", username),
                    set("bmi", bmi)
            );

            // matchedCount tells us whether MongoDB found the user.
            return result.getMatchedCount() > 0;
        });
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
    @Override
    public CompletableFuture<Integer> getCalories(String username) {
        // Run the synchronous MongoDB operation asynchronously.
        return CompletableFuture.supplyAsync(() -> {
            // Resolve the username into the user's ObjectId.
            ObjectId userId = findUserId(username);

            // Missing user behaves like the original implementation.
            if (userId == null) { return 0; }

            // LocalDate.toString() produces yyyy-MM-dd.
            var today = LocalDate.now().toString();

            // Find today's calories document.
            Document document = calories.find(
                    and(
                            eq("userId", userId),
                            eq("recordDate", today)
                    )
            ).first();

            // No calories entry exists for today.
            if (document == null) { return 0; }

            // Return the stored calories value.
            return document.getInteger("calories", 0);
        });
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
    @Override
    public CompletableFuture<Boolean> updateCalories(String username, int caloriesValue) {
        // Run the synchronous MongoDB operations asynchronously.
        return CompletableFuture.supplyAsync(() -> {
            // Preserve the existing calorie validation.
            if (caloriesValue < 0 || caloriesValue > 20000) { return false; }
            // Open a MongoDB client session.
            try (ClientSession session = mongoClient.startSession()) {
                // Execute the complete operation inside one transaction.
                return session.withTransaction(() -> {
                    // Find and lock the user inside the current transaction.
                    ObjectId userId = lockAndFindUserId(session, username);
                    // The user does not exist.
                    if (userId == null) { return false; }

                    // Build today's yyyy-MM-dd key.
                    var today = LocalDate.now().toString();

                    // Update today's calories document.
                    // If it does not exist, MongoDB creates it automatically.
                    calories.updateOne(session,
                            and(
                                    eq("userId", userId),
                                    eq("recordDate", today)
                            ),
                            set("calories", caloriesValue),
                            new UpdateOptions().upsert(true)
                    );

                    // Returning true allows withTransaction()
                    // to commit the transaction.
                    return true;
                });
            }
        });
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
    // The BMI category business logic remains in Java:
    // - Underweight
    // - Normal
    // - Overweight
    // - Obese
    // ---------------------------------------------------------------------
    @Override
    public CompletableFuture<Map<String, Integer>> getBmiDistribution() {
        // Run the synchronous MongoDB operation asynchronously.
        return CompletableFuture.supplyAsync(() -> {
            // Initialize all counters.
            var underweight = 0;
            var normal = 0;
            var overweight = 0;
            var obese = 0;

            // Read all users.
            for (Document document : users.find()) {
                // Read the BMI as Number because BSON numeric values
                // may be represented by different Java numeric types.
                Number bmiNumber = document.get("bmi", Number.class);

                // Ignore users without a BMI value.
                if (bmiNumber == null) { continue; }

                // Convert to primitive double for comparison.
                var bmi = bmiNumber.doubleValue();

                // Apply the same BMI classification used before.
                if (bmi < 18.5) {
                    underweight++;
                } else if (bmi >= 18.5 && bmi < 25.0) {
                    normal++;
                } else if (bmi >= 25.0 && bmi < 30.0) {
                    overweight++;
                } else if (bmi >= 30.0) {
                    obese++;
                }
            }

            // LinkedHashMap preserves the desired output order.
            Map<String, Integer> distribution = new LinkedHashMap<>();

            distribution.put("Underweight", underweight);
            distribution.put("Normal", normal);
            distribution.put("Overweight", overweight);
            distribution.put("Obese", obese);

            return distribution;
        });
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
    private ObjectId findUserId(String username) {

        // Find the matching user document.
        Document document = users.find(
                eq("username", username)
        ).first();

        if (document == null) { return null; }

        // Return MongoDB's _id value.
        return document.getObjectId("_id");
    }

    // ---------------------------------------------------------------------
    // Finds the ObjectId belonging to a username inside a MongoDB session.
    //
    // Mongo raw:
    // db.users.findOne({
    //     username: username
    // })
    // Returns null when no matching user exists.
    // ---------------------------------------------------------------------

    private ObjectId findUserId(ClientSession session, String username) {

        // Find the matching user document using the current transaction session.
        Document document = users.find(
                session,
                eq("username", username)
        ).first();

        if (document == null) { return null; }

        // Return MongoDB's _id value.
        return document.getObjectId("_id");
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
    private ObjectId lockAndFindUserId(ClientSession session, String username) {

        // Find the user and increment transactionVersion
        // inside the current transaction.
        Document document = users.findOneAndUpdate(
                session,
                eq("username", username),
                inc("transactionVersion", 1)
        );

        // The user does not exist.
        if (document == null) { return null; }

        // Return MongoDB's _id value.
        return document.getObjectId("_id");
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
    private User mapUser(Document document) {

        // Create an empty User object.
        var user = new User();

        // Copy String fields.
        user.setUserName(document.getString("username"));
        user.setPassword(document.getString("passwordHash"));
        user.setFullName(document.getString("fullName"));

        // Read numeric fields safely.
        Number age = document.get("age", Number.class);
        Number bmi = document.get("bmi", Number.class);

        // Use safe defaults when a value is missing.
        user.setAge(age == null ? 0 : age.intValue());
        user.setBmi(bmi == null ? 0.0 : bmi.doubleValue());

        return user;
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
    //     bmi: 22.5
    // }
    // ---------------------------------------------------------------------
    private Document userToDocument(User user) {
        return new Document("username", user.getUserName())
                .append("passwordHash", user.getPassword())
                .append("fullName", user.getFullName())
                .append("age", user.getAge())
                .append("bmi", user.getBmi())
                .append("transactionVersion", 0);
    }
}