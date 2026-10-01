@file:Suppress("PackageName")

package CapstoneTests
import com.mongodb.client.MongoCollection
import com.mongodb.client.MongoDatabase
import com.mongodb.client.model.Filters.eq
import org.bson.Document
import org.bson.types.ObjectId
import org.example.CapstoneProject.Application
import org.example.CapstoneProject.model.User
import org.example.CapstoneProject.service.AuthenticationService
import org.example.CapstoneProject.service.StatisticsService
import org.example.CapstoneProject.service.UserHealthService
import org.example.CapstoneProject.service.UserService
import org.example.CapstoneProject.service.WaterService
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.security.crypto.password.PasswordEncoder
import java.time.LocalDate
import java.time.ZoneId
import java.util.Collections
import java.util.Date
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

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

// Load the complete Spring Boot application context.
@SpringBootTest(classes = [Application::class])

// Use one test class instance so @BeforeAll and @AfterAll can be regular methods.
@TestInstance(TestInstance.Lifecycle.PER_CLASS)

// Run the test methods on the same JUnit execution thread.
@Execution(ExecutionMode.SAME_THREAD)
class CapstoneServicesIntegrationTest {

    // ---------------------------------------------------------------------
    // Real application services injected from the Spring context.
    // ---------------------------------------------------------------------

    // Ask Spring to inject the real UserService bean.
    @Autowired
    private lateinit var userService: UserService

    // Ask Spring to inject the real AuthenticationService bean.
    @Autowired
    private lateinit var authenticationService: AuthenticationService

    // Ask Spring to inject the real WaterService bean.
    @Autowired
    private lateinit var waterService: WaterService

    // Ask Spring to inject the real UserHealthService bean.
    @Autowired
    private lateinit var userHealthService: UserHealthService

    // Ask Spring to inject the real StatisticsService bean.
    @Autowired
    private lateinit var statisticsService: StatisticsService

    // PasswordEncoder is used only to verify that stored BCrypt hashes
    // match the original raw passwords.
    //
    // UserService and AuthenticationService are responsible for performing
    // the actual BCrypt encoding before persistence.

    // Ask Spring to inject the configured PasswordEncoder bean.
    @Autowired
    private lateinit var passwordEncoder: PasswordEncoder

    // Raw MongoDatabase access is used only by integration tests that must
    // inspect MongoDB-specific state such as ObjectId relationships,
    // transactionVersion and related collection documents.

    // Ask Spring to inject the configured MongoDatabase bean.
    @Autowired
    private lateinit var mongoDatabase: MongoDatabase

    // Username created for the first shared integration-test user.
    private lateinit var testUserName1: String

    // Username created for the second shared integration-test user.
    private lateinit var testUserName2: String

    // Track all users created by this test class so cleanup can still run
    // even if an individual test fails before deleting its temporary user.

    // Use a synchronized set because some tests create users concurrently.
    private val createdUsernames: MutableSet<String> =
        Collections.synchronizedSet(HashSet())

    // Hold the first shared baseline user.
    private lateinit var testUser1: User

    // Hold the second shared baseline user.
    private lateinit var testUser2: User

    // ---------------------------------------------------------------------
    // TEST LIFECYCLE
    // ---------------------------------------------------------------------

    // ---------------------------------------------------------------------
    // Creates a user directly through UserService.
    //
    // IMPORTANT:
    // UserService.createUser() performs BCrypt encoding itself.
    //
    // Therefore this helper must receive a User containing the RAW password.
    // The test must not pre-encode the password, otherwise the password would
    // be BCrypt-encoded twice.
    // ---------------------------------------------------------------------
    private fun createUserOrFail(user: User) {

        // Start the asynchronous user creation operation.
        val future = userService.createUser(user)

        // Wait up to 20 seconds for the creation result.
        val created = future.get(20, TimeUnit.SECONDS)

        // Fail the test immediately when the user could not be created.
        assertTrue(
            created,
            "Failed to create test user: ${user.userName}"
        )

        // Add the username to the cleanup set.
        createdUsernames.add(user.userName!!)
    }

    // ---------------------------------------------------------------------
    // Returns the MongoDB users collection.
    // ---------------------------------------------------------------------
    private fun usersCollection(): MongoCollection<Document> {

        // Return direct access to the users collection.
        return mongoDatabase.getCollection("users")
    }

    // ---------------------------------------------------------------------
    // Returns the MongoDB water_records collection.
    // ---------------------------------------------------------------------
    private fun waterRecordsCollection(): MongoCollection<Document> {

        // Return direct access to the water_records collection.
        return mongoDatabase.getCollection("water_records")
    }

    // ---------------------------------------------------------------------
    // Returns the MongoDB calories collection.
    // ---------------------------------------------------------------------
    private fun caloriesCollection(): MongoCollection<Document> {

        // Return direct access to the calories collection.
        return mongoDatabase.getCollection("calories")
    }

    // ---------------------------------------------------------------------
    // Returns the MongoDB goals collection.
    // ---------------------------------------------------------------------
    private fun goalsCollection(): MongoCollection<Document> {

        // Return direct access to the goals collection.
        return mongoDatabase.getCollection("goals")
    }

    // ---------------------------------------------------------------------
    // Resolves a username into its MongoDB ObjectId.
    //
    // Returns null when no matching user exists.
    // ---------------------------------------------------------------------
    private fun getUserIdFromMongo(username: String): ObjectId? {

        // Find the user document whose username matches the supplied value.
        val document = usersCollection().find(
            eq("username", username)
        ).first()

        // Return null when the user does not exist.
        if (document == null) {
            return null
        }

        // Return MongoDB's generated ObjectId.
        return document.getObjectId("_id")
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
    private fun getTransactionVersion(username: String): Long {

        // Find the requested user document.
        val document = usersCollection().find(
            eq("username", username)
        ).first()

        // Return -1 when the user does not exist.
        if (document == null) {
            return -1
        }

        // Read transactionVersion as Number to support BSON numeric types.
        val version = document.get("transactionVersion", Number::class.java)

        // Return zero when the field is missing, otherwise return its Long value.
        return version?.toLong() ?: 0
    }

    // Run once before the test methods in this class.
    @BeforeAll
    fun setUpTestUsers() {

        // Generate one unique suffix for this test execution.
        val runId = System.currentTimeMillis().toString()

        // Build the first unique shared username.
        testUserName1 = "integrationUser1_$runId"

        // Build the second unique shared username.
        testUserName2 = "integrationUser2_$runId"

        // Create the first baseline User object.
        testUser1 = User()

        // Assign the first baseline username.
        testUser1.userName = testUserName1

        // Assign the first user's raw password.
        testUser1.password = "pass1"

        // Create the second baseline User object.
        testUser2 = User()

        // Assign the second baseline username.
        testUser2.userName = testUserName2

        // Assign the second user's raw password.
        testUser2.password = "pass2"

        // Persist the first baseline user.
        createUserOrFail(testUser1)

        // Persist the second baseline user.
        createUserOrFail(testUser2)
    }

    // Run once after all tests in this class finish.
    @AfterAll
    fun cleanUpTestUsers() {

        // Iterate over a snapshot so cleanup is safe while using a synchronized set.
        for (username in ArrayList(createdUsernames)) {

            try {
                // Attempt to delete every user created by the test suite.
                userService.deleteUser(username).get(20, TimeUnit.SECONDS)

            } catch (e: Exception) {
                // Print a warning instead of stopping the remaining cleanup.
                println(
                    "WARN cleanup failed for username=$username message=${e.message}"
                )
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
    fun signup_createsNewUserAndRejectsDuplicate() {

        // Create a unique username for this signup test.
        val uniqueUsername =
            "signupUser_${System.currentTimeMillis()}"

        // Define the raw password sent during signup.
        val rawPassword = "signupPass"

        // Create the signup User object.
        val signupUser = User()

        // Assign the unique username.
        signupUser.userName = uniqueUsername

        // Assign the raw password.
        signupUser.password = rawPassword

        // Assign a full name.
        signupUser.fullName = "Sasa li"

        // Assign an age.
        signupUser.age = 25

        // Execute the first signup request and wait for its result.
        val firstResult = authenticationService
            .signup(signupUser)
            .get(20, TimeUnit.SECONDS)

        // Verify that the first signup succeeds.
        assertEquals("User created successfully", firstResult)

        // Register the created user for cleanup.
        createdUsernames.add(uniqueUsername)

        // Read the persisted user through UserService.
        val storedUser = userService
            .getUser(uniqueUsername)
            .get(20, TimeUnit.SECONDS)

        // Verify that the user exists.
        assertNotNull(storedUser)

        // Verify that the raw password was not stored directly.
        assertNotEquals(
            rawPassword,
            storedUser!!.password
        )

        // Verify that the stored BCrypt hash matches the original password.
        assertTrue(
            passwordEncoder.matches(
                rawPassword,
                storedUser.password
            )
        )

        // Attempt to sign up with the same username again.
        val secondResult = authenticationService
            .signup(signupUser)
            .get(20, TimeUnit.SECONDS)

        // Verify that the duplicate signup is rejected.
        assertEquals("Username already exists", secondResult)
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
    fun signup_concurrentDuplicateRequests_onlyOneUserIsCreated() {

        // Create one username that both concurrent requests will use.
        val username =
            "concurrentSignup_${System.currentTimeMillis()}"

        // Create the first competing user object.
        val userA = User()

        // Give the first user the shared username.
        userA.userName = username

        // Give the first user its raw password.
        userA.password = "passA"

        // Create the second competing user object.
        val userB = User()

        // Give the second user the same username.
        userB.userName = username

        // Give the second user a different raw password.
        userB.password = "passB"

        // Create a latch so both signup tasks begin together.
        val start = CountDownLatch(1)

        // Create the first asynchronous signup attempt.
        val requestA = CompletableFuture.supplyAsync<String> {

            try {
                // Wait until both concurrent tasks are ready.
                start.await()

                // Execute the first signup request.
                authenticationService
                    .signup(userA)
                    .get(20, TimeUnit.SECONDS)

            } catch (e: Exception) {
                // Convert checked failures into a runtime failure for the future.
                throw RuntimeException(e)
            }
        }

        // Create the second asynchronous signup attempt.
        val requestB = CompletableFuture.supplyAsync<String> {

            try {
                // Wait until both concurrent tasks are ready.
                start.await()

                // Execute the second signup request.
                authenticationService
                    .signup(userB)
                    .get(20, TimeUnit.SECONDS)

            } catch (e: Exception) {
                // Convert checked failures into a runtime failure for the future.
                throw RuntimeException(e)
            }
        }

        // Release both signup tasks at approximately the same time.
        start.countDown()

        // Wait for the first signup result.
        val resultA = requestA.get(20, TimeUnit.SECONDS)

        // Wait for the second signup result.
        val resultB = requestB.get(20, TimeUnit.SECONDS)

        // Count how many requests succeeded.
        var successCount = 0

        // Count how many requests were rejected as duplicates.
        var duplicateCount = 0

        // Count the first result as successful when appropriate.
        if (resultA == "User created successfully") {
            successCount++
        }

        // Count the second result as successful when appropriate.
        if (resultB == "User created successfully") {
            successCount++
        }

        // Count the first result as a duplicate when appropriate.
        if (resultA == "Username already exists") {
            duplicateCount++
        }

        // Count the second result as a duplicate when appropriate.
        if (resultB == "Username already exists") {
            duplicateCount++
        }

        // Verify that exactly one request succeeded.
        assertEquals(1, successCount)

        // Verify that exactly one request was rejected as a duplicate.
        assertEquals(1, duplicateCount)

        // Verify that MongoDB contains exactly one matching user document.
        assertEquals(
            1L,
            usersCollection().countDocuments(
                eq("username", username)
            )
        )

        // Register the surviving user for cleanup.
        createdUsernames.add(username)
    }

    // ---------------------------------------------------------------------
    // Verifies direct user creation, existence checking and deletion.
    //
    // UserService receives the RAW password and performs BCrypt encoding.
    // ---------------------------------------------------------------------
    @Test
    fun createUser_existsAndDeleteUser_flowWorks() {
        // Create a unique username for this flow test.
        val tempUsername = "tempUser_${System.currentTimeMillis()}"

        // Define the raw password.
        val rawPassword = "tempPass"

        // Create the temporary user.
        val tempUser = User()

        // Assign the username.
        tempUser.userName = tempUsername

        // Assign the raw password.
        tempUser.password = rawPassword

        // Assign a full name.
        tempUser.fullName = "Sasa li"

        // Assign an age.
        tempUser.age = 25

        // Create the user through UserService.
        val created = userService
            .createUser(tempUser)
            .get(20, TimeUnit.SECONDS)

        // Verify that creation succeeded.
        assertTrue(created)

        // Register the temporary user for cleanup.
        createdUsernames.add(tempUsername)

        // Read the stored user.
        val storedUser = userService
            .getUser(tempUsername)
            .get(20, TimeUnit.SECONDS)

        // Verify that the stored user exists.
        assertNotNull(storedUser)

        // Verify that the stored password differs from the raw password.
        assertNotEquals(
            rawPassword,
            storedUser!!.password
        )

        // Verify that the stored BCrypt hash matches the original raw password.
        assertTrue(
            passwordEncoder.matches(
                rawPassword,
                storedUser.password
            )
        )

        // Verify that exists() reports the user as present.
        assertTrue(
            userService
                .exists(tempUsername)
                .get(20, TimeUnit.SECONDS)
        )

        // Delete the user.
        assertTrue(
            userService
                .deleteUser(tempUsername)
                .get(20, TimeUnit.SECONDS)
        )

        // Verify that exists() reports the user as absent after deletion.
        assertFalse(
            userService
                .exists(tempUsername)
                .get(20, TimeUnit.SECONDS)
        )
    }

    // ---------------------------------------------------------------------
    // Verifies that getUser returns null for a username that does not exist.
    // ---------------------------------------------------------------------
    @Test
    fun getUser_nonExisting_returnsNull() {
        // Build a username that should not exist.
        val missingUsername =
            "getUserNoSuch_${System.currentTimeMillis()}"

        // Attempt to read the missing user.
        val result = userService.getUser(missingUsername)
            .get(20, TimeUnit.SECONDS)

        // Verify that the service returns null.
        assertNull(result)
    }

    // ---------------------------------------------------------------------
    // LOGIN TESTS
    // ---------------------------------------------------------------------

    // ---------------------------------------------------------------------
    // Verifies successful login using a raw password against the stored
    // BCrypt hash.
    // ---------------------------------------------------------------------
    @Test
    fun login_withCorrectCredentials_returnsUser() {
        // Attempt to log in with the correct credentials.
        val loggedUser = authenticationService
            .login(testUserName1, "pass1")
            .get(20, TimeUnit.SECONDS)

        // Verify that authentication succeeded.
        assertNotNull(loggedUser)

        // Verify that the returned user has the expected username.
        assertEquals(
            testUserName1,
            loggedUser!!.userName
        )

        // Verify that the returned password is not the original raw password.
        assertNotEquals(
            "pass1",
            loggedUser.password
        )

        // Verify that the stored hash still matches the original password.
        assertTrue(
            passwordEncoder.matches(
                "pass1",
                loggedUser.password
            )
        )
    }

    // ---------------------------------------------------------------------
    // Verifies that login fails when the raw password is incorrect.
    // ---------------------------------------------------------------------
    @Test
    fun login_withWrongPassword_returnsNull() {
        // Attempt to log in using the wrong password.
        val loggedUser = authenticationService
            .login(testUserName1, "wrongPass")
            .get(20, TimeUnit.SECONDS)

        // Verify that authentication fails with null.
        assertNull(loggedUser)
    }

    // ---------------------------------------------------------------------
    // WATER MODULE TESTS
    // ---------------------------------------------------------------------

    // ---------------------------------------------------------------------
    // Verifies that updateWater inserts a new water record and that both
    // getWater() and getWaterHistoryMap() reflect the new amount.
    // ---------------------------------------------------------------------
    @Test
    fun updateWater_increasesTodayTotal_and_getWaterIsConsistent() {
        // Read the user's current water totals before the update.
        val beforeJson = waterService
            .getWater(testUserName1)
            .get(20, TimeUnit.SECONDS)

        // Verify that the user and water response exist.
        assertNotNull(beforeJson)

        // Read today's total before adding another drink.
        val todayBefore = beforeJson!!.getLong("todayWater")

        // Define the drink amount that will be added.
        val addedAmount = 500

        // Add one new water record.
        val updated = waterService
            .updateWater(testUserName1, addedAmount)
            .get(20, TimeUnit.SECONDS)

        // Verify that the water update succeeded.
        assertTrue(updated)

        // Read the water totals after the update.
        val afterJson = waterService
            .getWater(testUserName1)
            .get(20, TimeUnit.SECONDS)

        // Verify that a response is still returned.
        assertNotNull(afterJson)

        // Read today's new total.
        val todayAfter = afterJson!!.getLong("todayWater")

        // Verify that today's total increased by exactly the inserted amount.
        assertEquals(
            todayBefore + addedAmount,
            todayAfter
        )

        // Build today's yyyy-MM-dd history key.
        val todayKey = LocalDate.now().toString()

        // Read the last three days of water history.
        val history = waterService
            .getWaterHistoryMap(testUserName1, 3)
            .get(20, TimeUnit.SECONDS)

        // Verify that history was returned.
        assertNotNull(history)

        // Verify that exactly three dates are present.
        assertEquals(3, history!!.size)

        // Verify that today's date exists in the history map.
        assertTrue(history.containsKey(todayKey))

        // Verify that today's history total matches getWater().
        assertEquals(todayAfter, history[todayKey])
    }

    // ---------------------------------------------------------------------
    // Verifies that a fresh user has zero water totals for all requested
    // history days.
    // ---------------------------------------------------------------------
    @Test
    fun getWaterHistoryMap_forNewUser_returnsAllZerosWithExpectedKeys() {

        // Request seven days of history.
        val days = 7

        // Create the expected ordered result map.
        val expected = LinkedHashMap<String, Long>()

        // Read today's calendar date.
        val today = LocalDate.now()

        // Build the expected seven date keys with zero totals.
        for (i in 0 until days) {

            // Insert the expected date and zero value.
            expected[
                today.minusDays(i.toLong()).toString()
            ] = 0L
        }

        // Request the actual seven-day history.
        val actual = waterService
            .getWaterHistoryMap(testUserName2, days)
            .get(20, TimeUnit.SECONDS)

        // Verify that a history map was returned.
        assertNotNull(actual)

        // Verify that all expected dates and values match.
        assertEquals(expected, actual)
    }

    // ---------------------------------------------------------------------
    // Verifies that a fresh user with no water records returns zero for
    // both today and yesterday.
    // ---------------------------------------------------------------------
    @Test
    fun getWater_forNewUser_returnsZeroTotals() {

        // Read the water totals for the fresh baseline user.
        val json = waterService
            .getWater(testUserName2)
            .get(20, TimeUnit.SECONDS)

        // Verify that a response was returned.
        assertNotNull(json)

        // Verify that today's total is zero.
        assertEquals(0L, json!!.getLong("todayWater"))

        // Verify that yesterday's total is also zero.
        assertEquals(0L, json.getLong("yesterdayWater"))
    }

    // ---------------------------------------------------------------------
    // Verifies the MongoDB date-range behavior used by getWater().
    //
    // The test inserts one record for today and one for yesterday directly
    // into MongoDB, then verifies that the service places each amount into
    // the correct day.
    // ---------------------------------------------------------------------
    @Test
    fun getWater_withTodayAndYesterdayMongoRecords_returnsCorrectTotals() {

        // Create a unique username for the date-range test.
        val username =
            "waterDateTest_${System.currentTimeMillis()}"

        // Create the test user.
        val user = User()

        // Assign the username.
        user.userName = username

        // Assign the raw password.
        user.password = "waterDatePass"

        // Persist the test user.
        createUserOrFail(user)

        // Resolve the new user's MongoDB ObjectId.
        val userId = getUserIdFromMongo(username)

        // Verify that the user has a MongoDB ObjectId.
        assertNotNull(userId)

        // Read today's calendar date.
        val today = LocalDate.now()

        // Calculate yesterday's calendar date.
        val yesterday = today.minusDays(1)

        // Create a timestamp at noon today.
        val todayTime = Date.from(
            today
                .atTime(12, 0)
                .atZone(ZoneId.systemDefault())
                .toInstant()
        )

        // Create a timestamp at noon yesterday.
        val yesterdayTime = Date.from(
            yesterday
                .atTime(12, 0)
                .atZone(ZoneId.systemDefault())
                .toInstant()
        )

        // Insert one water document for today.
        waterRecordsCollection().insertOne(
            Document("userId", userId!!)
                .append("amountMl", 700)
                .append("recordedAt", todayTime)
        )

        // Insert one water document for yesterday.
        waterRecordsCollection().insertOne(
            Document("userId", userId)
                .append("amountMl", 400)
                .append("recordedAt", yesterdayTime)
        )

        // Read the calculated today/yesterday totals.
        val result = waterService
            .getWater(username)
            .get(20, TimeUnit.SECONDS)

        // Verify that the service returned a result.
        assertNotNull(result)

        // Verify today's total.
        assertEquals(700L, result!!.getLong("todayWater"))

        // Verify yesterday's total.
        assertEquals(400L, result.getLong("yesterdayWater"))
    }

    // ---------------------------------------------------------------------
    // GOAL MODULE TESTS
    // ---------------------------------------------------------------------

    // ---------------------------------------------------------------------
    // Verifies that updateGoalMl changes today's goal and that getGoalMl
    // reads the same value.
    // ---------------------------------------------------------------------
    @Test
    fun updateGoalMl_changesGoal_and_getGoalMlReadsIt() {

        // Define the new valid daily water goal.
        val newGoal = 3200

        // Update the first baseline user's goal.
        val updated = waterService
            .updateGoalMl(testUserName1, newGoal)
            .get(20, TimeUnit.SECONDS)

        // Verify that the update succeeded.
        assertTrue(updated)

        // Read the stored goal back from the service.
        val goalValue = waterService
            .getGoalMl(testUserName1)
            .get(20, TimeUnit.SECONDS)

        // Verify that the stored goal matches the requested value.
        assertEquals(newGoal, goalValue)
    }

    // ---------------------------------------------------------------------
    // Verifies that invalid goal values are rejected and do not change the
    // user's stored goal.
    // ---------------------------------------------------------------------
    @Test
    fun updateGoalMl_outOfRange_isRejectedAndValueNotChanged() {

        // Create a unique username for the invalid-goal test.
        val username =
            "goalInvalidDeep_${System.currentTimeMillis()}"

        // Create the test user.
        val user = User()

        // Assign the username.
        user.userName = username

        // Assign a raw password.
        user.password = "p"

        // Persist the user.
        createUserOrFail(user)

        // Read the original goal value.
        val before = waterService
            .getGoalMl(username)
            .get(20, TimeUnit.SECONDS)

        // Verify that a new user starts with the default goal.
        assertEquals(3000, before)

        // Attempt to store a value below the allowed range.
        val low = waterService
            .updateGoalMl(username, 100)
            .get(20, TimeUnit.SECONDS)

        // Attempt to store a value above the allowed range.
        val high = waterService
            .updateGoalMl(username, 20000)
            .get(20, TimeUnit.SECONDS)

        // Verify that the low invalid value was rejected.
        assertFalse(low)

        // Verify that the high invalid value was rejected.
        assertFalse(high)

        // Read the goal again after both invalid attempts.
        val after = waterService
            .getGoalMl(username)
            .get(20, TimeUnit.SECONDS)

        // Verify that the goal was not changed.
        assertEquals(before, after)
    }

    // ---------------------------------------------------------------------
    // CALORIES MODULE TESTS
    // ---------------------------------------------------------------------

    // ---------------------------------------------------------------------
    // Verifies that updateCalories stores today's value and getCalories
    // returns the same value.
    // ---------------------------------------------------------------------
    @Test
    fun updateCalories_setsValue_and_getCaloriesReadsIt() {

        // Define the calories value to store.
        val newCalories = 1234

        // Update today's calories value.
        val updated = userHealthService
            .updateCalories(testUserName1, newCalories)
            .get(20, TimeUnit.SECONDS)

        // Verify that the update succeeded.
        assertTrue(updated)

        // Read today's calories value back.
        val calories = userHealthService
            .getCalories(testUserName1)
            .get(20, TimeUnit.SECONDS)

        // Verify that the stored value matches the requested value.
        assertEquals(newCalories, calories)
    }

    // ---------------------------------------------------------------------
    // Verifies that a fresh user has zero calories for today.
    // ---------------------------------------------------------------------
    @Test
    fun getCalories_forNewUser_returnsZero() {

        // Read today's calories for the fresh baseline user.
        val calories = userHealthService
            .getCalories(testUserName2)
            .get(20, TimeUnit.SECONDS)

        // Verify that the default value is zero.
        assertEquals(0, calories)
    }

    // ---------------------------------------------------------------------
    // Verifies that getCalories returns zero for a missing user.
    // ---------------------------------------------------------------------
    @Test
    fun getCalories_userNotFound_returnsZero() {

        // Build a username that should not exist.
        val missingUsername =
            "noSuchUser_${System.currentTimeMillis()}"

        // Ask the service for the missing user's calories.
        val calories = userHealthService
            .getCalories(missingUsername)
            .get(20, TimeUnit.SECONDS)

        // Verify that the service preserves the zero fallback.
        assertEquals(0, calories)
    }

    // ---------------------------------------------------------------------
    // Verifies valid and invalid calorie updates.
    //
    // Invalid values must not overwrite the last valid value.
    // ---------------------------------------------------------------------
    @Test
    fun updateCalories_validAndInvalidValues_behaveAsExpected() {

        // Create a unique username for the calorie validation test.
        val username =
            "calDeep_${System.currentTimeMillis()}"

        // Create the test user.
        val user = User()

        // Assign the username.
        user.userName = username

        // Assign a raw password.
        user.password = "p"

        // Persist the test user.
        createUserOrFail(user)

        // Read the initial calories value.
        val initial = userHealthService
            .getCalories(username)
            .get(20, TimeUnit.SECONDS)

        // Verify that the initial value is zero.
        assertEquals(0, initial)

        // Store one valid calories value.
        val validUpdated = userHealthService
            .updateCalories(username, 1200)
            .get(20, TimeUnit.SECONDS)

        // Verify that the valid update succeeded.
        assertTrue(validUpdated)

        // Read the value after the valid update.
        val afterValid = userHealthService
            .getCalories(username)
            .get(20, TimeUnit.SECONDS)

        // Verify that 1200 was stored.
        assertEquals(1200, afterValid)

        // Attempt to store a negative calories value.
        val invalidLow = userHealthService
            .updateCalories(username, -5)
            .get(20, TimeUnit.SECONDS)

        // Attempt to store a calories value above the allowed maximum.
        val invalidHigh = userHealthService
            .updateCalories(username, 50000)
            .get(20, TimeUnit.SECONDS)

        // Verify that the negative value was rejected.
        assertFalse(invalidLow)

        // Verify that the value above the maximum was rejected.
        assertFalse(invalidHigh)

        // Read the calories value after the invalid updates.
        val afterInvalid = userHealthService
            .getCalories(username)
            .get(20, TimeUnit.SECONDS)

        // Verify that the previous valid value was preserved.
        assertEquals(1200, afterInvalid)
    }

    // ---------------------------------------------------------------------
    // BMI DISTRIBUTION TEST
    // ---------------------------------------------------------------------

    // ---------------------------------------------------------------------
    // Verifies that the global BMI distribution correctly counts one new
    // user in each BMI category.
    // ---------------------------------------------------------------------
    @Test
    fun getBmiDistribution_countsEachBmiCategoryForNewUsers() {

        // Read the current BMI distribution before creating new users.
        val before = statisticsService
            .getBmiDistribution()
            .get(20, TimeUnit.SECONDS)

        // Read the original Underweight count.
        val underBefore = before.getOrDefault("Underweight", 0)

        // Read the original Normal count.
        val normalBefore = before.getOrDefault("Normal", 0)

        // Read the original Overweight count.
        val overBefore = before.getOrDefault("Overweight", 0)

        // Read the original Obese count.
        val obeseBefore = before.getOrDefault("Obese", 0)

        // Create one shared unique prefix for all four test users.
        val prefix =
            "bmiTestUser_${System.currentTimeMillis()}"

        // Build four unique usernames.
        val bmiUsers = arrayOf(
            "${prefix}_u",
            "${prefix}_n",
            "${prefix}_o",
            "${prefix}_ob"
        )

        // Define one BMI value for each BMI category.
        val bmiValues = doubleArrayOf(
            17.0,
            22.0,
            27.0,
            32.0
        )

        // Create one user for every BMI category.
        for (i in bmiUsers.indices) {

            // Create the current test user.
            val user = User()

            // Assign the current username.
            user.userName = bmiUsers[i]

            // Assign a raw password.
            user.password = "bmiPass"

            // Persist the current user.
            createUserOrFail(user)

            // Update the current user's BMI.
            val bmiUpdated = userHealthService
                .updateBmi(
                    bmiUsers[i],
                    bmiValues[i]
                )
                .get(20, TimeUnit.SECONDS)

            // Verify that the BMI update succeeded.
            assertTrue(bmiUpdated)
        }

        // Read the BMI distribution after creating the four users.
        val after = statisticsService
            .getBmiDistribution()
            .get(20, TimeUnit.SECONDS)

        // Read the new Underweight count.
        val underAfter = after.getOrDefault("Underweight", 0)

        // Read the new Normal count.
        val normalAfter = after.getOrDefault("Normal", 0)

        // Read the new Overweight count.
        val overAfter = after.getOrDefault("Overweight", 0)

        // Read the new Obese count.
        val obeseAfter = after.getOrDefault("Obese", 0)

        // Verify that exactly one Underweight user was added.
        assertEquals(underBefore + 1, underAfter)

        // Verify that exactly one Normal user was added.
        assertEquals(normalBefore + 1, normalAfter)

        // Verify that exactly one Overweight user was added.
        assertEquals(overBefore + 1, overAfter)

        // Verify that exactly one Obese user was added.
        assertEquals(obeseBefore + 1, obeseAfter)
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
    fun userRelatedWriteTransactions_incrementTransactionVersion() {

        // Create a unique username for the transactionVersion test.
        val username =
            "transactionVersion_${System.currentTimeMillis()}"

        // Create the test user.
        val user = User()

        // Assign the username.
        user.userName = username

        // Assign a raw password.
        user.password = "transactionPass"

        // Persist the test user.
        createUserOrFail(user)

        // Read the initial transactionVersion value.
        val initialVersion = getTransactionVersion(username)

        // Verify that the user exists and has a readable transactionVersion.
        assertTrue(initialVersion >= 0)

        // Perform one transactional water update.
        assertTrue(
            waterService
                .updateWater(username, 250)
                .get(20, TimeUnit.SECONDS)
        )

        // Read transactionVersion after the water update.
        val afterWater = getTransactionVersion(username)

        // Verify that updateWater incremented the version exactly once.
        assertEquals(initialVersion + 1, afterWater)

        // Perform one transactional calorie update.
        assertTrue(
            userHealthService
                .updateCalories(username, 1800)
                .get(20, TimeUnit.SECONDS)
        )

        // Read transactionVersion after the calories update.
        val afterCalories = getTransactionVersion(username)

        // Verify that updateCalories incremented the version exactly once.
        assertEquals(afterWater + 1, afterCalories)

        // Perform one transactional goal update.
        assertTrue(
            waterService
                .updateGoalMl(username, 3000)
                .get(20, TimeUnit.SECONDS)
        )

        // Read transactionVersion after the goal update.
        val afterGoal = getTransactionVersion(username)

        // Verify that updateGoalMl incremented the version exactly once.
        assertEquals(afterCalories + 1, afterGoal)
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
    fun deleteUser_removesRelatedMongoDocuments() {

        // Create a unique username for the delete transaction test.
        val username =
            "deleteTransaction_${System.currentTimeMillis()}"

        // Create the test user.
        val user = User()

        // Assign the username.
        user.userName = username

        // Assign a raw password.
        user.password = "deletePass"

        // Persist the test user.
        createUserOrFail(user)

        // Resolve the user's MongoDB ObjectId.
        val userId = getUserIdFromMongo(username)

        // Verify that the user exists in MongoDB.
        assertNotNull(userId)

        // Create one related water document.
        assertTrue(
            waterService
                .updateWater(username, 500)
                .get(20, TimeUnit.SECONDS)
        )

        // Create today's related calories document.
        assertTrue(
            userHealthService
                .updateCalories(username, 2200)
                .get(20, TimeUnit.SECONDS)
        )

        // Create today's related goal document.
        assertTrue(
            waterService
                .updateGoalMl(username, 3300)
                .get(20, TimeUnit.SECONDS)
        )

        // Verify that at least one related water document exists.
        assertTrue(
            waterRecordsCollection().countDocuments(
                eq("userId", userId!!)
            ) > 0
        )

        // Verify that at least one related calories document exists.
        assertTrue(
            caloriesCollection().countDocuments(
                eq("userId", userId)
            ) > 0
        )

        // Verify that at least one related goal document exists.
        assertTrue(
            goalsCollection().countDocuments(
                eq("userId", userId)
            ) > 0
        )

        // Delete the user through the normal service path.
        val deleted = userService
            .deleteUser(username)
            .get(20, TimeUnit.SECONDS)

        // Verify that the delete operation succeeded.
        assertTrue(deleted)

        // Verify that the main user document was deleted.
        assertNull(
            usersCollection().find(
                eq("_id", userId)
            ).first()
        )

        // Verify that no related water documents remain.
        assertEquals(
            0L,
            waterRecordsCollection().countDocuments(
                eq("userId", userId)
            )
        )

        // Verify that no related calories documents remain.
        assertEquals(
            0L,
            caloriesCollection().countDocuments(
                eq("userId", userId)
            )
        )

        // Verify that no related goal documents remain.
        assertEquals(
            0L,
            goalsCollection().countDocuments(
                eq("userId", userId)
            )
        )
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
    fun concurrentDeleteAndWaterUpdate_neverLeaveOrphanWaterRecord() {

        // Create a unique username for this concurrency test.
        val username =
            "deleteWaterRace_${System.currentTimeMillis()}"

        // Create the test user.
        val user = User()

        // Assign the username.
        user.userName = username

        // Assign a raw password.
        user.password = "racePass"

        // Persist the test user.
        createUserOrFail(user)

        // Resolve the user's MongoDB ObjectId before the race starts.
        val userId = getUserIdFromMongo(username)

        // Verify that the ObjectId exists.
        assertNotNull(userId)

        // Create a latch so both operations begin together.
        val start = CountDownLatch(1)

        // Create the concurrent delete attempt.
        val deleteAttempt = CompletableFuture.supplyAsync<Any> {

            try {
                // Wait for the shared start signal.
                start.await()

                // Attempt to delete the user.
                userService
                    .deleteUser(username)
                    .get(20, TimeUnit.SECONDS)

            } catch (e: Exception) {
                // Preserve the exception as the future result.
                e
            }
        }

        // Create the concurrent water update attempt.
        val waterAttempt = CompletableFuture.supplyAsync<Any> {

            try {
                // Wait for the shared start signal.
                start.await()

                // Attempt to insert a water record.
                waterService
                    .updateWater(username, 650)
                    .get(20, TimeUnit.SECONDS)

            } catch (e: Exception) {
                // Preserve the exception as the future result.
                e
            }
        }

        // Release both concurrent operations.
        start.countDown()

        // Wait for the delete attempt to finish.
        deleteAttempt.get(20, TimeUnit.SECONDS)

        // Wait for the water update attempt to finish.
        waterAttempt.get(20, TimeUnit.SECONDS)

        // Read the user document after both operations finish.
        val userDocument = usersCollection().find(
            eq("_id", userId!!)
        ).first()

        // Count water records that still reference the original userId.
        val waterCount = waterRecordsCollection().countDocuments(
            eq("userId", userId)
        )

        // If delete won and the user no longer exists, the transaction
        // protection must ensure that no orphan water record remains.
        if (userDocument == null) {

            // Verify that no orphan water record exists.
            assertEquals(0L, waterCount)
        }

        // If the write transaction won and delete failed because of a
        // transaction conflict, the user may still exist. That state is
        // consistent because the related water document still references
        // a valid user.
        else {

            // Verify that the surviving document belongs to the expected user.
            assertEquals(
                username,
                userDocument.getString("username")
            )
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
    fun concurrentDeleteCaloriesAndGoalUpdates_neverLeaveOrphanDocuments() {

        // Create a unique username for the health-data concurrency test.
        val username =
            "deleteHealthRace_${System.currentTimeMillis()}"

        // Create the test user.
        val user = User()

        // Assign the username.
        user.userName = username

        // Assign a raw password.
        user.password = "racePass"

        // Persist the test user.
        createUserOrFail(user)

        // Resolve the user's MongoDB ObjectId before the race begins.
        val userId = getUserIdFromMongo(username)

        // Verify that the user exists in MongoDB.
        assertNotNull(userId)

        // Create a latch so all three operations start together.
        val start = CountDownLatch(1)

        // Create the concurrent delete attempt.
        val deleteAttempt = CompletableFuture.supplyAsync<Any> {

            try {
                // Wait for the shared start signal.
                start.await()

                // Attempt to delete the user.
                userService
                    .deleteUser(username)
                    .get(20, TimeUnit.SECONDS)

            } catch (e: Exception) {
                // Preserve the exception as the future result.
                e
            }
        }

        // Create the concurrent calories update attempt.
        val caloriesAttempt = CompletableFuture.supplyAsync<Any> {

            try {
                // Wait for the shared start signal.
                start.await()

                // Attempt to update today's calories.
                userHealthService
                    .updateCalories(username, 2100)
                    .get(20, TimeUnit.SECONDS)

            } catch (e: Exception) {
                // Preserve the exception as the future result.
                e
            }
        }

        // Create the concurrent goal update attempt.
        val goalAttempt = CompletableFuture.supplyAsync<Any> {

            try {
                // Wait for the shared start signal.
                start.await()

                // Attempt to update today's water goal.
                waterService
                    .updateGoalMl(username, 3400)
                    .get(20, TimeUnit.SECONDS)

            } catch (e: Exception) {
                // Preserve the exception as the future result.
                e
            }
        }

        // Release all three concurrent operations.
        start.countDown()

        // Wait for the delete attempt to finish.
        deleteAttempt.get(20, TimeUnit.SECONDS)

        // Wait for the calories update attempt to finish.
        caloriesAttempt.get(20, TimeUnit.SECONDS)

        // Wait for the goal update attempt to finish.
        goalAttempt.get(20, TimeUnit.SECONDS)

        // Read the user document after all operations finish.
        val userDocument = usersCollection().find(
            eq("_id", userId!!)
        ).first()

        // Count calories documents that still reference the original userId.
        val caloriesCount = caloriesCollection().countDocuments(
            eq("userId", userId)
        )

        // Count goal documents that still reference the original userId.
        val goalsCount = goalsCollection().countDocuments(
            eq("userId", userId)
        )

        // A deleted user must never have related orphan documents.
        if (userDocument == null) {

            // Verify that no orphan calories document remains.
            assertEquals(0L, caloriesCount)

            // Verify that no orphan goal document remains.
            assertEquals(0L, goalsCount)
        }

        // If delete lost the transaction conflict, the user is still valid.
        else {

            // Verify that the surviving user document is the expected one.
            assertEquals(
                username,
                userDocument.getString("username")
            )
        }
    }
}