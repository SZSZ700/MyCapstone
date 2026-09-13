package CapstoneTests;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.example.CapstoneProject.Application;
import org.example.CapstoneProject.model.User;
import org.example.CapstoneProject.service.AuthenticationService;
import org.example.CapstoneProject.service.StatisticsService;
import org.example.CapstoneProject.service.UserHealthService;
import org.example.CapstoneProject.service.UserService;
import org.example.CapstoneProject.service.WaterService;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static com.mongodb.client.model.Filters.eq;
import static org.junit.jupiter.api.Assertions.*;

// -------------------------------------------------------------------------
// End-to-end service integration tests.
//
// These tests load the real Spring application context and exercise the
// service layer together with the real repository implementation and MongoDB.
//
// The tests intentionally call the services instead of the REST controller.
// This lets the suite verify:
//
// - user creation, update and deletion
// - BCrypt password handling
// - signup and login behavior
// - water history
// - calorie history
// - water goals
// - BMI statistics
// - MongoDB document relationships
// - MongoDB transactionVersion behavior
// - transaction-based deletion of related documents
// - concurrency between delete and user-related writes
//
// IMPORTANT:
// MongoDB transactions require the MongoDB server used by these tests
// to run as a replica set. A standalone MongoDB server cannot execute
// the transaction-based repository methods.
// -------------------------------------------------------------------------
@SuppressWarnings("FieldCanBeLocal")
@SpringBootTest(classes = Application.class)
@TestInstance(Lifecycle.PER_CLASS)
@Execution(ExecutionMode.SAME_THREAD)
public class CapstoneServicesIntegrationTest {
    // ---------------------------------------------------------------------
    // Real application services injected from the Spring context.
    // ---------------------------------------------------------------------

    @Autowired
    private UserService userService;

    @Autowired
    private AuthenticationService authenticationService;

    @Autowired
    private WaterService waterService;

    @Autowired
    private UserHealthService userHealthService;

    @Autowired
    private StatisticsService statisticsService;

    // PasswordEncoder is used only to verify that stored BCrypt hashes
    // match the original raw passwords.
    //
    // UserService and AuthenticationService are responsible for performing
    // the actual BCrypt encoding before persistence.
    @Autowired
    private PasswordEncoder passwordEncoder;

    // Raw MongoDatabase access is used only by integration tests that must
    // inspect MongoDB-specific state such as ObjectId relationships,
    // transactionVersion and related collection documents.
    @Autowired
    private MongoDatabase mongoDatabase;

    // Per-run usernames for the two shared baseline users.
    private String TEST_USERNAME_1;
    private String TEST_USERNAME_2;

    // Track all users created by this test class so cleanup can still run
    // even if an individual test fails before deleting its temporary user.
    private final Set<String> createdUsernames =
            Collections.synchronizedSet(new HashSet<>());

    private User testUser1;
    private User testUser2;

    // ---------------------------------------------------------------------
    // TEST LIFECYCLE
    // ---------------------------------------------------------------------

    // ---------------------------------------------------------------------
    // Creates a user directly through UserService.
    //
    // IMPORTANT:
    // UserService.createUser() now performs BCrypt encoding itself.
    //
    // Therefore this helper must receive a User containing the RAW password.
    // The test must not pre-encode the password, otherwise the password would
    // be BCrypt-encoded twice.
    // ---------------------------------------------------------------------
    private void createUserOrFail(User user) throws Exception {

        CompletableFuture<Boolean> future = userService.createUser(user);

        Boolean created = future.get(20, TimeUnit.SECONDS);

        assertTrue(
                created,
                "Failed to create test user: " + user.getUserName()
        );

        createdUsernames.add(user.getUserName());
    }

    // ---------------------------------------------------------------------
    // Returns the MongoDB users collection.
    // ---------------------------------------------------------------------
    private MongoCollection<Document> usersCollection() {
        return mongoDatabase.getCollection("users");
    }

    // ---------------------------------------------------------------------
    // Returns the MongoDB water_records collection.
    // ---------------------------------------------------------------------
    private MongoCollection<Document> waterRecordsCollection() {
        return mongoDatabase.getCollection("water_records");
    }

    // ---------------------------------------------------------------------
    // Returns the MongoDB calories collection.
    // ---------------------------------------------------------------------
    private MongoCollection<Document> caloriesCollection() {
        return mongoDatabase.getCollection("calories");
    }

    // ---------------------------------------------------------------------
    // Returns the MongoDB goals collection.
    // ---------------------------------------------------------------------
    private MongoCollection<Document> goalsCollection() {
        return mongoDatabase.getCollection("goals");
    }

    // ---------------------------------------------------------------------
    // Resolves a username into its MongoDB ObjectId.
    //
    // Returns null when no matching user exists.
    // ---------------------------------------------------------------------
    private ObjectId getUserIdFromMongo(String username) {

        Document document = usersCollection().find(
                eq("username", username)
        ).first();

        if (document == null) { return null; }

        return document.getObjectId("_id");
    }

    // ---------------------------------------------------------------------
    // Reads the internal transactionVersion field from the user document.
    //
    // The field is used by repository write transactions so operations such
    // as delete, water updates, calorie updates and goal updates write to the
    // same user document and therefore participate in MongoDB write-conflict
    // detection.
    //
    // Returns -1 when the user does not exist.
    // ---------------------------------------------------------------------
    private long getTransactionVersion(String username) {

        Document document = usersCollection().find(
                eq("username", username)
        ).first();

        if (document == null) { return -1; }

        Number version = document.get("transactionVersion", Number.class);

        return version == null ? 0 : version.longValue();
    }

    @BeforeAll
    void setUpTestUsers() throws Exception {

        String runId = String.valueOf(System.currentTimeMillis());

        TEST_USERNAME_1 = "integrationUser1_" + runId;
        TEST_USERNAME_2 = "integrationUser2_" + runId;

        testUser1 = new User();
        testUser1.setUserName(TEST_USERNAME_1);
        testUser1.setPassword("pass1");

        testUser2 = new User();
        testUser2.setUserName(TEST_USERNAME_2);
        testUser2.setPassword("pass2");

        createUserOrFail(testUser1);
        createUserOrFail(testUser2);
    }

    @AfterAll
    void cleanUpTestUsers() {

        for (String username : new ArrayList<>(createdUsernames)) {

            try {
                userService.deleteUser(username).get(20, TimeUnit.SECONDS);
            } catch (Exception e) {
                System.out.println(
                        "WARN cleanup failed for username=" +
                                username +
                                " message=" +
                                e.getMessage()
                );
            }
        }
    }

    // ---------------------------------------------------------------------
    // SIGNUP / CREATE / DELETE TESTS
    // ---------------------------------------------------------------------

    // ---------------------------------------------------------------------
    // Verifies that signup:
    // - creates a new user
    // - stores a BCrypt hash instead of the raw password
    // - rejects a duplicate username
    // ---------------------------------------------------------------------
    @Test
    void signup_createsNewUserAndRejectsDuplicate() throws Exception {

        String uniqueUsername =
                "signupUser_" + System.currentTimeMillis();

        String rawPassword = "signupPass";

        User signupUser = new User();
        signupUser.setUserName(uniqueUsername);
        signupUser.setPassword(rawPassword);
        signupUser.setFullName("Sasa li");
        signupUser.setAge(25);

        String firstResult = authenticationService
                .signup(signupUser)
                .get(20, TimeUnit.SECONDS);

        assertEquals("User created successfully", firstResult);

        createdUsernames.add(uniqueUsername);

        User storedUser = userService
                .getUser(uniqueUsername)
                .get(20, TimeUnit.SECONDS);

        assertNotNull(storedUser);
        assertNotEquals(rawPassword, storedUser.getPassword());
        assertTrue(
                passwordEncoder.matches(
                        rawPassword,
                        storedUser.getPassword()
                )
        );

        String secondResult = authenticationService
                .signup(signupUser)
                .get(20, TimeUnit.SECONDS);

        assertEquals("Username already exists", secondResult);
    }

    // ---------------------------------------------------------------------
    // Verifies concurrent signup protection.
    //
    // Two signup requests try to create the same username at the same time.
    //
    // Expected:
    // - exactly one request succeeds
    // - the other request reports that the username already exists
    // - MongoDB contains exactly one document with that username
    //
    // The UNIQUE username index is the final protection against the race.
    // ---------------------------------------------------------------------
    @Test
    void signup_concurrentDuplicateRequests_onlyOneUserIsCreated() throws Exception {

        String username =
                "concurrentSignup_" + System.currentTimeMillis();

        User userA = new User();
        userA.setUserName(username);
        userA.setPassword("passA");

        User userB = new User();
        userB.setUserName(username);
        userB.setPassword("passB");

        CountDownLatch start = new CountDownLatch(1);

        CompletableFuture<String> requestA =
                CompletableFuture.supplyAsync(() -> {
                    try {
                        start.await();

                        return authenticationService
                                .signup(userA)
                                .get(20, TimeUnit.SECONDS);

                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                });

        CompletableFuture<String> requestB =
                CompletableFuture.supplyAsync(() -> {
                    try {
                        start.await();

                        return authenticationService
                                .signup(userB)
                                .get(20, TimeUnit.SECONDS);

                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                });

        start.countDown();

        String resultA = requestA.get(20, TimeUnit.SECONDS);
        String resultB = requestB.get(20, TimeUnit.SECONDS);

        int successCount = 0;
        int duplicateCount = 0;

        if ("User created successfully".equals(resultA)) { successCount++; }
        if ("User created successfully".equals(resultB)) { successCount++; }

        if ("Username already exists".equals(resultA)) { duplicateCount++; }
        if ("Username already exists".equals(resultB)) { duplicateCount++; }

        assertEquals(1, successCount);
        assertEquals(1, duplicateCount);

        assertEquals(
                1,
                usersCollection().countDocuments(
                        eq("username", username)
                )
        );

        createdUsernames.add(username);
    }

    // ---------------------------------------------------------------------
    // Verifies direct user creation, existence checking and deletion.
    //
    // UserService receives the RAW password and performs BCrypt encoding.
    // ---------------------------------------------------------------------
    @Test
    void createUser_existsAndDeleteUser_flowWorks() throws Exception {

        String tempUsername =
                "tempUser_" + System.currentTimeMillis();

        String rawPassword = "tempPass";

        User tempUser = new User();
        tempUser.setUserName(tempUsername);
        tempUser.setPassword(rawPassword);
        tempUser.setFullName("Sasa li");
        tempUser.setAge(25);

        Boolean created = userService
                .createUser(tempUser)
                .get(20, TimeUnit.SECONDS);

        assertTrue(created);

        createdUsernames.add(tempUsername);

        User storedUser = userService
                .getUser(tempUsername)
                .get(20, TimeUnit.SECONDS);

        assertNotNull(storedUser);
        assertNotEquals(rawPassword, storedUser.getPassword());
        assertTrue(
                passwordEncoder.matches(
                        rawPassword,
                        storedUser.getPassword()
                )
        );

        assertTrue(
                userService
                        .exists(tempUsername)
                        .get(20, TimeUnit.SECONDS)
        );

        assertTrue(
                userService
                        .deleteUser(tempUsername)
                        .get(20, TimeUnit.SECONDS)
        );

        assertFalse(
                userService
                        .exists(tempUsername)
                        .get(20, TimeUnit.SECONDS)
        );
    }

    // ---------------------------------------------------------------------
    // Verifies that getUser returns null for a username that does not exist.
    // ---------------------------------------------------------------------
    @Test
    void getUser_nonExisting_returnsNull() throws Exception {

        String missingUsername =
                "getUserNoSuch_" + System.currentTimeMillis();

        User result = userService
                .getUser(missingUsername)
                .get(20, TimeUnit.SECONDS);

        assertNull(result);
    }

    // ---------------------------------------------------------------------
    // USER UPDATE TESTS
    // ---------------------------------------------------------------------

    // ---------------------------------------------------------------------
    // Verifies that updateUser:
    // - updates password, fullName and age
    // - stores the new password as BCrypt
    // - keeps the username unchanged
    // - returns the complete updated user
    // ---------------------------------------------------------------------
    @Test
    void updateUser_existing_updatesEditableFieldsAndReturnsUpdatedUser()
            throws Exception {

        String tempUsername =
                "updateUserDeep_" + System.currentTimeMillis();

        User originalUser = new User();
        originalUser.setUserName(tempUsername);
        originalUser.setPassword("origPass");
        originalUser.setFullName("Original Name");
        originalUser.setAge(20);

        createUserOrFail(originalUser);

        User updatedUser = new User();
        updatedUser.setUserName(tempUsername);
        updatedUser.setPassword("newPass");
        updatedUser.setFullName("Updated Name");
        updatedUser.setAge(30);

        User updated = userService
                .updateUser(tempUsername, updatedUser)
                .get(20, TimeUnit.SECONDS);

        assertNotNull(updated);
        assertEquals(tempUsername, updated.getUserName());
        assertNotEquals("newPass", updated.getPassword());

        assertTrue(
                passwordEncoder.matches(
                        "newPass",
                        updated.getPassword()
                )
        );

        assertEquals("Updated Name", updated.getFullName());
        assertEquals(30, updated.getAge());

        User fromDb = userService
                .getUser(tempUsername)
                .get(20, TimeUnit.SECONDS);

        assertNotNull(fromDb);
        assertNotEquals("newPass", fromDb.getPassword());

        assertTrue(
                passwordEncoder.matches(
                        "newPass",
                        fromDb.getPassword()
                )
        );

        assertEquals("Updated Name", fromDb.getFullName());
        assertEquals(30, fromDb.getAge());
        assertNotEquals("origPass", fromDb.getPassword());
        assertNotEquals("Original Name", fromDb.getFullName());
        assertNotEquals(20, fromDb.getAge());

        assertTrue(
                userService
                        .deleteUser(tempUsername)
                        .get(20, TimeUnit.SECONDS)
        );
    }

    // ---------------------------------------------------------------------
    // Verifies that updating a missing user returns null.
    // ---------------------------------------------------------------------
    @Test
    void updateUser_nonExisting_returnsNull() throws Exception {

        String missingUsername =
                "updateUserNoSuch_" + System.currentTimeMillis();

        User candidate = new User();
        candidate.setUserName(missingUsername);
        candidate.setPassword("somePass");
        candidate.setFullName("Some Name");
        candidate.setAge(40);

        User updated = userService
                .updateUser(missingUsername, candidate)
                .get(20, TimeUnit.SECONDS);

        assertNull(updated);

        User fromDb = userService
                .getUser(missingUsername)
                .get(20, TimeUnit.SECONDS);

        assertNull(fromDb);
    }

    // ---------------------------------------------------------------------
    // LOGIN TESTS
    // ---------------------------------------------------------------------

    // ---------------------------------------------------------------------
    // Verifies successful login using a raw password against the stored
    // BCrypt hash.
    // ---------------------------------------------------------------------
    @Test
    void login_withCorrectCredentials_returnsUser() throws Exception {

        User loggedUser = authenticationService
                .login(TEST_USERNAME_1, "pass1")
                .get(20, TimeUnit.SECONDS);

        assertNotNull(loggedUser);
        assertEquals(TEST_USERNAME_1, loggedUser.getUserName());
        assertNotEquals("pass1", loggedUser.getPassword());

        assertTrue(
                passwordEncoder.matches(
                        "pass1",
                        loggedUser.getPassword()
                )
        );
    }

    // ---------------------------------------------------------------------
    // Verifies that login fails when the raw password is incorrect.
    // ---------------------------------------------------------------------
    @Test
    void login_withWrongPassword_returnsNull() throws Exception {

        User loggedUser = authenticationService
                .login(TEST_USERNAME_1, "wrongPass")
                .get(20, TimeUnit.SECONDS);

        assertNull(loggedUser);
    }

    // ---------------------------------------------------------------------
    // WATER MODULE TESTS
    // ---------------------------------------------------------------------

    // ---------------------------------------------------------------------
    // Verifies that updateWater inserts a new water record and that both
    // getWater() and getWaterHistoryMap() reflect the new amount.
    // ---------------------------------------------------------------------
    @Test
    void updateWater_increasesTodayTotal_and_getWaterIsConsistent()
            throws Exception {

        JSONObject beforeJson = waterService
                .getWater(TEST_USERNAME_1)
                .get(20, TimeUnit.SECONDS);

        assertNotNull(beforeJson);

        long todayBefore = beforeJson.getLong("todayWater");

        int addedAmount = 500;

        Boolean updated = waterService
                .updateWater(TEST_USERNAME_1, addedAmount)
                .get(20, TimeUnit.SECONDS);

        assertTrue(updated);

        JSONObject afterJson = waterService
                .getWater(TEST_USERNAME_1)
                .get(20, TimeUnit.SECONDS);

        assertNotNull(afterJson);

        long todayAfter = afterJson.getLong("todayWater");

        assertEquals(todayBefore + addedAmount, todayAfter);

        String todayKey = LocalDate.now().toString();

        Map<String, Long> history = waterService
                .getWaterHistoryMap(TEST_USERNAME_1, 3)
                .get(20, TimeUnit.SECONDS);

        assertNotNull(history);
        assertEquals(3, history.size());
        assertTrue(history.containsKey(todayKey));
        assertEquals(todayAfter, history.get(todayKey));
    }

    // ---------------------------------------------------------------------
    // Verifies that a fresh user has zero water totals for all requested
    // history days.
    // ---------------------------------------------------------------------
    @Test
    void getWaterHistoryMap_forNewUser_returnsAllZerosWithExpectedKeys()
            throws Exception {

        int days = 7;

        Map<String, Long> expected = new LinkedHashMap<>();

        LocalDate today = LocalDate.now();

        for (int i = 0; i < days; i++) {
            expected.put(
                    today.minusDays(i).toString(),
                    0L
            );
        }

        Map<String, Long> actual = waterService
                .getWaterHistoryMap(TEST_USERNAME_2, days)
                .get(20, TimeUnit.SECONDS);

        assertNotNull(actual);
        assertEquals(expected, actual);
    }

    // ---------------------------------------------------------------------
    // Verifies that a fresh user with no water records returns zero for
    // both today and yesterday.
    // ---------------------------------------------------------------------
    @Test
    void getWater_forNewUser_returnsZeroTotals() throws Exception {

        JSONObject json = waterService
                .getWater(TEST_USERNAME_2)
                .get(20, TimeUnit.SECONDS);

        assertNotNull(json);
        assertEquals(0, json.getLong("todayWater"));
        assertEquals(0, json.getLong("yesterdayWater"));
    }

    // ---------------------------------------------------------------------
    // Verifies the MongoDB date-range behavior used by getWater().
    //
    // The test inserts one record for today and one for yesterday directly
    // into MongoDB, then verifies that the service places each amount into
    // the correct day.
    // ---------------------------------------------------------------------
    @Test
    void getWater_withTodayAndYesterdayMongoRecords_returnsCorrectTotals()
            throws Exception {

        String username =
                "waterDateTest_" + System.currentTimeMillis();

        User user = new User();
        user.setUserName(username);
        user.setPassword("waterDatePass");

        createUserOrFail(user);

        ObjectId userId = getUserIdFromMongo(username);

        assertNotNull(userId);

        LocalDate today = LocalDate.now();
        LocalDate yesterday = today.minusDays(1);

        Date todayTime = Date.from(
                today
                        .atTime(12, 0)
                        .atZone(ZoneId.systemDefault())
                        .toInstant()
        );

        Date yesterdayTime = Date.from(
                yesterday
                        .atTime(12, 0)
                        .atZone(ZoneId.systemDefault())
                        .toInstant()
        );

        waterRecordsCollection().insertOne(
                new Document("userId", userId)
                        .append("amountMl", 700)
                        .append("recordedAt", todayTime)
        );

        waterRecordsCollection().insertOne(
                new Document("userId", userId)
                        .append("amountMl", 400)
                        .append("recordedAt", yesterdayTime)
        );

        JSONObject result = waterService
                .getWater(username)
                .get(20, TimeUnit.SECONDS);

        assertNotNull(result);
        assertEquals(700, result.getLong("todayWater"));
        assertEquals(400, result.getLong("yesterdayWater"));
    }

    // ---------------------------------------------------------------------
    // GOAL MODULE TESTS
    // ---------------------------------------------------------------------

    // ---------------------------------------------------------------------
    // Verifies that updateGoalMl changes today's goal and that getGoalMl
    // reads the same value.
    // ---------------------------------------------------------------------
    @Test
    void updateGoalMl_changesGoal_and_getGoalMlReadsIt() throws Exception {

        int newGoal = 3200;

        Boolean updated = waterService
                .updateGoalMl(TEST_USERNAME_1, newGoal)
                .get(20, TimeUnit.SECONDS);

        assertTrue(updated);

        Integer goalValue = waterService
                .getGoalMl(TEST_USERNAME_1)
                .get(20, TimeUnit.SECONDS);

        assertNotNull(goalValue);
        assertEquals(newGoal, goalValue.intValue());
    }

    // ---------------------------------------------------------------------
    // Verifies that invalid goal values are rejected and do not change the
    // user's stored goal.
    // ---------------------------------------------------------------------
    @Test
    void updateGoalMl_outOfRange_isRejectedAndValueNotChanged()
            throws Exception {

        String username =
                "goalInvalidDeep_" + System.currentTimeMillis();

        User user = new User();
        user.setUserName(username);
        user.setPassword("p");

        createUserOrFail(user);

        Integer before = waterService
                .getGoalMl(username)
                .get(20, TimeUnit.SECONDS);

        assertEquals(3000, before.intValue());

        Boolean low = waterService
                .updateGoalMl(username, 100)
                .get(20, TimeUnit.SECONDS);

        Boolean high = waterService
                .updateGoalMl(username, 20000)
                .get(20, TimeUnit.SECONDS);

        assertFalse(low);
        assertFalse(high);

        Integer after = waterService
                .getGoalMl(username)
                .get(20, TimeUnit.SECONDS);

        assertEquals(before.intValue(), after.intValue());
    }

    // ---------------------------------------------------------------------
    // CALORIES MODULE TESTS
    // ---------------------------------------------------------------------

    // ---------------------------------------------------------------------
    // Verifies that updateCalories stores today's value and getCalories
    // returns the same value.
    // ---------------------------------------------------------------------
    @Test
    void updateCalories_setsValue_and_getCaloriesReadsIt() throws Exception {

        int newCalories = 1234;

        Boolean updated = userHealthService
                .updateCalories(TEST_USERNAME_1, newCalories)
                .get(20, TimeUnit.SECONDS);

        assertTrue(updated);

        Integer calories = userHealthService
                .getCalories(TEST_USERNAME_1)
                .get(20, TimeUnit.SECONDS);

        assertNotNull(calories);
        assertEquals(newCalories, calories.intValue());
    }

    // ---------------------------------------------------------------------
    // Verifies that a fresh user has zero calories for today.
    // ---------------------------------------------------------------------
    @Test
    void getCalories_forNewUser_returnsZero() throws Exception {

        Integer calories = userHealthService
                .getCalories(TEST_USERNAME_2)
                .get(20, TimeUnit.SECONDS);

        assertNotNull(calories);
        assertEquals(0, calories.intValue());
    }

    // ---------------------------------------------------------------------
    // Verifies that getCalories returns zero for a missing user.
    // ---------------------------------------------------------------------
    @Test
    void getCalories_userNotFound_returnsZero() throws Exception {

        String missingUsername =
                "noSuchUser_" + System.currentTimeMillis();

        Integer calories = userHealthService
                .getCalories(missingUsername)
                .get(20, TimeUnit.SECONDS);

        assertEquals(0, calories.intValue());
    }

    // ---------------------------------------------------------------------
    // Verifies valid and invalid calorie updates.
    //
    // Invalid values must not overwrite the last valid value.
    // ---------------------------------------------------------------------
    @Test
    void updateCalories_validAndInvalidValues_behaveAsExpected()
            throws Exception {

        String username =
                "calDeep_" + System.currentTimeMillis();

        User user = new User();
        user.setUserName(username);
        user.setPassword("p");

        createUserOrFail(user);

        Integer initial = userHealthService
                .getCalories(username)
                .get(20, TimeUnit.SECONDS);

        assertEquals(0, initial.intValue());

        Boolean validUpdated = userHealthService
                .updateCalories(username, 1200)
                .get(20, TimeUnit.SECONDS);

        assertTrue(validUpdated);

        Integer afterValid = userHealthService
                .getCalories(username)
                .get(20, TimeUnit.SECONDS);

        assertEquals(1200, afterValid.intValue());

        Boolean invalidLow = userHealthService
                .updateCalories(username, -5)
                .get(20, TimeUnit.SECONDS);

        Boolean invalidHigh = userHealthService
                .updateCalories(username, 50000)
                .get(20, TimeUnit.SECONDS);

        assertFalse(invalidLow);
        assertFalse(invalidHigh);

        Integer afterInvalid = userHealthService
                .getCalories(username)
                .get(20, TimeUnit.SECONDS);

        assertEquals(1200, afterInvalid.intValue());
    }

    // ---------------------------------------------------------------------
    // BMI DISTRIBUTION TEST
    // ---------------------------------------------------------------------

    // ---------------------------------------------------------------------
    // Verifies that the global BMI distribution correctly counts one new
    // user in each BMI category.
    // ---------------------------------------------------------------------
    @Test
    void getBmiDistribution_countsEachBmiCategoryForNewUsers()
            throws Exception {

        Map<String, Integer> before = statisticsService
                .getBmiDistribution()
                .get(20, TimeUnit.SECONDS);

        assertNotNull(before);

        var underBefore = before.getOrDefault("Underweight", 0);
        var normalBefore = before.getOrDefault("Normal", 0);
        var overBefore = before.getOrDefault("Overweight", 0);
        var obeseBefore = before.getOrDefault("Obese", 0);

        String prefix =
                "bmiTestUser_" + System.currentTimeMillis();

        String[] bmiUsers = new String[]{
                prefix + "_u",
                prefix + "_n",
                prefix + "_o",
                prefix + "_ob"
        };

        double[] bmiValues = new double[]{
                17.0,
                22.0,
                27.0,
                32.0
        };

        for (int i = 0; i < bmiUsers.length; i++) {

            User user = new User();
            user.setUserName(bmiUsers[i]);
            user.setPassword("bmiPass");

            createUserOrFail(user);

            Boolean bmiUpdated = userHealthService
                    .updateBmi(
                            bmiUsers[i],
                            bmiValues[i]
                    )
                    .get(20, TimeUnit.SECONDS);

            assertTrue(bmiUpdated);
        }

        Map<String, Integer> after = statisticsService
                .getBmiDistribution()
                .get(20, TimeUnit.SECONDS);

        assertNotNull(after);

        var underAfter = after.getOrDefault("Underweight", 0);
        var normalAfter = after.getOrDefault("Normal", 0);
        var overAfter = after.getOrDefault("Overweight", 0);
        var obeseAfter = after.getOrDefault("Obese", 0);

        assertEquals(underBefore + 1, underAfter);
        assertEquals(normalBefore + 1, normalAfter);
        assertEquals(overBefore + 1, overAfter);
        assertEquals(obeseBefore + 1, obeseAfter);
    }

    // ---------------------------------------------------------------------
    // MONGODB TRANSACTION / CONCURRENCY TESTS
    // ---------------------------------------------------------------------

    // ---------------------------------------------------------------------
    // Verifies that all user-related write transactions increment the same
    // transactionVersion field on the user document.
    //
    // This confirms that:
    //
    // updateWater()
    // updateCalories()
    // updateGoalMl()
    //
    // all perform a write against the same users document before writing
    // their related collection data.
    //
    // This shared write is what allows MongoDB to detect write conflicts
    // against deleteByUsername() when operations overlap.
    // ---------------------------------------------------------------------
    @Test
    void userRelatedWriteTransactions_incrementTransactionVersion()
            throws Exception {

        String username =
                "transactionVersion_" + System.currentTimeMillis();

        User user = new User();
        user.setUserName(username);
        user.setPassword("transactionPass");

        createUserOrFail(user);

        long initialVersion = getTransactionVersion(username);

        assertTrue(initialVersion >= 0);

        assertTrue(
                waterService
                        .updateWater(username, 250)
                        .get(20, TimeUnit.SECONDS)
        );

        long afterWater = getTransactionVersion(username);

        assertEquals(initialVersion + 1, afterWater);

        assertTrue(
                userHealthService
                        .updateCalories(username, 1800)
                        .get(20, TimeUnit.SECONDS)
        );

        long afterCalories = getTransactionVersion(username);

        assertEquals(afterWater + 1, afterCalories);

        assertTrue(
                waterService
                        .updateGoalMl(username, 3000)
                        .get(20, TimeUnit.SECONDS)
        );

        long afterGoal = getTransactionVersion(username);

        assertEquals(afterCalories + 1, afterGoal);
    }

    // ---------------------------------------------------------------------
    // Verifies the delete transaction across all related MongoDB collections.
    //
    // Before deletion, the test creates:
    // - one user
    // - one water record
    // - one calories record
    // - one goal record
    //
    // deleteUser() must remove everything as one logical operation.
    // ---------------------------------------------------------------------
    @Test
    void deleteUser_removesRelatedMongoDocuments() throws Exception {

        String username =
                "deleteTransaction_" + System.currentTimeMillis();

        User user = new User();
        user.setUserName(username);
        user.setPassword("deletePass");

        createUserOrFail(user);

        ObjectId userId = getUserIdFromMongo(username);

        assertNotNull(userId);

        assertTrue(
                waterService
                        .updateWater(username, 500)
                        .get(20, TimeUnit.SECONDS)
        );

        assertTrue(
                userHealthService
                        .updateCalories(username, 2200)
                        .get(20, TimeUnit.SECONDS)
        );

        assertTrue(
                waterService
                        .updateGoalMl(username, 3300)
                        .get(20, TimeUnit.SECONDS)
        );

        assertTrue(
                waterRecordsCollection().countDocuments(
                        eq("userId", userId)
                ) > 0
        );

        assertTrue(
                caloriesCollection().countDocuments(
                        eq("userId", userId)
                ) > 0
        );

        assertTrue(
                goalsCollection().countDocuments(
                        eq("userId", userId)
                ) > 0
        );

        Boolean deleted = userService
                .deleteUser(username)
                .get(20, TimeUnit.SECONDS);

        assertTrue(deleted);

        assertNull(
                usersCollection().find(
                        eq("_id", userId)
                ).first()
        );

        assertEquals(
                0,
                waterRecordsCollection().countDocuments(
                        eq("userId", userId)
                )
        );

        assertEquals(
                0,
                caloriesCollection().countDocuments(
                        eq("userId", userId)
                )
        );

        assertEquals(
                0,
                goalsCollection().countDocuments(
                        eq("userId", userId)
                )
        );
    }

    // ---------------------------------------------------------------------
    // Verifies concurrency between deleteByUsername() and updateWater().
    //
    // Both operations use a transaction and both write transactionVersion
    // on the same user document.
    //
    // Under real concurrency, MongoDB may allow one transaction to commit
    // and make the other transaction fail with a transient write conflict.
    //
    // This test does NOT require both operations to succeed.
    //
    // The important consistency rule is:
    //
    // If the user was deleted, there must not be any water record left
    // referencing the deleted user's ObjectId.
    //
    // That is the orphan-document race condition we want to prevent.
    // ---------------------------------------------------------------------
    @Test
    void concurrentDeleteAndWaterUpdate_neverLeaveOrphanWaterRecord()
            throws Exception {

        String username =
                "deleteWaterRace_" + System.currentTimeMillis();

        User user = new User();
        user.setUserName(username);
        user.setPassword("racePass");

        createUserOrFail(user);

        ObjectId userId = getUserIdFromMongo(username);

        assertNotNull(userId);

        CountDownLatch start = new CountDownLatch(1);

        CompletableFuture<Object> deleteAttempt =
                CompletableFuture.supplyAsync(() -> {
                    try {
                        start.await();

                        return userService
                                .deleteUser(username)
                                .get(20, TimeUnit.SECONDS);

                    } catch (Exception e) {
                        return e;
                    }
                });

        CompletableFuture<Object> waterAttempt =
                CompletableFuture.supplyAsync(() -> {
                    try {
                        start.await();

                        return waterService
                                .updateWater(username, 650)
                                .get(20, TimeUnit.SECONDS);

                    } catch (Exception e) {
                        return e;
                    }
                });

        start.countDown();

        deleteAttempt.get(20, TimeUnit.SECONDS);
        waterAttempt.get(20, TimeUnit.SECONDS);

        Document userDocument = usersCollection().find(
                eq("_id", userId)
        ).first();

        long waterCount = waterRecordsCollection().countDocuments(
                eq("userId", userId)
        );

        // If delete won and the user no longer exists, the transaction
        // protection must ensure that no orphan water record remains.
        if (userDocument == null) {
            assertEquals(0, waterCount);
        }

        // If the write transaction won and delete failed because of a
        // transaction conflict, the user may still exist. That state is
        // consistent because the related water document still references
        // a valid user.
        else {
            assertEquals(username, userDocument.getString("username"));
        }
    }

    // ---------------------------------------------------------------------
    // Verifies concurrency between deleteByUsername() and the two daily
    // history write operations:
    //
    // - updateCalories()
    // - updateGoalMl()
    //
    // The test starts all operations together.
    //
    // Some transactions may fail because MongoDB detects a write conflict.
    // That is acceptable.
    //
    // The required invariant is:
    //
    // When the user document is gone, no calories or goal document may
    // remain with that deleted userId.
    // ---------------------------------------------------------------------
    @Test
    void concurrentDeleteCaloriesAndGoalUpdates_neverLeaveOrphanDocuments()
            throws Exception {

        String username =
                "deleteHealthRace_" + System.currentTimeMillis();

        User user = new User();
        user.setUserName(username);
        user.setPassword("racePass");

        createUserOrFail(user);

        ObjectId userId = getUserIdFromMongo(username);

        assertNotNull(userId);

        CountDownLatch start = new CountDownLatch(1);

        CompletableFuture<Object> deleteAttempt =
                CompletableFuture.supplyAsync(() -> {
                    try {
                        start.await();

                        return userService
                                .deleteUser(username)
                                .get(20, TimeUnit.SECONDS);

                    } catch (Exception e) {
                        return e;
                    }
                });

        CompletableFuture<Object> caloriesAttempt =
                CompletableFuture.supplyAsync(() -> {
                    try {
                        start.await();

                        return userHealthService
                                .updateCalories(username, 2100)
                                .get(20, TimeUnit.SECONDS);

                    } catch (Exception e) {
                        return e;
                    }
                });

        CompletableFuture<Object> goalAttempt =
                CompletableFuture.supplyAsync(() -> {
                    try {
                        start.await();

                        return waterService
                                .updateGoalMl(username, 3400)
                                .get(20, TimeUnit.SECONDS);

                    } catch (Exception e) {
                        return e;
                    }
                });

        start.countDown();

        deleteAttempt.get(20, TimeUnit.SECONDS);
        caloriesAttempt.get(20, TimeUnit.SECONDS);
        goalAttempt.get(20, TimeUnit.SECONDS);

        Document userDocument = usersCollection().find(
                eq("_id", userId)
        ).first();

        long caloriesCount = caloriesCollection().countDocuments(
                eq("userId", userId)
        );

        long goalsCount = goalsCollection().countDocuments(
                eq("userId", userId)
        );

        // A deleted user must never have related orphan documents.
        if (userDocument == null) {
            assertEquals(0, caloriesCount);
            assertEquals(0, goalsCount);
        }

        // If delete lost the transaction conflict, the user is still valid.
        else {
            assertEquals(username, userDocument.getString("username"));
        }
    }
}
