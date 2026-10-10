@file:Suppress("PackageName", "UNCHECKED_CAST")
// Define the package for this integration test class.
package CapstoneTests
import org.example.CapstoneProject.Application
import org.example.CapstoneProject.dto.LoginResponse
import org.example.CapstoneProject.dto.UserResponse
import org.example.CapstoneProject.model.User
import org.example.CapstoneProject.service.JwtService
import org.example.CapstoneProject.service.UserService
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.resttestclient.TestRestTemplate
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.security.crypto.password.PasswordEncoder
import java.util.*
import java.util.concurrent.TimeUnit

// -------------------------------------------------------------------------
// UsersControllerIntegrationTest is an end-to-end integration test class.
//
// It loads a real Spring Boot web server and uses TestRestTemplate
// to perform HTTP requests against the /api/users endpoints.
//
// The tests verify:
//
// - request mappings
// - HTTP status codes
// - JSON serialization and deserialization
// - JWT authentication
// - signup and login
// - user retrieval and updates
// - user deletion
// - BMI updates
// - water operations
// - water history
// - weekly averages
// - water goals
// - BMI statistics
// - calories
//
// Test users are created and cleaned up through UserService.
//
// MongoDB transaction and low-level concurrency behavior is tested
// separately inside CapstoneServicesIntegrationTest.
// -------------------------------------------------------------------------

// Load the complete Spring Boot application context.
@SpringBootTest(
    // Use the real application configuration.
    classes = [Application::class],
    // Start a real web server on a random available port.
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    // Disable HTTPS only for this integration test environment.
    properties = ["server.ssl.enabled=false"]
)

@AutoConfigureTestRestTemplate
// Use one test instance for the complete test class.
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class UsersControllerIntegrationTest {
    // Inject TestRestTemplate to perform real HTTP requests.
    @Autowired
    private lateinit var restTemplate: TestRestTemplate

    // Inject UserService to prepare and clean up test users.
    @Autowired
    private lateinit var userService: UserService

    // Inject PasswordEncoder to verify stored BCrypt hashes.
    @Autowired
    private lateinit var passwordEncoder: PasswordEncoder

    // Inject JwtService to generate real JWT tokens for protected requests.
    @Autowired

    private lateinit var jwtService: JwtService

    // Define the maximum wait time for asynchronous service operations.
    private val timeOutSeconds = 20L

    // Keep track of every username created by this test class.
    private val createdUsernames: MutableSet<String> =
        Collections.synchronizedSet(HashSet())

    // ---------------------------------------------------------------------
    // HELPER METHODS
    // ---------------------------------------------------------------------

    // ---------------------------------------------------------------------
    // Builds a basic User object used by the integration tests.
    // ---------------------------------------------------------------------
    private fun buildUser(username: String, password: String): User {
        // Create a new User object.
        val user = User()
        // Store the supplied username.
        user.userName = username
        // Store the supplied raw password.
        user.password = password
        // Store a predictable full name for assertions.
        user.fullName = "Test User $username"
        // Store a predictable age for assertions.
        user.age = 25
        // Return the prepared user.
        return user
    }

    // ---------------------------------------------------------------------
    // Creates a test user directly through UserService.
    //
    // UserService.createUser() performs BCrypt encoding itself.
    //
    // Therefore this helper passes the raw password exactly once.
    // Pre-encoding the password here would cause double BCrypt encoding.
    // ---------------------------------------------------------------------
    @Suppress("UnusedReturnValue")
    private fun createTestUser(username: String, password: String): User {
        // Build a user containing the raw password.
        val user = buildUser(username, password)
        // Start the asynchronous user creation operation.
        val future = userService.createUser(user)
        // Wait for the user creation result.
        val created = future.get(timeOutSeconds, TimeUnit.SECONDS)

        // Fail the test when the user could not be created.
        assertTrue(created,
            "Failed to create test user in the database: $username"
        )

        // Check that the creation operation succeeded.
        if (created) {
            // Remember the username for cleanup.
            createdUsernames.add(username)
        }

        // Return the created User object.
        return user
    }

    // ---------------------------------------------------------------------
    // Deletes a test user safely through UserService.
    // ---------------------------------------------------------------------
    private fun deleteTestUser(username: String) {
        try {
            // Start the asynchronous delete operation.
            val future = userService.deleteUser(username)

            // Wait for the delete operation to complete.
            future.get(timeOutSeconds, TimeUnit.SECONDS)

        } catch (e: Exception) {
            // Print a warning without stopping cleanup of other users.
            println(
                "WARN cleanup failed for username=$username message=${e.message}"
            )
        }
    }

    // ---------------------------------------------------------------------
    // Returns the username that should be used to generate a JWT for the
    // current protected request.
    //
    // Public endpoints return null.
    //
    // For user-specific routes, the first path segment after /api/users/
    // represents the username and must match the JWT subject.
    // ---------------------------------------------------------------------
    private fun getJwtUsernameForRequest(path: String): String? {
        // Define the base API path.
        val basePath = "/api/users"
        // Find where the API base path begins inside the request path.
        val baseIndex = path.indexOf(basePath)

        // Return null when the request does not contain the expected API path.
        if (baseIndex == -1) { return null }

        // Keep only the API path starting from /api/users.
        val apiPath = path.substring(baseIndex)

        // Check whether the request targets one of the public endpoints.
        if (
            apiPath == "/api/users/health" ||
            apiPath == "/api/users/signup" ||
            apiPath == "/api/users/login" ||
            apiPath == "/api/users/stats/bmiDistribution"
        ) {
            // Public endpoints do not require a JWT.
            return null
        }

        // Check whether the request targets the protected GET-all-users route.
        if (apiPath == "/api/users") {
            // Return a fixed subject because this route has no username path variable.
            return "controller-integration-test"
        }

        // Define the prefix used before user-specific routes.
        val prefix = "/api/users/"

        // Reject paths that do not follow the expected user-specific structure.
        if (!apiPath.startsWith(prefix)) { return null }

        // Remove /api/users/ from the path.
        val remainingPath = apiPath.substring(prefix.length)

        // Find the slash separating the username from another path segment.
        val slashIndex = remainingPath.indexOf('/')

        // Check whether the path ends immediately after the username.
        if (slashIndex == -1) {
            // The entire remaining path is the username.
            return remainingPath
        }

        // Return only the username before the next slash.
        return remainingPath.substring(0, slashIndex)
    }

    // ---------------------------------------------------------------------
    // TEST SETUP
    // ---------------------------------------------------------------------

    // Run once before all controller integration tests.
    @BeforeAll
    fun beforeAll() {
        // Print a debug message indicating that this test class started.
        println("DEBUG UsersControllerIntegrationTest")

        // Add an interceptor that attaches a real JWT to protected requests.
        restTemplate.restTemplate.interceptors.add { request, body, execution ->
            // Determine which username should be used as the JWT subject.
            val username = getJwtUsernameForRequest(request.uri.path)

            // Check whether this request requires authentication.
            if (!username.isNullOrBlank()) {
                // Generate a real JWT for the required username.
                val token = jwtService.generateToken(username)
                // Attach the token using the standard Bearer Authorization header.
                request.headers.setBearerAuth(token)
            }

            // Continue executing the HTTP request.
            execution.execute(request, body)
        }
    }

    // Run once after all tests finish.
    @AfterAll
    fun cleanupAllTestUsers() {
        // Iterate over a snapshot of all usernames created by the test suite.
        for (username in ArrayList(createdUsernames)) {
            // Delete the current test user.
            deleteTestUser(username)
        }
    }

    // ---------------------------------------------------------------------
    // HEALTH CHECK TEST
    // ---------------------------------------------------------------------

    // ---------------------------------------------------------------------
    // Verifies that GET /api/users/health returns HTTP 200 and "OK".
    // ---------------------------------------------------------------------
    @Test
    fun health_returnsOk() {
        // Perform the health-check request.
        val response = restTemplate.getForEntity(
            "/api/users/health",
            String::class.java
        )

        // Verify that the server returns HTTP 200.
        assertEquals(HttpStatus.OK, response.statusCode)

        // Verify the exact health-check response body.
        assertEquals("OK", response.body)
    }

    // ---------------------------------------------------------------------
    // SIGNUP TESTS
    // ---------------------------------------------------------------------

    // ---------------------------------------------------------------------
    // Verifies that signup:
    // - creates a new user
    // - stores a BCrypt hash instead of the raw password
    // - rejects duplicate usernames
    // ---------------------------------------------------------------------
    @Test
    fun signup_createsUserAndRejectsDuplicate() {
        // Generate a unique username.
        val username = "signupController_${System.currentTimeMillis()}"

        // Define the raw password sent by the client.
        val rawPassword = "signupPass"

        // Build the signup request object.
        val user = buildUser(username, rawPassword)

        // Send the first signup request.
        val firstResponse = restTemplate.postForEntity(
            "/api/users/signup",
            user,
            String::class.java
        )

        // Verify that the first signup returns HTTP 201 Created.
        assertEquals(HttpStatus.CREATED, firstResponse.statusCode)

        // Verify the successful signup message.
        assertEquals("User created successfully", firstResponse.body)

        // Register the created username for cleanup.
        createdUsernames.add(username)

        // Read the stored user directly through UserService.
        val storedUser = userService
            .getUser(username)
            .get(timeOutSeconds, TimeUnit.SECONDS)

        // Verify that the user exists.
        assertNotNull(storedUser)

        // Verify that the raw password was not stored directly.
        assertNotEquals(
            rawPassword,
            storedUser!!.password
        )

        // Verify that the stored BCrypt hash matches the raw password.
        assertTrue(
            passwordEncoder.matches(
                rawPassword,
                storedUser.password!!
            )
        )

        // Send the same signup request again.
        val secondResponse = restTemplate.postForEntity(
            "/api/users/signup",
            user,
            String::class.java
        )

        // Verify that the duplicate request returns HTTP 409 Conflict.
        assertEquals(
            HttpStatus.CONFLICT,
            secondResponse.statusCode
        )

        // Verify the duplicate username response message.
        assertEquals(
            "Username already exists",
            secondResponse.body
        )
    }

    // ---------------------------------------------------------------------
    // LOGIN TESTS
    // ---------------------------------------------------------------------

    // ---------------------------------------------------------------------
    // Verifies successful login using the correct raw password against
    // the BCrypt hash stored in MongoDB.
    // ---------------------------------------------------------------------
    @Test
    fun login_withCorrectCredentials_returnsUserJson() {

        // Generate a unique username.
        val username =
            "loginOk_${System.currentTimeMillis()}"

        // Create the user before performing login.
        createTestUser(username, "pass1")

        // Create the login request object.
        val loginRequestUser = User()

        // Set the username supplied during authentication.
        loginRequestUser.userName = username

        // Set the raw password supplied during authentication.
        loginRequestUser.password = "pass1"

        // Send the login request.
        val response = restTemplate.postForEntity(
            "/api/users/login",
            loginRequestUser,
            LoginResponse::class.java
        )

        // Verify that login succeeds.
        assertEquals(
            HttpStatus.OK,
            response.statusCode
        )

        // Extract the login response body.
        val body = response.body

        // Verify that a response body exists.
        assertNotNull(body)

        // Verify that the server returned a JWT.
        assertNotNull(body!!.token)

        // Verify that the JWT is not blank.
        assertFalse(body.token.isBlank())

        // Verify that the returned username is correct.
        assertEquals(
            username,
            body.userName
        )

        // Verify that the returned age is correct.
        assertEquals(
            25,
            body.age
        )

        // Verify that the returned full name is correct.
        assertEquals(
            "Test User $username",
            body.fullName
        )
    }

    // ---------------------------------------------------------------------
    // Verifies that login returns HTTP 401 for an incorrect password.
    // ---------------------------------------------------------------------
    @Test
    fun login_withWrongPassword_returns401() {

        // Generate a unique username.
        val username =
            "loginBad_${System.currentTimeMillis()}"

        // Create a user with the correct password.
        createTestUser(username, "realPass")

        // Create a login request object.
        val loginRequestUser = User()

        // Set the correct username.
        loginRequestUser.userName = username

        // Set an incorrect password.
        loginRequestUser.password = "wrongPass"

        // Send the login request.
        val response = restTemplate.postForEntity(
            "/api/users/login",
            loginRequestUser,
            String::class.java
        )

        // Verify that authentication fails with HTTP 401 Unauthorized.
        assertEquals(
            HttpStatus.UNAUTHORIZED,
            response.statusCode
        )

        // Verify the authentication failure message.
        assertEquals(
            "Invalid username or password",
            response.body
        )
    }

    // ---------------------------------------------------------------------
    // GET ALL USERS TEST
    // ---------------------------------------------------------------------

    // ---------------------------------------------------------------------
    // Verifies that GET /api/users returns an array containing
    // the user created by the test.
    // ---------------------------------------------------------------------
    @Test
    fun getAllUsers_returnsArrayAndContainsAtLeastOneUser() {

        // Generate a unique username.
        val username =
            "allUsers_${System.currentTimeMillis()}"

        // Create the user through the service layer.
        createTestUser(username, "p")

        // Perform GET /api/users and deserialize into UserResponse[].
        val response = restTemplate.getForEntity(
            "/api/users",
            arrayOf<UserResponse>()::class.java
        )

        // Verify that the request succeeds.
        assertEquals(
            HttpStatus.OK,
            response.statusCode
        )

        // Extract the returned users array.
        val users = response.body

        // Verify that the response body exists.
        assertNotNull(users)

        // Verify that the created user appears in the returned array.
        assertTrue(
            users!!.any { user -> user.userName == username },
            "Expected getAllUsers response to include created test user: $username"
        )
    }

    // ---------------------------------------------------------------------
    // GET USER TESTS
    // ---------------------------------------------------------------------

    // ---------------------------------------------------------------------
    // Verifies that GET /api/users/{username} returns the expected user.
    // ---------------------------------------------------------------------
    @Test
    fun getUser_existingUser_returnsUserJson() {

        // Generate a unique username.
        val username =
            "getUserOk_${System.currentTimeMillis()}"

        // Create the user through the service layer.
        createTestUser(username, "p")

        // Perform the GET request and deserialize into UserResponse.
        val response = restTemplate.getForEntity(
            "/api/users/$username",
            UserResponse::class.java
        )

        // Verify that the request succeeds.
        assertEquals(
            HttpStatus.OK,
            response.statusCode
        )

        // Extract the response body.
        val body = response.body

        // Verify that the body exists.
        assertNotNull(body)

        // Verify the username.
        assertEquals(
            username,
            body!!.userName
        )

        // Verify the full name.
        assertEquals(
            "Test User $username",
            body.fullName
        )

        // Verify the age.
        assertEquals(
            25,
            body.age
        )
    }

    // ---------------------------------------------------------------------
    // Verifies that requesting a missing user returns HTTP 404.
    // ---------------------------------------------------------------------
    @Test
    fun getUser_nonExistingUser_returns404() {

        // Generate a username that should not exist.
        val username =
            "noSuchUser_${System.currentTimeMillis()}"

        // Perform the GET request.
        val response = restTemplate.getForEntity(
            "/api/users/$username",
            String::class.java
        )

        // Verify HTTP 404 Not Found.
        assertEquals(
            HttpStatus.NOT_FOUND,
            response.statusCode
        )

        // Verify the error message.
        assertEquals(
            "User not found",
            response.body
        )
    }

    // ---------------------------------------------------------------------
    // PATCH USER TESTS
    // ---------------------------------------------------------------------

    // ---------------------------------------------------------------------
    // Verifies that PATCH updates the requested fullName field.
    // ---------------------------------------------------------------------
    @Test
    fun patchUser_existingUser_updatesField() {

        // Generate a unique username.
        val username =
            "patchUserOk_${System.currentTimeMillis()}"

        // Create the original user.
        createTestUser(username, "p")

        // Create the dynamic PATCH update map.
        val updates = HashMap<String, Any>()

        // Request a fullName update.
        updates["fullName"] = "Patched Name"

        // Wrap the update map in an HTTP request entity.
        val entity = HttpEntity(updates)

        // Perform the PATCH request.
        val response = restTemplate.exchange(
            "/api/users/{username}",
            HttpMethod.PATCH,
            entity,
            UserResponse::class.java,
            username
        )

        // Verify that the update succeeds.
        assertEquals(
            HttpStatus.OK,
            response.statusCode
        )

        // Extract the returned user.
        val body = response.body

        // Verify that the response body exists.
        assertNotNull(body)

        // Verify that fullName was changed.
        assertEquals(
            "Patched Name",
            body!!.fullName
        )

        // Verify that the username was not changed.
        assertEquals(
            username,
            body.userName
        )
    }

    // ---------------------------------------------------------------------
    // Verifies that PATCH encodes a new raw password and allows login
    // with that new password.
    // ---------------------------------------------------------------------
    @Test
    fun patchUser_password_encodesPasswordAndAllowsLogin() {

        // Generate a unique username.
        val username =
            "patchPassword_${System.currentTimeMillis()}"

        // Create the user with the original password.
        createTestUser(username, "oldPass")

        // Create the PATCH update map.
        val updates = HashMap<String, Any>()

        // Put the new raw password into the PATCH request.
        updates["password"] = "patchedPass"

        // Wrap the PATCH body inside an HTTP entity.
        val entity = HttpEntity(updates)

        // Perform the password PATCH request.
        val patchResponse = restTemplate.exchange(
            "/api/users/{username}",
            HttpMethod.PATCH,
            entity,
            UserResponse::class.java,
            username
        )

        // Verify that the PATCH request succeeds.
        assertEquals(
            HttpStatus.OK,
            patchResponse.statusCode
        )

        // Verify that a public UserResponse was returned.
        assertNotNull(patchResponse.body)

        // Read the complete internal user through UserService.
        val storedUser = userService
            .getUser(username)
            .get(timeOutSeconds, TimeUnit.SECONDS)

        // Verify that the user still exists.
        assertNotNull(storedUser)

        // Verify that the raw patched password was not stored directly.
        assertNotEquals(
            "patchedPass",
            storedUser!!.password
        )

        // Verify that the stored BCrypt hash matches the new raw password.
        assertTrue(
            passwordEncoder.matches(
                "patchedPass",
                storedUser.password!!
            )
        )

        // Build a login request using the new password.
        val loginRequest = User()

        // Set the username.
        loginRequest.userName = username

        // Set the new raw password.
        loginRequest.password = "patchedPass"

        // Perform login with the patched password.
        val loginResponse = restTemplate.postForEntity(
            "/api/users/login",
            loginRequest,
            LoginResponse::class.java
        )

        // Verify that authentication succeeds.
        assertEquals(
            HttpStatus.OK,
            loginResponse.statusCode
        )

        // Extract the login response body.
        val loginBody = loginResponse.body

        // Verify that a login response exists.
        assertNotNull(loginBody)

        // Verify that login returned a JWT.
        assertNotNull(loginBody!!.token)

        // Verify that the JWT is not blank.
        assertFalse(loginBody.token.isBlank())

        // Verify that the authenticated username is correct.
        assertEquals(
            username,
            loginBody.userName
        )
    }

    // ---------------------------------------------------------------------
    // Verifies that PATCH returns HTTP 404 for a missing user.
    // ---------------------------------------------------------------------
    @Test
    fun patchUser_nonExistingUser_returns404() {

        // Generate a username that should not exist.
        val username =
            "patchNoSuch_${System.currentTimeMillis()}"

        // Create a PATCH update map.
        val updates = HashMap<String, Any>()

        // Add one valid field to the update request.
        updates["fullName"] = "Someone"

        // Wrap the update map inside an HTTP entity.
        val entity = HttpEntity(updates)

        // Perform the PATCH request.
        val response = restTemplate.exchange(
            "/api/users/{username}",
            HttpMethod.PATCH,
            entity,
            String::class.java,
            username
        )

        // Verify HTTP 404 Not Found.
        assertEquals(
            HttpStatus.NOT_FOUND,
            response.statusCode
        )

        // Verify the error message.
        assertEquals(
            "User not found",
            response.body
        )
    }

    // ---------------------------------------------------------------------
    // DELETE USER TESTS
    // ---------------------------------------------------------------------

    // ---------------------------------------------------------------------
    // Verifies that deleting an existing user returns HTTP 200.
    // ---------------------------------------------------------------------
    @Test
    fun deleteUser_existingUser_returnsOk() {

        // Generate a unique username.
        val username =
            "deleteUserOk_${System.currentTimeMillis()}"

        // Create the user before deletion.
        createTestUser(username, "p")

        // Perform the DELETE request.
        val response = restTemplate.exchange(
            "/api/users/{username}",
            HttpMethod.DELETE,
            null,
            String::class.java,
            username
        )

        // Verify HTTP 200 OK.
        assertEquals(
            HttpStatus.OK,
            response.statusCode
        )

        // Verify the deletion confirmation message.
        assertEquals(
            "User deleted",
            response.body
        )
    }

    // ---------------------------------------------------------------------
    // Verifies that deleting a missing user returns HTTP 404.
    // ---------------------------------------------------------------------
    @Test
    fun deleteUser_nonExistingUser_returns404() {

        // Generate a username that should not exist.
        val username =
            "deleteNoSuch_${System.currentTimeMillis()}"

        // Perform the DELETE request.
        val response = restTemplate.exchange(
            "/api/users/{username}",
            HttpMethod.DELETE,
            null,
            String::class.java,
            username
        )

        // Verify HTTP 404 Not Found.
        assertEquals(
            HttpStatus.NOT_FOUND,
            response.statusCode
        )

        // Verify the error message.
        assertEquals(
            "User not found",
            response.body
        )
    }

    // ---------------------------------------------------------------------
    // HEAD USER TESTS
    // ---------------------------------------------------------------------

    // ---------------------------------------------------------------------
    // Verifies that HEAD returns HTTP 200 for an existing user.
    // ---------------------------------------------------------------------
    @Test
    fun headUser_existingUser_returns200() {

        // Generate a unique username.
        val username =
            "headUserOk_${System.currentTimeMillis()}"

        // Create the user.
        createTestUser(username, "p")

        // Perform the HEAD request.
        val response = restTemplate.exchange(
            "/api/users/{username}",
            HttpMethod.HEAD,
            null,
            Void::class.java,
            username
        )

        // Verify HTTP 200 OK.
        assertEquals(
            HttpStatus.OK,
            response.statusCode
        )

        // Verify that HEAD does not return a response body.
        assertNull(response.body)
    }

    // ---------------------------------------------------------------------
    // Verifies that HEAD returns HTTP 404 for a missing user.
    // ---------------------------------------------------------------------
    @Test
    fun headUser_nonExistingUser_returns404() {

        // Generate a username that should not exist.
        val username =
            "headNoSuch_${System.currentTimeMillis()}"

        // Perform the HEAD request.
        val response = restTemplate.exchange(
            "/api/users/{username}",
            HttpMethod.HEAD,
            null,
            Void::class.java,
            username
        )

        // Verify HTTP 404 Not Found.
        assertEquals(
            HttpStatus.NOT_FOUND,
            response.statusCode
        )
    }

    // ---------------------------------------------------------------------
    // UPDATE BMI TESTS
    // ---------------------------------------------------------------------

    // ---------------------------------------------------------------------
    // Verifies that BMI can be updated for an existing user.
    // ---------------------------------------------------------------------
    @Test
    fun updateBmi_existingUser_returns200() {

        // Generate a unique username.
        val username =
            "bmiOk_${System.currentTimeMillis()}"

        // Create the user.
        createTestUser(username, "p")

        // Build the BMI update URL.
        val url =
            "/api/users/$username/bmi?bmi=23.5"

        // Perform the PATCH request.
        val response = restTemplate.exchange(
            url,
            HttpMethod.PATCH,
            null,
            String::class.java
        )

        // Verify HTTP 200 OK.
        assertEquals(
            HttpStatus.OK,
            response.statusCode
        )

        // Verify the update confirmation message.
        assertEquals(
            "BMI updated successfully",
            response.body
        )
    }

    // ---------------------------------------------------------------------
    // Verifies that BMI update returns HTTP 404 for a missing user.
    // ---------------------------------------------------------------------
    @Test
    fun updateBmi_nonExistingUser_returns404() {

        // Generate a username that should not exist.
        val username =
            "bmiNoSuch_${System.currentTimeMillis()}"

        // Build the BMI update URL.
        val url =
            "/api/users/$username/bmi?bmi=23.5"

        // Perform the PATCH request.
        val response = restTemplate.exchange(
            url,
            HttpMethod.PATCH,
            null,
            String::class.java
        )

        // Verify HTTP 404 Not Found.
        assertEquals(
            HttpStatus.NOT_FOUND,
            response.statusCode
        )

        // Verify the error message.
        assertEquals(
            "User not found",
            response.body
        )
    }

    // ---------------------------------------------------------------------
    // WATER MODULE TESTS
    // ---------------------------------------------------------------------

    // ---------------------------------------------------------------------
    // Verifies updateWater and getWater together through HTTP.
    // ---------------------------------------------------------------------
    @Test
    fun updateWater_and_getWater_flowForExistingUser() {

        // Generate a unique username.
        val username =
            "waterOk_${System.currentTimeMillis()}"

        // Create the user.
        createTestUser(username, "p")

        // Read the current water totals.
        val beforeResponse = restTemplate.getForEntity(
            "/api/users/$username/water",
            Map::class.java
        )

        // Verify HTTP 200 OK.
        assertEquals(
            HttpStatus.OK,
            beforeResponse.statusCode
        )

        // Convert the raw JSON map into the expected Kotlin map type.
        val beforeBody =
            beforeResponse.body as Map<String, Any>?

        // Verify that the response body exists.
        assertNotNull(beforeBody)

        // Read today's water value as Number.
        val todayBeforeNumber =
            beforeBody!!.getOrDefault("todayWater", 0) as Number

        // Convert today's water value into Long.
        val todayBefore =
            todayBeforeNumber.toLong()

        // Define the amount that will be added.
        val addedAmount = 400

        // Build the water update URL.
        val patchUrl =
            "/api/users/$username/water?amount=$addedAmount"

        // Perform the PATCH request.
        val patchResponse = restTemplate.exchange(
            patchUrl,
            HttpMethod.PATCH,
            null,
            String::class.java
        )

        // Verify HTTP 200 OK.
        assertEquals(
            HttpStatus.OK,
            patchResponse.statusCode
        )

        // Verify the update confirmation message.
        assertEquals(
            "Water updated successfully",
            patchResponse.body
        )

        // Read the water totals again after the update.
        val afterResponse = restTemplate.getForEntity(
            "/api/users/$username/water",
            Map::class.java
        )

        // Verify HTTP 200 OK.
        assertEquals(
            HttpStatus.OK,
            afterResponse.statusCode
        )

        // Convert the returned raw map into the expected Kotlin map type.
        val afterBody =
            afterResponse.body as Map<String, Any>?

        // Verify that the response body exists.
        assertNotNull(afterBody)

        // Read today's water after the update.
        val todayAfterNumber =
            afterBody!!.getOrDefault("todayWater", 0) as Number

        // Convert the returned value into Long.
        val todayAfter =
            todayAfterNumber.toLong()

        // Verify that the total increased by exactly the inserted amount.
        assertEquals(
            todayBefore + addedAmount,
            todayAfter
        )
    }

    // ---------------------------------------------------------------------
    // Verifies that getWater returns HTTP 404 for a missing user.
    // ---------------------------------------------------------------------
    @Test
    fun getWater_nonExistingUser_returns404() {

        // Generate a username that should not exist.
        val username =
            "waterNoSuch_${System.currentTimeMillis()}"

        // Perform the water GET request.
        val response = restTemplate.getForEntity(
            "/api/users/$username/water",
            String::class.java
        )

        // Verify HTTP 404 Not Found.
        assertEquals(
            HttpStatus.NOT_FOUND,
            response.statusCode
        )

        // Verify the error message.
        assertEquals(
            "User not found",
            response.body
        )
    }

    // ---------------------------------------------------------------------
    // WATER HISTORY MAP TESTS
    // ---------------------------------------------------------------------

    // ---------------------------------------------------------------------
    // Verifies that the requested number of history days is returned.
    // ---------------------------------------------------------------------
    @Test
    fun getWaterHistoryMap_existingUser_returnsMapWithRequestedDays() {

        // Generate a unique username.
        val username =
            "waterHistoryOk_${System.currentTimeMillis()}"

        // Create the user.
        createTestUser(username, "p")

        // Define the requested history size.
        val days = 5

        // Build the history endpoint URL.
        val url =
            "/api/users/$username/waterHistoryMap?days=$days"

        // Perform the GET request.
        val response = restTemplate.getForEntity(
            url,
            Map::class.java
        )

        // Verify HTTP 200 OK.
        assertEquals(
            HttpStatus.OK,
            response.statusCode
        )

        // Convert the raw JSON body into a Kotlin map.
        val body =
            response.body as Map<String, Any>?

        // Verify that the response body exists.
        assertNotNull(body)

        // Verify that exactly the requested number of days is returned.
        assertEquals(
            days,
            body!!.size
        )
    }

    // ---------------------------------------------------------------------
    // Verifies that water history returns HTTP 404 for a missing user.
    // ---------------------------------------------------------------------
    @Test
    fun getWaterHistoryMap_nonExistingUser_returns404() {

        // Generate a username that should not exist.
        val username =
            "waterHistoryNoSuch_${System.currentTimeMillis()}"

        // Build the history endpoint URL.
        val url =
            "/api/users/$username/waterHistoryMap?days=3"

        // Perform the GET request.
        val response = restTemplate.getForEntity(
            url,
            String::class.java
        )

        // Verify HTTP 404 Not Found.
        assertEquals(
            HttpStatus.NOT_FOUND,
            response.statusCode
        )

        // Verify the error message.
        assertEquals(
            "User not found",
            response.body
        )
    }

    // ---------------------------------------------------------------------
    // WEEKLY AVERAGES TESTS
    // ---------------------------------------------------------------------

    // ---------------------------------------------------------------------
    // Verifies that weekly averages return four week entries.
    // ---------------------------------------------------------------------
    @Test
    fun getWeeklyAverages_existingUser_returnsMap() {

        // Generate a unique username.
        val username =
            "weeklyAvgOk_${System.currentTimeMillis()}"

        // Create the user.
        createTestUser(username, "p")

        // Build a water update URL so at least one week contains data.
        val waterUrl =
            "/api/users/$username/water?amount=300"

        // Add one water record.
        val waterResponse = restTemplate.exchange(
            waterUrl,
            HttpMethod.PATCH,
            null,
            String::class.java
        )

        // Verify that the water update succeeds.
        assertEquals(
            HttpStatus.OK,
            waterResponse.statusCode
        )

        // Build the weekly averages URL.
        val url =
            "/api/users/$username/weeklyAverages"

        // Perform the GET request.
        val response = restTemplate.getForEntity(
            url,
            Map::class.java
        )

        // Verify HTTP 200 OK.
        assertEquals(
            HttpStatus.OK,
            response.statusCode
        )

        // Convert the raw JSON response into a Kotlin map.
        val body =
            response.body as Map<String, Any>?

        // Verify that the response body exists.
        assertNotNull(body)

        // Verify that Week 1 through Week 4 are present.
        assertEquals(
            4,
            body!!.size
        )
    }

    // ---------------------------------------------------------------------
    // Verifies that weekly averages return HTTP 404 and an empty map
    // for a missing user.
    // ---------------------------------------------------------------------
    @Test
    fun getWeeklyAverages_nonExistingUser_returns404() {

        // Generate a username that should not exist.
        val username =
            "weeklyAvgNoSuch_${System.currentTimeMillis()}"

        // Build the weekly averages URL.
        val url =
            "/api/users/$username/weeklyAverages"

        // Perform the GET request.
        val response = restTemplate.exchange(
            url,
            HttpMethod.GET,
            null,
            Map::class.java
        )

        // Verify HTTP 404 Not Found.
        assertEquals(
            HttpStatus.NOT_FOUND,
            response.statusCode
        )

        // Convert the raw response into a Kotlin map.
        val body =
            response.body as Map<String, Any>?

        // Verify that the body exists.
        assertNotNull(body)

        // Verify that the returned map is empty.
        assertTrue(body!!.isEmpty())
    }

    // ---------------------------------------------------------------------
    // GOAL MODULE TESTS
    // ---------------------------------------------------------------------

    // ---------------------------------------------------------------------
    // Verifies that setting and reading a goal works through HTTP.
    // ---------------------------------------------------------------------
    @Test
    fun goal_setAndGet_flowForExistingUser() {

        // Generate a unique username.
        val username =
            "goalOk_${System.currentTimeMillis()}"

        // Create the user.
        createTestUser(username, "p")

        // Define a valid water goal.
        val newGoal = 3400

        // Build the goal update URL.
        val setUrl =
            "/api/users/$username/goal?goalMl=$newGoal"

        // Perform the PUT request.
        val setResponse = restTemplate.exchange(
            setUrl,
            HttpMethod.PUT,
            null,
            Map::class.java
        )

        // Verify HTTP 200 OK.
        assertEquals(
            HttpStatus.OK,
            setResponse.statusCode
        )

        // Convert the returned body into a Kotlin map.
        val setBody =
            setResponse.body as Map<String, Any>?

        // Verify that a response body exists.
        assertNotNull(setBody)

        // Verify the success status returned by the endpoint.
        assertEquals(
            "OK",
            setBody!!["status"]
        )

        // Build the GET goal URL.
        val getUrl =
            "/api/users/$username/goal"

        // Read the current goal.
        val getResponse = restTemplate.getForEntity(
            getUrl,
            Map::class.java
        )

        // Verify HTTP 200 OK.
        assertEquals(
            HttpStatus.OK,
            getResponse.statusCode
        )

        // Convert the returned goal JSON into a Kotlin map.
        val getBody =
            getResponse.body as Map<String, Any>?

        // Verify that the body exists.
        assertNotNull(getBody)

        // Read goalMl as Number.
        val goalNumber =
            getBody!!["goalMl"] as Number

        // Verify that the stored goal matches the requested value.
        assertEquals(
            newGoal,
            goalNumber.toInt()
        )
    }

    // ---------------------------------------------------------------------
    // Verifies that an invalid water goal returns HTTP 400.
    // ---------------------------------------------------------------------
    @Test
    fun setGoal_invalidValue_returnsBadRequest() {

        // Generate a unique username.
        val username =
            "goalInvalid_${System.currentTimeMillis()}"

        // Create the user.
        createTestUser(username, "p")

        // Build a URL containing a goal below the valid range.
        val url =
            "/api/users/$username/goal?goalMl=100"

        // Perform the PUT request.
        val response = restTemplate.exchange(
            url,
            HttpMethod.PUT,
            null,
            Map::class.java
        )

        // Verify HTTP 400 Bad Request.
        assertEquals(
            HttpStatus.BAD_REQUEST,
            response.statusCode
        )

        // Convert the returned JSON into a Kotlin map.
        val body =
            response.body as Map<String, Any>?

        // Verify that the error body exists.
        assertNotNull(body)

        // Verify the expected endpoint status value.
        assertEquals(
            "INVALID_OR_NOT_FOUND",
            body!!["status"]
        )
    }

    // ---------------------------------------------------------------------
    // BMI DISTRIBUTION TEST
    // ---------------------------------------------------------------------

    // ---------------------------------------------------------------------
    // Verifies that the BMI distribution endpoint returns HTTP 200
    // and a JSON map.
    // ---------------------------------------------------------------------
    @Test
    fun getBmiDistribution_returnsOkWithMap() {

        // Define the public BMI distribution URL.
        val url =
            "/api/users/stats/bmiDistribution"

        // Perform the GET request.
        val response = restTemplate.getForEntity(
            url,
            Map::class.java
        )

        // Verify HTTP 200 OK.
        assertEquals(
            HttpStatus.OK,
            response.statusCode
        )

        // Convert the response body into a Kotlin map.
        val body =
            response.body as Map<String, Any>?

        // Verify that a map was returned.
        assertNotNull(body)
    }

    // ---------------------------------------------------------------------
    // CALORIES MODULE TESTS
    // ---------------------------------------------------------------------

    // ---------------------------------------------------------------------
    // Verifies valid and invalid calorie updates and confirms that
    // invalid updates do not overwrite the last valid value.
    // ---------------------------------------------------------------------
    @Test
    fun calories_updateAndGet_flowWithValidAndInvalidValues() {

        // Generate a unique username.
        val username =
            "caloriesOk_${System.currentTimeMillis()}"

        // Create the user.
        createTestUser(username, "p")

        // Build the calories GET URL.
        val getInitialUrl =
            "/api/users/$username/calories"

        // Read the initial calories value.
        val initialResponse = restTemplate.getForEntity(
            getInitialUrl,
            Map::class.java
        )

        // Verify HTTP 200 OK.
        assertEquals(
            HttpStatus.OK,
            initialResponse.statusCode
        )

        // Convert the initial JSON body into a Kotlin map.
        val initialBody =
            initialResponse.body as Map<String, Any>?

        // Verify that the initial response exists.
        assertNotNull(initialBody)

        // Read the initial calories value as Number.
        val initialCalories =
            initialBody!!.getOrDefault("calories", 0) as Number

        // Verify that a fresh user starts with zero calories.
        assertEquals(
            0,
            initialCalories.toInt()
        )

        // Build a valid calories update URL.
        val putValidUrl =
            "/api/users/$username/calories?calories=1500"

        // Perform the valid calories update.
        val validResponse = restTemplate.exchange(
            putValidUrl,
            HttpMethod.PUT,
            null,
            Void::class.java
        )

        // Verify HTTP 204 No Content.
        assertEquals(
            HttpStatus.NO_CONTENT,
            validResponse.statusCode
        )

        // Read the calories value after the valid update.
        val afterValidResponse = restTemplate.getForEntity(
            getInitialUrl,
            Map::class.java
        )

        // Verify HTTP 200 OK.
        assertEquals(
            HttpStatus.OK,
            afterValidResponse.statusCode
        )

        // Convert the response body into a Kotlin map.
        val afterValidBody =
            afterValidResponse.body as Map<String, Any>?

        // Verify that the response exists.
        assertNotNull(afterValidBody)

        // Read the updated calories value.
        val afterValidCalories =
            afterValidBody!!.getOrDefault("calories", 0) as Number

        // Verify that the new value is exactly 1500.
        assertEquals(
            1500,
            afterValidCalories.toInt()
        )

        // Build a URL containing an invalid negative calories value.
        val putInvalidLowUrl =
            "/api/users/$username/calories?calories=-10"

        // Perform the invalid negative update.
        val invalidLowResponse = restTemplate.exchange(
            putInvalidLowUrl,
            HttpMethod.PUT,
            null,
            Void::class.java
        )

        // Verify HTTP 400 Bad Request.
        assertEquals(
            HttpStatus.BAD_REQUEST,
            invalidLowResponse.statusCode
        )

        // Build a URL containing an invalid excessively high value.
        val putInvalidHighUrl =
            "/api/users/$username/calories?calories=50000"

        // Perform the invalid high-value update.
        val invalidHighResponse = restTemplate.exchange(
            putInvalidHighUrl,
            HttpMethod.PUT,
            null,
            Void::class.java
        )

        // Verify HTTP 400 Bad Request.
        assertEquals(
            HttpStatus.BAD_REQUEST,
            invalidHighResponse.statusCode
        )

        // Read the calories again after both invalid requests.
        val afterInvalidResponse = restTemplate.getForEntity(
            getInitialUrl,
            Map::class.java
        )

        // Verify HTTP 200 OK.
        assertEquals(
            HttpStatus.OK,
            afterInvalidResponse.statusCode
        )

        // Convert the final JSON body into a Kotlin map.
        val afterInvalidBody =
            afterInvalidResponse.body as Map<String, Any>?

        // Verify that the response exists.
        assertNotNull(afterInvalidBody)

        // Read the calories value after the invalid updates.
        val afterInvalidCalories =
            afterInvalidBody!!.getOrDefault("calories", 0) as Number

        // Verify that the previous valid value was preserved.
        assertEquals(
            1500,
            afterInvalidCalories.toInt()
        )
    }
}