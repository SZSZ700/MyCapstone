package org.example.CapstoneProject.repository.mongo;
import com.mongodb.client.ClientSession;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.example.CapstoneProject.repository.WaterRepository;
import org.json.JSONObject;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Filters.gte;
import static com.mongodb.client.model.Filters.lt;
import static com.mongodb.client.model.Updates.inc;
import static com.mongodb.client.model.Updates.set;

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
// Java handles:
// - daily totals
// - missing-day values
// - weekly buckets
// - weekly averages
// - JSONObject response construction
// -------------------------------------------------------------------------
@SuppressWarnings({"unused", "ExtractMethodRecommender"})
@Repository
public class MongoWaterRepository implements WaterRepository {

    // Default water goal used when no goal has been stored.
    private static final int DEFAULT_GOAL_ML = 3000;

    // Hold the users collection.
    private final MongoCollection<Document> users;

    // Hold every individual water drink.
    private final MongoCollection<Document> waterRecords;

    // Hold daily goal history.
    private final MongoCollection<Document> goals;

    private final MongoClient mongoClient;

    // ---------------------------------------------------------------------
    // Builds the repository using the raw MongoDatabase object.
    // ---------------------------------------------------------------------
    public MongoWaterRepository(MongoDatabase database, MongoClient mongoClient) {
        this.mongoClient = mongoClient;
        this.users = database.getCollection("users");
        this.waterRecords = database.getCollection("water_records");
        this.goals = database.getCollection("goals");
    }

    // ---------------------------------------------------------------------
    // Adds one water drink for the user.
    //
    // Mongo raw:
    //
    // db.water_records.insertOne({
    //     userId: userId,
    //     amountMl: waterAmount,
    //     recordedAt: new Date()
    // })
    //
    // Every drink becomes a separate document.
    //
    // Returns false when:
    // - waterAmount <= 0
    // - the user does not exist
    // ---------------------------------------------------------------------
    @Override
    public CompletableFuture<Boolean> updateWater(String username, int waterAmount) {
        // Run the synchronous MongoDB operations asynchronously.
        return CompletableFuture.supplyAsync(() -> {
            // Reject invalid drink amounts.
            if (waterAmount <= 0) { return false; }

            // Open a MongoDB client session.
            try (ClientSession session = mongoClient.startSession()) {
                // Start a new MongoDB transaction.
                session.startTransaction();
                try {

                    // Resolve the username inside the current transaction.
                    ObjectId userId = lockAndFindUserId(session, username);

                    // The user does not exist.
                    if (userId == null) {
                        session.abortTransaction();
                        return false;
                    }

                    // Insert one water record inside the same transaction.
                    waterRecords.insertOne(
                            session,
                            new Document("userId", userId)
                                    .append("amountMl", waterAmount)
                                    .append("recordedAt", new Date())
                    );

                    // Make the inserted water record permanent.
                    session.commitTransaction();
                    return true;
                } catch (Exception e) {
                    // Roll back the transaction if any MongoDB operation fails.
                    session.abortTransaction();
                    throw e;
                }
            }
        });
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
    // Java then separates the documents into today and yesterday.
    //
    // Returns null when the user does not exist.
    // ---------------------------------------------------------------------
    @Override
    public CompletableFuture<JSONObject> getWater(String username) {
        // Run the synchronous MongoDB operation asynchronously.
        return CompletableFuture.supplyAsync(() -> {
            // Resolve the user.
            ObjectId userId = findUserId(username);

            if (userId == null) {
                return null;
            }

            // Get today's date.
            LocalDate today = LocalDate.now();

            // Calculate yesterday.
            LocalDate yesterday = today.minusDays(1);

            // Start at 00:00 yesterday.
            Date start = startOfDay(yesterday);

            // Stop before 00:00 tomorrow.
            Date end = startOfDay(today.plusDays(1));

            // Hold the two calculated totals.
            long todayWater = 0;
            long yesterdayWater = 0;

            // Find only documents inside the required time range.
            for (Document document : waterRecords.find(
                    and(
                            eq("userId", userId),
                            gte("recordedAt", start),
                            lt("recordedAt", end)
                    )
            )) {

                // Read when the drink was recorded.
                Date recordedAt = document.getDate("recordedAt");

                // Read the drink amount.
                Number amount = document.get("amountMl", Number.class);

                // Ignore malformed or incomplete records.
                if (recordedAt == null || amount == null) {
                    continue;
                }

                // Convert MongoDB's timestamp into LocalDate.
                LocalDate recordDate = toLocalDate(recordedAt);

                // Add the amount to today's total.
                if (recordDate.equals(today)) {
                    todayWater += amount.longValue();
                }

                // Add the amount to yesterday's total.
                else if (recordDate.equals(yesterday)) {
                    yesterdayWater += amount.longValue();
                }
            }

            // Build the same JSON response used by the existing application.
            var result = new JSONObject();

            result.put("todayWater", todayWater);
            result.put("yesterdayWater", yesterdayWater);

            return result;
        });
    }

    // ---------------------------------------------------------------------
    // Returns water history for the requested number of days.
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
    // Java pre-creates every requested day with a value of zero.
    //
    // Every drink returned from MongoDB is then added to the correct day.
    //
    // This preserves the original behavior:
    // missing days still appear in the result with 0.
    // ---------------------------------------------------------------------
    @Override
    public CompletableFuture<Map<String, Long>> getWaterHistoryMap(String username, int days) {
        // Run the synchronous MongoDB operation asynchronously.
        return CompletableFuture.supplyAsync(() -> {
            // Resolve the username into the user's ObjectId.
            ObjectId userId = findUserId(username);

            if (userId == null) {
                return null;
            }

            // LinkedHashMap preserves insertion order.
            //
            // The order is:
            // today,
            // yesterday,
            // older days...
            Map<String, Long> result = new LinkedHashMap<>();

            LocalDate today = LocalDate.now();

            // Create every requested date with an initial value of zero.
            for (var i = 0; i < days; i++) {
                result.put(today.minusDays(i).toString(), 0L);
            }

            // When no days were requested, return the empty map.
            if (days <= 0) {
                return result;
            }

            // Calculate the oldest date that should be included.
            LocalDate oldestDate = today.minusDays(days - 1);

            // Start at the beginning of the oldest requested day.
            Date start = startOfDay(oldestDate);

            // Stop before the beginning of tomorrow.
            Date end = startOfDay(today.plusDays(1));

            // Read all drinks inside the requested time range.
            for (Document document : waterRecords.find(
                    and(
                            eq("userId", userId),
                            gte("recordedAt", start),
                            lt("recordedAt", end)
                    )
            )) {

                // Read the drink timestamp.
                Date recordedAt = document.getDate("recordedAt");

                // Read the drink amount.
                Number amount = document.get("amountMl", Number.class);

                // Ignore malformed records.
                if (recordedAt == null || amount == null) {
                    continue;
                }

                // Convert the timestamp into yyyy-MM-dd.
                String dateKey = toLocalDate(recordedAt).toString();

                // Add the drink to the existing total for that date.
                if (result.containsKey(dateKey)) {
                    result.put(dateKey, result.get(dateKey) + amount.longValue());
                }
            }

            return result;
        });
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
    // Java first calculates one total per day.
    //
    // Java then divides the last 28 days into four buckets:
    //
    // Days 0-6   -> Week 4
    // Days 7-13  -> Week 3
    // Days 14-20 -> Week 2
    // Days 21-27 -> Week 1
    //
    // Important:
    // Days without water records are NOT included in the average denominator.
    //
    // This matches the original Firebase behavior.
    // ---------------------------------------------------------------------
    @Override
    public CompletableFuture<Map<String, Integer>> getWeeklyAverages(String username) {
        // Run the synchronous MongoDB operation asynchronously.
        return CompletableFuture.supplyAsync(() -> {
            // Resolve the user.
            ObjectId userId = findUserId(username);

            // Preserve the old behavior for a missing user.
            if (userId == null) {
                return Collections.emptyMap();
            }

            // Today's date.
            LocalDate today = LocalDate.now();

            // The last 28 days include today and the previous 27 days.
            LocalDate oldestDate = today.minusDays(27);

            // Start of the oldest day.
            Date start = startOfDay(oldestDate);

            // Stop before tomorrow.
            Date end = startOfDay(today.plusDays(1));

            // Hold one accumulated total for every day that has water data.
            Map<LocalDate, Long> dailyTotals = new HashMap<>();

            // Read all water documents from the last 28 days.
            for (Document document : waterRecords.find(
                    and(
                            eq("userId", userId),
                            gte("recordedAt", start),
                            lt("recordedAt", end)
                    )
            )) {

                // Read the stored timestamp.
                Date recordedAt = document.getDate("recordedAt");

                // Read the stored drink amount.
                Number amount = document.get("amountMl", Number.class);

                // Ignore invalid records.
                if (recordedAt == null || amount == null) {
                    continue;
                }

                // Determine the LocalDate represented by this record.
                LocalDate recordDate = toLocalDate(recordedAt);

                // Read the current total for that day.
                long currentTotal = dailyTotals.getOrDefault(recordDate, 0L);

                // Add the current drink amount.
                dailyTotals.put(recordDate, currentTotal + amount.longValue());
            }

            // Hold total water per week bucket.
            var sums = new long[4];

            // Hold how many days actually contain water data.
            var counts = new int[4];

            // Visit each of the last 28 days.
            for (var i = 0; i < 28; i++) {
                // Calculate the date represented by this index.
                LocalDate date = today.minusDays(i);

                // Integer division creates groups of seven:
                //
                // 0 / 7 through 6 / 7   -> 0
                // 7 / 7 through 13 / 7  -> 1
                // 14 / 7 through 20 / 7 -> 2
                // 21 / 7 through 27 / 7 -> 3
                int weekIndex = i / 7;

                // Read the already calculated daily total.
                Long amount = dailyTotals.get(date);

                // Missing days must not participate in the average.
                if (amount != null) {
                    sums[weekIndex] += amount;
                    counts[weekIndex]++;
                }
            }

            // Build the final response.
            Map<String, Integer> result = new LinkedHashMap<>();

            // Convert the four internal buckets into Week 4 ... Week 1.
            for (var week = 0; week < 4; week++) {
                // Preserve integer division from the previous implementation.
                var average = counts[week] > 0 ? (int) (sums[week] / counts[week]) : 0;
                // Bucket 0 becomes Week 4,
                // bucket 1 becomes Week 3, etc.
                result.put("Week " + (4 - week), average);
            }

            return result;
        });
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
    @Override
    public CompletableFuture<Integer> getGoalMl(String username) {
        // Run the synchronous MongoDB operation asynchronously.
        return CompletableFuture.supplyAsync(() -> {
            // Resolve the user.
            ObjectId userId = findUserId(username);

            if (userId == null) { return DEFAULT_GOAL_ML; }

            // Find the newest goal for this user.
            Document goalDocument = goals.find(
                    eq("userId", userId))
                    .sort(new Document("recordDate", -1)
            ).first();

            // Use the default when no goal exists.
            if (goalDocument == null) { return DEFAULT_GOAL_ML; }

            // Return the stored goal.
            return goalDocument.getInteger("goalMl", DEFAULT_GOAL_ML);
        });
    }

    // ---------------------------------------------------------------------
    // Updates today's water goal using a MongoDB transaction.
    //
    // The transaction contains:
    // 1. Find the user.
    // 2. Check whether today's goal document already exists.
    // 3. Insert a new goal or update the existing one.
    // 4. Commit all operations together.
    //
    // If any MongoDB operation fails, the transaction is aborted.
    //
    // Mongo raw:
    //
    // session.startTransaction()
    //
    // db.users.findOne({
    //     username: username
    // })
    //
    // db.goals.findOne({
    //     userId: userId,
    //     recordDate: today
    // })
    //
    // If no document exists:
    //
    // db.goals.insertOne({
    //     userId: userId,
    //     goalMl: goalMl,
    //     recordDate: today
    // })
    //
    // If a document already exists:
    //
    // db.goals.updateOne(
    //     {
    //         userId: userId,
    //         recordDate: today
    //     },
    //     {
    //         $set: {
    //             goalMl: goalMl
    //         }
    //     }
    // )
    //
    // session.commitTransaction()
    //
    // Older goal documents remain stored as history.
    //
    // Returns false for invalid values or missing users.
    // ---------------------------------------------------------------------
    @Override
    public CompletableFuture<Boolean> updateGoalMl(String username, int goalMl) {
        // Run the synchronous MongoDB operations asynchronously.
        return CompletableFuture.supplyAsync(() -> {

            // Preserve the original goal validation.
            if (goalMl < 500 || goalMl > 10000) { return false; }

            // Open a MongoDB client session.
            // The session is automatically closed when this block finishes.
            try (ClientSession session = mongoClient.startSession()) {

                // Start a new MongoDB transaction.
                session.startTransaction();

                try {

                    // Resolve the username into MongoDB's user ObjectId
                    // inside the current transaction.
                    ObjectId userId = lockAndFindUserId(session, username);

                    // The user does not exist.
                    if (userId == null) {
                        session.abortTransaction();
                        return false;
                    }

                    // Build today's yyyy-MM-dd key.
                    String today = LocalDate.now().toString();

                    // Check whether a goal already exists for today
                    // inside the same transaction.
                    Document existingGoal = goals.find(
                            session,
                            and(
                                    eq("userId", userId),
                                    eq("recordDate", today)
                            )
                    ).first();

                    if (existingGoal == null) {

                        // No goal exists for today,
                        // therefore insert a new goal history document
                        // inside the transaction.
                        goals.insertOne(
                                session,
                                new Document("userId", userId)
                                        .append("goalMl", goalMl)
                                        .append("recordDate", today)
                        );

                    } else {

                        // Today's goal already exists,
                        // therefore update only the goalMl field
                        // inside the transaction.
                        goals.updateOne(
                                session,
                                and(
                                        eq("userId", userId),
                                        eq("recordDate", today)
                                ),
                                set("goalMl", goalMl)
                        );
                    }

                    // All operations succeeded.
                    // Commit the transaction and make the changes permanent.
                    session.commitTransaction();

                    return true;

                } catch (Exception e) {

                    // A MongoDB operation failed.
                    // Roll back all operations made in this transaction.
                    session.abortTransaction();

                    // Re-throw the exception so the CompletableFuture
                    // completes exceptionally.
                    throw e;
                }
            }
        });
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
    private ObjectId findUserId(String username) {
        // Find the user document.
        Document document = users.find(eq("username", username)).first();

        // User does not exist.
        if (document == null) { return null; }

        // Return MongoDB's generated _id.
        return document.getObjectId("_id");
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
    private Date startOfDay(LocalDate date) {
        return Date.from(date.atStartOfDay(ZoneId.systemDefault()).toInstant());
    }

    // ---------------------------------------------------------------------
    // Converts MongoDB's BSON Date back into LocalDate.
    //
    // The application's local timezone is used when converting
    // the timestamp into a calendar day.
    // ---------------------------------------------------------------------
    private LocalDate toLocalDate(Date date) {
        return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
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
}
