// Define the package for this test class
package CapstoneTests;
import org.example.CapstoneProject.Application;
// Import assertion methods from JUnit Jupiter
import org.example.CapstoneProject.service.AuthenticationService;
import org.example.CapstoneProject.service.StatisticsService;
import org.example.CapstoneProject.service.UserHealthService;
import org.example.CapstoneProject.service.UserService;
import org.example.CapstoneProject.service.WaterService;
import org.junit.jupiter.api.AfterAll;
// Import annotation to define methods that run before all tests
import org.junit.jupiter.api.BeforeAll;
// Import annotation for standard test methods
import org.junit.jupiter.api.Test;
// Import annotation to control test instance lifecycle (per class instead of per method)
import org.junit.jupiter.api.TestInstance;
// Import the TestInstance lifecycle enum
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
// Import the SpringBootTest annotation to load the full Spring context
import org.springframework.boot.test.context.SpringBootTest;
// Import Autowired to inject Spring beans into the test class
import org.springframework.beans.factory.annotation.Autowired;
// Import static assertion methods for cleaner code
import static org.junit.jupiter.api.Assertions.*;
// Import the User model used by the service layer
import org.example.CapstoneProject.model.User;
// Import JSONObject used by getWater method
import org.json.JSONObject;
// Import standard Java concurrency utilities
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
// Import date and time utilities for building expected date keys
import java.text.SimpleDateFormat;
import java.util.*;
import org.springframework.security.crypto.password.PasswordEncoder;
// CapstoneServicesIntegrationTest is an end-to-end integration test class that
// verifies the behavior of the refactored service layer against a real Firebase
// Realtime Database. Instead of calling the REST controllers, these tests
// interact directly with the domain services to verify user, authentication,
// water, health and statistics operations. The services delegate database
// access to the repository layer, which uses the real Firebase implementation.
// By running these tests we can detect issues related to data structure, paths,
// serialization, asynchronous operations and integration between the service
// and repository layers before the HTTP layer is involved.

// Annotate this class as a Spring Boot integration test (loads the full application context)
@SpringBootTest(classes = Application.class)
// Use a single test instance for the whole class so @BeforeAll and @AfterAll can be non-static
@TestInstance(Lifecycle.PER_CLASS)
@Execution(ExecutionMode.SAME_THREAD)
public class CapstoneServicesIntegrationTest {

    // Inject the real UserService bean from the Spring context.
    @Autowired
    private UserService userService;

    // Inject the real AuthenticationService bean from the Spring context.
    @Autowired
    private AuthenticationService authenticationService;

    // Inject the real WaterService bean from the Spring context.
    @Autowired
    private WaterService waterService;

    // Inject the real UserHealthService bean from the Spring context.
    @Autowired
    private UserHealthService userHealthService;

    // Inject the real StatisticsService bean from the Spring context.
    @Autowired
    private StatisticsService statisticsService;

    // Inject the password encoder used by the application.
    //
    // Integration tests that create users directly through UserService
    // must encode their passwords before storing them in Firebase.
    @Autowired
    private PasswordEncoder passwordEncoder;

    // Per-run usernames for shared baseline users.
    private String TEST_USERNAME_1;
    private String TEST_USERNAME_2;

    // Track all test-created users so cleanup still works even if a test fails midway.
    private final Set<String> createdUsernames = Collections.synchronizedSet(new HashSet<>());

    // Store the main test user object for convenience
    @SuppressWarnings("FieldCanBeLocal")
    private User testUser1;

    // Store the second test user object for convenience
    @SuppressWarnings("FieldCanBeLocal")
    private User testUser2;

    // --------------------------- TEST LIFECYCLE ---------------------------

    // ---------------------------------------------------------------------
    // Creates a user directly through UserService for integration tests.
    //
    // UserService.createUser() does not perform authentication logic,
    // therefore the raw password must be encoded here before the user
    // is stored in Firebase.
    //
    // This keeps directly created test users consistent with users
    // created through the real signup flow.
    // ---------------------------------------------------------------------
    private void createUserOrFail(User user) throws Exception {
        // Read the raw password from the test user.
        var rawPassword = user.getPassword();

        // Encode the password before storing the user.
        if (rawPassword != null) {
            user.setPassword(passwordEncoder.encode(rawPassword));
        }

        // Create the user through UserService.
        CompletableFuture<Boolean> future = userService.createUser(user);

        // Wait for the asynchronous creation result.
        Boolean created = future.get(20, TimeUnit.SECONDS);

        // Assert that the user was created successfully.
        assertTrue(created,
                "Failed to create test user: " + user.getUserName()
        );

        // Remember the username for cleanup.
        createdUsernames.add(user.getUserName());
    }

    // This method will run once before all tests in this class
    @BeforeAll
    void setUpTestUsers() throws Exception {
        String runId = String.valueOf(System.currentTimeMillis());
        TEST_USERNAME_1 = "integrationUser1_" + runId;
        TEST_USERNAME_2 = "integrationUser2_" + runId;

        // Create a new User instance for the first test user
        testUser1 = new User();
        // Set the username for the first test user
        testUser1.setUserName(TEST_USERNAME_1);
        // Set a password for the first test user
        testUser1.setPassword("pass1");

        // Create a new User instance for the second test user
        testUser2 = new User();
        // Set the username for the second test user
        testUser2.setUserName(TEST_USERNAME_2);
        // Set a password for the second test user
        testUser2.setPassword("pass2");

        createUserOrFail(testUser1);
        createUserOrFail(testUser2);
    }

    // This method will run once after all tests in this class
    @AfterAll
    void cleanUpTestUsers() {
        for (String username : new ArrayList<>(createdUsernames)) {
            try {
                userService.deleteUser(username).get(20, TimeUnit.SECONDS);
            } catch (Exception e) {
                System.out.println("WARN cleanup failed for username=" + username
                        + " message=" + e.getMessage());
            }
        }
    }

    // --------------------------- SIGNUP / CREATE / DELETE ---------------------------

    // Test that signup creates a new user with a BCrypt password
    // and rejects duplicate usernames.
    @Test
    void signup_createsNewUserAndRejectsDuplicate() throws Exception {
        // Build a unique username for this test.
        String uniqueUsername = "signupUser_" + System.currentTimeMillis();
        // Keep the raw password that a real client would provide.
        String rawPassword = "signupPass";
        // Create a new user for the signup flow.
        User signupUser = new User();
        // Set the username.
        signupUser.setUserName(uniqueUsername);
        // Set the raw password.
        signupUser.setPassword(rawPassword);
        // Set the full name.
        signupUser.setFullName("Sasa li");
        // Set the age.
        signupUser.setAge(25);

        // Call the real signup service.
        CompletableFuture<String> resultFuture = authenticationService.signup(signupUser);

        // Wait for the signup result.
        String firstResult = resultFuture.get(20, TimeUnit.SECONDS);

        // Assert that signup succeeded.
        assertEquals("User created successfully", firstResult);

        // Remember the created username for cleanup.
        createdUsernames.add(uniqueUsername);

        // Read the stored user directly from Firebase.
        User storedUser = userService.getUser(uniqueUsername)
                        .get(20, TimeUnit.SECONDS);

        // Assert that the user exists.
        assertNotNull(storedUser);
        // Assert that the raw password was not stored directly.
        assertNotEquals(rawPassword, storedUser.getPassword());

        // Assert that the stored BCrypt hash matches
        // the original raw password.
        assertTrue(passwordEncoder.matches(rawPassword, storedUser.getPassword()));

        // Call signup again with the same username.
        CompletableFuture<String> duplicateFuture = authenticationService.signup(signupUser);

        // Wait for the duplicate signup result.
        String secondResult = duplicateFuture.get(20, TimeUnit.SECONDS);

        // Assert that duplicate usernames are rejected.
        assertEquals("Username already exists", secondResult);
    }

    // Test that createUser, exists and deleteUser work consistently
    // while directly created users store BCrypt passwords.
    @Test
    void createUser_existsAndDeleteUser_flowWorks() throws Exception {
        // Build a temporary username for this test.
        String tempUsername = "tempUser_" + System.currentTimeMillis();
        // Keep the original raw password.
        String rawPassword = "tempPass";
        // Create the temporary user.
        User tempUser = new User();
        // Set the username.
        tempUser.setUserName(tempUsername);
        // Encode the password before calling createUser directly.
        tempUser.setPassword(passwordEncoder.encode(rawPassword));
        // Set the full name.
        tempUser.setFullName("Sasa li");
        // Set the age.
        tempUser.setAge(25);

        // Create the user directly through UserService.
        CompletableFuture<Boolean> createFuture = userService.createUser(tempUser);

        // Wait for the creation result.
        Boolean created = createFuture.get(20, TimeUnit.SECONDS);

        // Assert that creation succeeded.
        assertTrue(created);

        // Remember the user for cleanup.
        createdUsernames.add(tempUsername);

        // Read the stored user.
        User storedUser = userService.getUser(tempUsername)
                        .get(20, TimeUnit.SECONDS);

        // Assert that the user exists.
        assertNotNull(storedUser);
        // Assert that the raw password was not stored.
        assertNotEquals(rawPassword, storedUser.getPassword());
        // Assert that the stored BCrypt password matches the raw password.
        assertTrue(passwordEncoder.matches(rawPassword, storedUser.getPassword()));

        // Check that the user exists.
        CompletableFuture<Boolean> existsFuture = userService.exists(tempUsername);
        Boolean exists = existsFuture.get(20, TimeUnit.SECONDS);
        assertTrue(exists);

        // Delete the user.
        CompletableFuture<Boolean> deleteFuture = userService.deleteUser(tempUsername);
        Boolean deleted = deleteFuture.get(20, TimeUnit.SECONDS);
        assertTrue(deleted);

        // Verify that the user no longer exists.
        CompletableFuture<Boolean> existsAfterDeleteFuture = userService.exists(tempUsername);
        Boolean existsAfterDelete = existsAfterDeleteFuture.get(20, TimeUnit.SECONDS);
        assertFalse(existsAfterDelete);
    }

    // --------------------------- GET USER NEGATIVE TEST ---------------------------
    // Test that getUser returns null for a username that does not exist in Firebase
    @Test
    void getUser_nonExisting_returnsNull() throws Exception {
        // Build a username that should not exist in Firebase
        String missingUsername = "getUserNoSuch_" + System.currentTimeMillis();

        // Call getUser for this missing username
        CompletableFuture<User> future =
                userService.getUser(missingUsername);
        // Wait for the getUser result with a timeout of 20 seconds
        User result = future.get(20, TimeUnit.SECONDS);
        // Assert that no user object was found
        assertNull(result);
    }

    // --------------------------- UPDATE USER FULL RECORD TEST ---------------------------

    // --------------------------- UPDATE USER EXISTING TEST ---------------------------
    // Test that updateUser updates the editable fields,
    // stores the new password as BCrypt and returns the complete user.
    @Test
    void updateUser_existing_updatesEditableFieldsAndReturnsUpdatedUser()
            throws Exception {
        // Build a unique username for this test.
        String tempUsername = "updateUserDeep_" + System.currentTimeMillis();

        // Create the original user.
        User originalUser = new User();
        // Set the username.
        originalUser.setUserName(tempUsername);
        // Set the raw original password.
        originalUser.setPassword("origPass");
        // Set the original full name.
        originalUser.setFullName("Original Name");
        // Set the original age.
        originalUser.setAge(20);

        // Create the user using the BCrypt-aware helper.
        createUserOrFail(originalUser);

        // Create the updated user object.
        User updatedUser = new User();
        // Keep the same username.
        updatedUser.setUserName(tempUsername);
        // Send a new raw password.
        // UserService.updateUser() is responsible for encoding it.
        updatedUser.setPassword("newPass");
        // Set the new full name.
        updatedUser.setFullName("Updated Name");
        // Set the new age.
        updatedUser.setAge(30);

        // Update the user.
        CompletableFuture<User> updateFuture =
                userService.updateUser(
                        tempUsername,
                        updatedUser
                );

        // Wait for the update result.
        User updated = updateFuture.get(20, TimeUnit.SECONDS);

        // Assert that a user was returned.
        assertNotNull(updated);

        // Assert that the username remains unchanged.
        assertEquals(tempUsername, updated.getUserName());
        // Assert that plaintext was not returned from the stored model.
        assertNotEquals("newPass", updated.getPassword());

        // Assert that the returned BCrypt hash matches the new password.
        assertTrue(
                passwordEncoder.matches("newPass", updated.getPassword())
        );

        // Assert that the full name was updated.
        assertEquals("Updated Name", updated.getFullName());
        // Assert that the age was updated.
        assertEquals(30, updated.getAge());

        // Read the user again directly from Firebase.
        CompletableFuture<User> getFuture = userService.getUser(tempUsername);

        User fromDb = getFuture.get(20, TimeUnit.SECONDS);

        // Assert that the stored user exists.
        assertNotNull(fromDb);

        // Assert that Firebase does not contain the raw new password.
        assertNotEquals("newPass", fromDb.getPassword());

        // Assert that the stored BCrypt password matches newPass.
        assertTrue(
                passwordEncoder.matches("newPass", fromDb.getPassword())
        );
        // Assert that the updated full name was persisted.
        assertEquals("Updated Name", fromDb.getFullName());
        // Assert that the updated age was persisted.
        assertEquals(30, fromDb.getAge());
        // Assert that the old raw password is no longer stored.
        assertNotEquals("origPass", fromDb.getPassword());
        // Assert that the old full name is gone.
        assertNotEquals("Original Name", fromDb.getFullName());
        // Assert that the old age is gone.
        assertNotEquals(20, fromDb.getAge());

        // Delete the temporary user.
        CompletableFuture<Boolean> deleteFuture = userService.deleteUser(tempUsername);
        Boolean deleted = deleteFuture.get(20, TimeUnit.SECONDS);
        // Assert that cleanup succeeded.
        assertTrue(deleted);
    }


    // --------------------------- UPDATE USER NON-EXISTING TEST ---------------------------
    // Test that updateUser returns null when trying to update a non-existing user.
    @Test
    void updateUser_nonExisting_returnsNull() throws Exception {

        // Build a username that should not exist in Firebase.
        String missingUsername =
                "updateUserNoSuch_" + System.currentTimeMillis();

        // Create a User instance with this missing username.
        User candidate = new User();

        // Set the username for the candidate user.
        candidate.setUserName(missingUsername);

        // Set a password for the candidate user.
        candidate.setPassword("somePass");

        // Set a full name for the candidate user.
        candidate.setFullName("Some Name");

        // Set an age for the candidate user.
        candidate.setAge(40);

        // Call updateUser for this missing username.
        CompletableFuture<User> updateFuture =
                userService.updateUser(missingUsername, candidate);

        // Wait for the update result with a timeout of 20 seconds.
        User updated = updateFuture.get(20, TimeUnit.SECONDS);

        // Assert that null was returned because the user does not exist.
        assertNull(updated);

        // Verify that getUser still returns null for this username.
        CompletableFuture<User> getFuture =
                userService.getUser(missingUsername);

        // Wait for the getUser result with a timeout of 20 seconds.
        User fromDb = getFuture.get(20, TimeUnit.SECONDS);

        // Assert that no user object exists in Firebase for this username.
        assertNull(fromDb);
    }

    // --------------------------- LOGIN TESTS ---------------------------

    // Test that login returns a valid User object when
    // the raw password matches the stored BCrypt password.
    @Test
    void login_withCorrectCredentials_returnsUser() throws Exception {

        // Login using the raw password that belongs
        // to the first baseline test user.
        CompletableFuture<User> loginFuture = authenticationService.login(
                        TEST_USERNAME_1,
                        "pass1"
        );

        // Wait for the authentication result.
        User loggedUser = loginFuture.get(20, TimeUnit.SECONDS);

        // Assert that authentication succeeded.
        assertNotNull(loggedUser);
        // Assert that the correct user was returned.
        assertEquals(TEST_USERNAME_1, loggedUser.getUserName());
        // The stored password must not equal the raw password.
        assertNotEquals("pass1", loggedUser.getPassword());

        // Verify that the stored BCrypt hash matches
        // the raw password used during login.
        assertTrue(
                passwordEncoder.matches("pass1", loggedUser.getPassword())
        );
    }


    // Test that login returns null when the provided raw password
    // does not match the user's stored BCrypt password.
    @Test
    void login_withWrongPassword_returnsNull() throws Exception {
        // Attempt login with the correct username
        // but an incorrect raw password.
        CompletableFuture<User> loginFuture =
                authenticationService.login(
                        TEST_USERNAME_1,
                        "wrongPass"
                );

        // Wait for the authentication result.
        User loggedUser = loginFuture.get(20, TimeUnit.SECONDS);

        // Assert that authentication failed.
        assertNull(loggedUser);
    }


    // --------------------------- WATER MODULE TESTS ---------------------------

    // Test that updateWater increases today's total and getWater reflects the change
    @Test
    void updateWater_increasesTodayTotal_and_getWaterIsConsistent() throws Exception {
        // Call getWater to read today's and yesterday's values before the update
        CompletableFuture<JSONObject> beforeFuture = waterService.getWater(TEST_USERNAME_1);
        // Wait for the JSON result with a timeout of 20 seconds
        JSONObject beforeJson = beforeFuture.get(20, TimeUnit.SECONDS);
        // Assert that the JSON object is not null
        assertNotNull(beforeJson);
        // Extract today's water amount from the JSON object
        long todayBefore = beforeJson.getLong("todayWater");

        // Define the amount of water to add in this test
        int addedAmount = 500;

        // Call updateWater to add the new amount for the given user
        CompletableFuture<Boolean> updateFuture = waterService.updateWater(TEST_USERNAME_1, addedAmount);
        // Wait for the update result with a timeout of 20 seconds
        Boolean updated = updateFuture.get(20, TimeUnit.SECONDS);
        // Assert that the update operation succeeded
        assertTrue(updated);

        // Call getWater again to read the updated values
        CompletableFuture<JSONObject> afterFuture = waterService.getWater(TEST_USERNAME_1);
        // Wait for the updated JSON result with a timeout of 20 seconds
        JSONObject afterJson = afterFuture.get(20, TimeUnit.SECONDS);
        // Assert that the JSON object is not null
        assertNotNull(afterJson);
        // Extract today's water amount after the update
        long todayAfter = afterJson.getLong("todayWater");

        // Assert that today's water increased exactly by the added amount
        assertEquals(todayBefore + addedAmount, todayAfter);

        // Build today's date key in the same format used by WaterService
        String todayKey = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

        // Call getWaterHistoryMap for the last 3 days for this user
        CompletableFuture<Map<String, Long>> historyFuture =
                waterService.getWaterHistoryMap(TEST_USERNAME_1, 3);
        // Wait for the history map result with a timeout of 20 seconds
        Map<String, Long> history = historyFuture.get(20, TimeUnit.SECONDS);
        // Assert that the history map is not null
        assertNotNull(history);
        // Assert that the history map contains exactly 3 entries (for 3 days)
        assertEquals(3, history.size());

        // Assert that the map contains an entry for today's date key
        assertTrue(history.containsKey(todayKey));
        // Assert that the value in the map for today equals today's water total we observed
        assertEquals(todayAfter, history.get(todayKey));
    }

    // Test for a "fresh" user, history map should contain only zeros for all requested days
    @Test
    void getWaterHistoryMap_forNewUser_returnsAllZerosWithExpectedKeys() throws Exception {
        // Choose the number of days we want to request in the history map
        int days = 7;
        // Create a date formatter that matches the format used by WaterService ("yyyy-MM-dd")
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        // Create a Calendar instance initialized to "now" (today)
        Calendar cal = Calendar.getInstance();

        // Create a LinkedHashMap to store the expected result (keeps insertion order)
        Map<String, Long> expected = new LinkedHashMap<>();
        // Generate the last `days` date keys and put 0L for each (new user has no water logs)
        for (int i = 0; i < days; i++) {
            // Format the current calendar date to the string key
            String dateKey = sdf.format(cal.getTime());
            // Put this date key with a value of 0 (no water logged) into the expected map
            expected.put(dateKey, 0L);
            // Move the calendar one day backwards
            cal.add(Calendar.DAY_OF_YEAR, -1);
        }

        // Call the service to get the actual water history map for the second test user
        CompletableFuture<Map<String, Long>> future =
                waterService.getWaterHistoryMap(TEST_USERNAME_2, days);
        // Wait for the asynchronous result with a timeout of 20 seconds
        Map<String, Long> actual = future.get(20, TimeUnit.SECONDS);
        // Assert that the actual map is not null
        assertNotNull(actual);
        // Assert that the actual map is exactly equal to the expected map (keys and values)
        assertEquals(expected, actual);
    }

    // Test that a "fresh" user (second test user) with no water updates returns zeros
    @Test
    void getWater_forNewUser_returnsZeroTotals() throws Exception {
        // Call getWater for the second test user (assuming no water updates done yet)
        CompletableFuture<JSONObject> future = waterService.getWater(TEST_USERNAME_2);
        // Wait for the JSON result with a timeout of 20 seconds
        JSONObject json = future.get(20, TimeUnit.SECONDS);
        // Assert that the JSON object is not null (service returns an object, not null)
        assertNotNull(json);
        // Extract today's water amount from the JSON object
        long today = json.getLong("todayWater");
        // Extract yesterday's water amount from the JSON object
        long yesterday = json.getLong("yesterdayWater");
        // Assert that today's water is zero for a new user
        assertEquals(0, today);
        // Assert that yesterday's water is also zero for a new user
        assertEquals(0, yesterday);
    }


    // add test: (add water amount for today and yesterday in the firebase ofc)
    // and check if we recive the same amounts for each day (yeaterday & tomorow)

    // --------------------------- GOAL MODULE TESTS ---------------------------

    // Test that updateGoalMl changes the goal and getGoalMl reads the updated value
    @Test
    void updateGoalMl_changesGoal_and_getGoalMlReadsIt() throws Exception {
        // Choose a valid goal value in the allowed range (between 500 and 10000)
        int newGoal = 3200;

        // Call updateGoalMl to set the new goal for the main test user
        CompletableFuture<Boolean> updateFuture =
                waterService.updateGoalMl(TEST_USERNAME_1, newGoal);
        // Wait for the update result with a timeout of 20 seconds
        Boolean updated = updateFuture.get(20, TimeUnit.SECONDS);
        // Assert that the update operation returned true
        assertTrue(updated);

        // Call getGoalMl to read the goal for the same user
        CompletableFuture<Integer> getFuture =
                waterService.getGoalMl(TEST_USERNAME_1);
        // Wait for the goal result with a timeout of 20 seconds
        Integer goalValue = getFuture.get(20, TimeUnit.SECONDS);
        // Assert that the returned goal is not null
        assertNotNull(goalValue);
        // Assert that the returned goal equals the value we set
        assertEquals(newGoal, goalValue.intValue());
    }

    // Test that updateGoalMl rejects out-of-range values and does not change the stored goal
    @Test
    void updateGoalMl_outOfRange_isRejectedAndValueNotChanged() throws Exception {
        // Build a unique username for this test run
        String username = "goalInvalidDeep_" + System.currentTimeMillis();

        // Create a new User instance for this test
        User user = new User();
        // Set username for the test user
        user.setUserName(username);
        // Encode the password with BCrypt before storing the user directly
        // through UserService.createUser().
        user.setPassword(passwordEncoder.encode("p"));

        // Create the user in Firebase using UserService.createUser
        CompletableFuture<Boolean> createFuture = userService.createUser(user);

        // Wait for the creation result with a timeout of 20 seconds
        Boolean created = createFuture.get(20, TimeUnit.SECONDS);

        // Assert that the user was created successfully
        assertTrue(created);

        // Add the created username to the internal cleanup list
        createdUsernames.add(username);

        // Call getGoalMl before any explicit update to read the default goal
        CompletableFuture<Integer> beforeFuture = waterService.getGoalMl(username);

        // Wait for the goal result with a timeout of 20 seconds
        Integer before = beforeFuture.get(20, TimeUnit.SECONDS);

        // Assert that the default goal value is 3000 for a new user
        assertEquals(3000, before.intValue());

        // Try to update the goal with a value below the allowed range
        CompletableFuture<Boolean> lowFuture = waterService.updateGoalMl(username, 100);

        // Wait for the low update result with a timeout of 20 seconds
        Boolean low = lowFuture.get(20, TimeUnit.SECONDS);

        // Try to update the goal with a value above the allowed range
        CompletableFuture<Boolean> highFuture = waterService.updateGoalMl(username, 20000);

        // Wait for the high update result with a timeout of 20 seconds
        Boolean high = highFuture.get(20, TimeUnit.SECONDS);

        // Assert that both out-of-range updates were rejected
        assertFalse(low);
        assertFalse(high);

        // Call getGoalMl again after the invalid updates
        CompletableFuture<Integer> afterFuture = waterService.getGoalMl(username);

        // Wait for the goal result with a timeout of 20 seconds
        Integer after = afterFuture.get(20, TimeUnit.SECONDS);

        // Assert that the goal value did not change after invalid updates
        assertEquals(before.intValue(), after.intValue());

        // Clean up: delete the temporary test user from Firebase
        CompletableFuture<Boolean> deleteFuture = userService.deleteUser(username);

        // Wait for the delete result with a timeout of 20 seconds
        Boolean deleted = deleteFuture.get(20, TimeUnit.SECONDS);

        // Assert that the delete operation succeeded
        assertTrue(deleted);
    }

    // Test that patchUser can update goalMl and that getGoalMl reflects this change
    @Test
    void patchUser_canUpdateGoalMlField_and_getGoalMlSeesChange() throws Exception {
        // Create a Map to hold partial updates for the user
        Map<String, Object> updates = new HashMap<>();
        // Put a new goalMl value into the updates map
        updates.put("goalMl", 4500);

        // Call patchUser with the partial updates for the main test user
        CompletableFuture<User> patchFuture =
                userService.patchUser(TEST_USERNAME_1, updates);
        // Wait for the updated User object with a timeout of 20 seconds
        User updatedUser = patchFuture.get(20, TimeUnit.SECONDS);
        // Assert that the returned User object is not null
        assertNotNull(updatedUser);

        // Call getGoalMl to verify that goalMl was really updated in Firebase
        CompletableFuture<Integer> goalFuture =
                waterService.getGoalMl(TEST_USERNAME_1);
        // Wait for the goal result with a timeout of 20 seconds
        Integer goalValue = goalFuture.get(20, TimeUnit.SECONDS);
        // Assert that the returned goal is not null
        assertNotNull(goalValue);
        // Assert that the returned goal equals the value we patched
        assertEquals(4500, goalValue.intValue());
    }

    // --------------------------- CALORIES MODULE TESTS ---------------------------

    // Test that updateCalories sets the field and getCalories reads the same value
    @Test
    void updateCalories_setsValue_and_getCaloriesReadsIt() throws Exception {
        // Choose a valid calories value (between 0 and 20000 according to validation)
        int newCalories = 1234;

        // Call updateCalories for the main test user
        CompletableFuture<Boolean> updateFuture =
                userHealthService.updateCalories(TEST_USERNAME_1, newCalories);
        // Wait for the update result with a timeout of 20 seconds
        Boolean updated = updateFuture.get(20, TimeUnit.SECONDS);
        // Assert that the update operation succeeded
        assertTrue(updated);

        // Call getCalories to read the calories value for the same user
        CompletableFuture<Integer> getFuture =
                userHealthService.getCalories(TEST_USERNAME_1);
        // Wait for the calories result with a timeout of 20 seconds
        Integer calories = getFuture.get(20, TimeUnit.SECONDS);
        // Assert that the returned calories value is not null
        assertNotNull(calories);
        // Assert that the returned calories value matches what we set
        assertEquals(newCalories, calories.intValue());
    }

    // Test that getCalories returns 0 for a user with no calories set yet (second test user)
    @Test
    void getCalories_forNewUser_returnsZero() throws Exception {
        // Call getCalories for the second test user
        CompletableFuture<Integer> getFuture =
                userHealthService.getCalories(TEST_USERNAME_2);
        // Wait for the calories result with a timeout of 20 seconds
        Integer calories = getFuture.get(20, TimeUnit.SECONDS);
        // Assert that the returned calories value is not null
        assertNotNull(calories);
        // Assert that for a new user, calories default is 0
        assertEquals(0, calories.intValue());
    }

    // Test that getCalories returns 0 when the user does not exist in Firebase
    @Test
    void getCalories_userNotFound_returnsZero() throws Exception {
        // Build a username that should not exist in Firebase
        String missingUsername = "noSuchUser_" + System.currentTimeMillis();

        // Call getCalories for this non-existing username
        CompletableFuture<Integer> future =
                userHealthService.getCalories(missingUsername);
        // Wait for the calories result with a timeout of 20 seconds
        Integer cals = future.get(20, TimeUnit.SECONDS);
        // Assert that the returned calories value is exactly 0
        assertEquals(0, cals.intValue());
    }


    // Test that updateCalories accepts valid values, rejects invalid ones,
    // and keeps the last valid value.
    @Test
    void updateCalories_validAndInvalidValues_behaveAsExpected() throws Exception {
        // Build a unique username for this test run
        String username = "calDeep_" + System.currentTimeMillis();
        // Create a new User instance for this test
        User user = new User();
        // Set username for the test user
        user.setUserName(username);
        // Encode the password with BCrypt before storing the user directly
        // through UserService.createUser().
        user.setPassword(passwordEncoder.encode("p"));

        // Create the user in Firebase
        CompletableFuture<Boolean> createFuture = userService.createUser(user);
        // Wait for the creation result with a timeout of 20 seconds
        Boolean created = createFuture.get(20, TimeUnit.SECONDS);
        // Assert that the user was created successfully
        assertTrue(created);

        // Add the created username to the internal cleanup list
        createdUsernames.add(username);

        // Read the initial calories value for this user
        CompletableFuture<Integer> initialFuture = userHealthService.getCalories(username);
        // Wait for the calories result with a timeout of 20 seconds
        Integer initial = initialFuture.get(20, TimeUnit.SECONDS);
        // Assert that the initial calories value is 0
        assertEquals(0, initial.intValue());

        // ---- Valid update ----

        // Call updateCalories with a valid value inside the allowed range
        CompletableFuture<Boolean> validUpdateFuture =
                userHealthService.updateCalories(username, 1200);

        // Wait for the update result with a timeout of 20 seconds
        Boolean validUpdated = validUpdateFuture.get(20, TimeUnit.SECONDS);

        // Assert that the update operation succeeded
        assertTrue(validUpdated);

        // Read calories after the valid update
        CompletableFuture<Integer> afterValidFuture = userHealthService.getCalories(username);
        // Wait for the calories result with a timeout of 20 seconds
        Integer afterValid = afterValidFuture.get(20, TimeUnit.SECONDS);
        // Assert that the calories value was updated correctly to 1200
        assertEquals(1200, afterValid.intValue());

        // ---- Invalid updates ----

        // Try to update calories with a negative value
        CompletableFuture<Boolean> invalidLowFuture =
                userHealthService.updateCalories(username, -5);
        // Wait for the invalid low update result
        Boolean invalidLow = invalidLowFuture.get(20, TimeUnit.SECONDS);
        // Assert that the negative value was rejected
        assertFalse(invalidLow);

        // Try to update calories with a value above the allowed maximum
        CompletableFuture<Boolean> invalidHighFuture =
                userHealthService.updateCalories(username, 50000);
        // Wait for the invalid high update result
        Boolean invalidHigh = invalidHighFuture.get(20, TimeUnit.SECONDS);
        // Assert that the excessive value was rejected
        assertFalse(invalidHigh);

        // Read calories again after both invalid updates
        CompletableFuture<Integer> afterInvalidFuture =
                userHealthService.getCalories(username);
        // Wait for the calories result with a timeout of 20 seconds
        Integer afterInvalid = afterInvalidFuture.get(20, TimeUnit.SECONDS);
        // Assert that the last valid value remained unchanged
        assertEquals(1200, afterInvalid.intValue());

        // Clean up: delete the temporary test user
        CompletableFuture<Boolean> deleteFuture = userService.deleteUser(username);
        // Wait for the delete result with a timeout of 20 seconds
        Boolean deleted = deleteFuture.get(20, TimeUnit.SECONDS);
        // Assert that the delete operation succeeded
        assertTrue(deleted);
    }

    // --------------------------- BMI DISTRIBUTION BASIC TEST ---------------------------

    // Deep test: BMI distribution should correctly count 4 new users, one in each category
    @Test
    void getBmiDistribution_countsEachBmiCategoryForNewUsers() throws Exception {
        // Call getBmiDistribution once to capture the initial state before adding test users
        CompletableFuture<Map<String, Integer>> beforeFuture = statisticsService.getBmiDistribution();
        // Wait for the "before" distribution map with a timeout of 20 seconds
        Map<String, Integer> before = beforeFuture.get(20, TimeUnit.SECONDS);
        // Assert that the "before" map is not null
        assertNotNull(before);

        // Read the initial count for the "Underweight" category
        var underBefore = before.getOrDefault("Underweight", 0);
        // Read the initial count for the "Normal" category
        var normalBefore = before.getOrDefault("Normal", 0);
        // Read the initial count for the "Overweight" category
        var overBefore = before.getOrDefault("Overweight", 0);
        // Read the initial count for the "Obese" category
        var obeseBefore = before.getOrDefault("Obese", 0);

        // Build a common prefix for temporary BMI test users using the current timestamp
        var prefix = "bmiTestUser_" + System.currentTimeMillis();
        // Create an array of usernames for the four BMI test users
        var bmiUsers = new String[]{
                prefix + "_u",
                prefix + "_n",
                prefix + "_o",
                prefix + "_ob"
        };

        // Create an array of BMI values matching the categories in the same order
        double[] bmiValues = new double[]{
                17.0,
                22.0,
                27.0,
                32.0
        };

        // Loop through all BMI test users
        for (var i = 0; i < bmiUsers.length; i++) {
            // Create a new User instance for the current BMI test user
            var user = new User();
            // Set the username for this test user
            user.setUserName(bmiUsers[i]);
            // Encode the password with BCrypt before storing the user directly through UserService.createUser()
            user.setPassword(passwordEncoder.encode("bmiPass"));

            // Create the user in Firebase using UserService.createUser
            CompletableFuture<Boolean> createFuture = userService.createUser(user);
            // Wait for the creation result with a timeout of 20 seconds
            Boolean created = createFuture.get(20, TimeUnit.SECONDS);
            // Assert that the user was created successfully
            assertTrue(created);

            // Add the created username to the internal cleanup list
            createdUsernames.add(bmiUsers[i]);

            // Update the BMI value for this user according to the array
            CompletableFuture<Boolean> bmiFuture = userHealthService.updateBmi(bmiUsers[i], bmiValues[i]);
            // Wait for the BMI update result with a timeout of 20 seconds
            Boolean bmiUpdated = bmiFuture.get(20, TimeUnit.SECONDS);
            // Assert that the BMI update operation succeeded
            assertTrue(bmiUpdated);
        }

        // Call getBmiDistribution again after adding the four new test users
        CompletableFuture<Map<String, Integer>> afterFuture = statisticsService.getBmiDistribution();
        // Wait for the "after" distribution map with a timeout of 20 seconds
        Map<String, Integer> after = afterFuture.get(20, TimeUnit.SECONDS);
        // Assert that the "after" map is not null
        assertNotNull(after);

        // Read the updated count for the "Underweight" category
        var underAfter = after.getOrDefault("Underweight", 0);
        // Read the updated count for the "Normal" category
        var normalAfter = after.getOrDefault("Normal", 0);
        // Read the updated count for the "Overweight" category
        var overAfter = after.getOrDefault("Overweight", 0);
        // Read the updated count for the "Obese" category
        var obeseAfter = after.getOrDefault("Obese", 0);

        // Assert that the "Underweight" count increased exactly by 1
        assertEquals(underBefore + 1, underAfter);
        // Assert that the "Normal" count increased exactly by 1
        assertEquals(normalBefore + 1, normalAfter);
        // Assert that the "Overweight" count increased exactly by 1
        assertEquals(overBefore + 1, overAfter);
        // Assert that the "Obese" count increased exactly by 1
        assertEquals(obeseBefore + 1, obeseAfter);

        // Finally, clean up all four temporary BMI test users from Firebase
        for (String username : bmiUsers) {
            // Call deleteUser for the current temporary BMI user
            CompletableFuture<Boolean> deleteFuture = userService.deleteUser(username);

            // Wait for the deletion result with a timeout of 20 seconds
            Boolean deleted = deleteFuture.get(20, TimeUnit.SECONDS);

            // Assert that the delete operation finished successfully
            assertTrue(deleted);
        }
    }

}