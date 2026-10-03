// Define the package that contains this instrumented test class.
package com.example.myfinaltopapplication
// Import JUnit runner support.
import org.junit.runner.RunWith
// Import Robolectric runner for local Android-aware JVM tests.
import org.robolectric.RobolectricTestRunner
// Import assertions for JUnit tests.
import org.junit.Assert.assertEquals
// Import assertFalse for use in this test file.
import org.junit.Assert.assertFalse
// Import assertNotNull for use in this test file.
import org.junit.Assert.assertNotNull
// Import assertNull for use in this test file.
import org.junit.Assert.assertNull
// Import assertTrue for use in this test file.
import org.junit.Assert.assertTrue
// Import Test for use in this test file.
import org.junit.Test
// Import JSON object for parsing and building JSON bodies.
import org.json.JSONObject
// Import collections for map-based responses.
import java.util.LinkedHashMap
// Import concurrency utilities for waiting on CompletableFuture.
import java.util.concurrent.CompletableFuture
// Import TimeUnit for use in this test file.
import java.util.concurrent.TimeUnit
// Import OkHttp client for building test client.
import okhttp3.OkHttpClient
// Import MockWebServer classes for mocking HTTP server behavior.
import okhttp3.mockwebserver.MockResponse
// Import MockWebServer for use in this test file.
import okhttp3.mockwebserver.MockWebServer
// Import RecordedRequest for use in this test file.
import okhttp3.mockwebserver.RecordedRequest
import org.junit.After
import org.junit.Before

/**
 * RestClientTest - integration-like unit tests for the Android RestClient class
 * using OkHttp's MockWebServer. Each test verifies both:
 *  1) The HTTP request that RestClient sends (method, path, body).
 *  2) The way RestClient parses and exposes the response via CompletableFuture.
 */

/*
 In these tests I use OkHttp’s MockWebServer to simulate the backend instead of
 calling the real Spring Boot server.

 MockWebServer returns predefined fake HTTP responses, while takeRequest(...)
 lets us inspect the request that RestClient actually sent.
*/


// Declare the RestClientTest test class.
@RunWith(RobolectricTestRunner::class)
class RestClientTest {
    // Hold a single MockWebServer instance for the current test.
    private lateinit var mockWebServer: MockWebServer

    // Hold the original OkHttpClient from RestClient so we can restore it after the test.
    private lateinit var originalClient: OkHttpClient

    // Open a companion object for constants shared by all tests.
    companion object {
        // Define a timeout in seconds for waiting on CompletableFuture results.
        private const val FUTURE_TIMEOUT_SECONDS = 5L
    }

    // -------------------------------------------------------------
    // Setup before each test.
    // -------------------------------------------------------------
    // Run the following setup function before every test.
    @Before
    fun setUp() {
        // Create a new MockWebServer instance.
        mockWebServer = MockWebServer()

        // Start the mock server so it begins listening on an available port.
        mockWebServer.start()

        // Log the URL of the mock server for debug purposes.
        println("TEST - MockWebServer started at: ${mockWebServer.url("/")}")

        // Use reflection to read the current OkHttpClient from RestClient.
        val clientField = RestClient::class.java.getDeclaredField("client")

        // Allow access to the private field.
        clientField.isAccessible = true

        // Save the original client so we can restore it later.
        originalClient = clientField.get(null) as OkHttpClient

        // Build a new OkHttpClient that redirects RestClient requests to MockWebServer.
        val testClient = OkHttpClient.Builder()
            // Add an interceptor that rewrites each outgoing request URL.
            .addInterceptor { chain ->
                // Capture the original outgoing request.
                val originalRequest = chain.request()

                // Extract the original URL from the request.
                val originalUrl = originalRequest.url

                /*
                 RestClient uses HTTPS in the real application, but MockWebServer
                 runs over HTTP by default.

                 Replace HTTPS with HTTP while keeping the original path and
                 query parameters unchanged.
                */
                // Start building a replacement URL based on the original URL.
                val newUrl = originalUrl.newBuilder()
                    // Change the URL scheme to HTTP because MockWebServer uses HTTP.
                    .scheme("http")
                    // Replace the original host with the MockWebServer host.
                    .host(mockWebServer.hostName)
                    // Replace the original port with the MockWebServer port.
                    .port(mockWebServer.port)
                    // Finish building the current OkHttp object.
                    .build()

                // Build a new request with the rewritten MockWebServer URL.
                val newRequest = originalRequest.newBuilder()
                    // Replace the original URL.
                    .url(newUrl)
                    // Finish building the request.
                    .build()

                // Send the request to MockWebServer.
                chain.proceed(newRequest)
            }
            // Finish building the test client.
            .build()

        // Replace the client in RestClient with the test client.
        clientField.set(null, testClient)
    }

    // -------------------------------------------------------------
    // Cleanup after each test.
    // -------------------------------------------------------------
    // Run the following cleanup function after every test.
    @After
    fun tearDown() {
        // Use reflection to get the client field in RestClient.
        val clientField = RestClient::class.java.getDeclaredField("client")

        // Allow access to the private field.
        clientField.isAccessible = true

        // Restore the original client only if setup initialized it successfully.
        if (::originalClient.isInitialized) {
            clientField.set(null, originalClient)
        }

        // Shut down MockWebServer only if setup initialized it successfully.
        if (::mockWebServer.isInitialized) {
            mockWebServer.shutdown()
        }
    }

    // -------------------------------------------------------------
    // Helper method: wait for a Boolean future with timeout.
    // -------------------------------------------------------------
    private fun awaitBoolean(future: CompletableFuture<Boolean>): Boolean {
        // Wait for the CompletableFuture to complete and return its Boolean value.
        return future.get(FUTURE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        // Close the current block.
    }

    // -------------------------------------------------------------
    // Helper method: wait for a nullable Double future with timeout.
    // -------------------------------------------------------------
    private fun awaitDouble(future: CompletableFuture<Double?>): Double? {
        // Wait for the CompletableFuture to complete and return its Double value.
        return future.get(FUTURE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        // Close the current block.
    }

    // -------------------------------------------------------------
    // Helper method: wait for a nullable Int future with timeout.
    // -------------------------------------------------------------
    private fun awaitInteger(future: CompletableFuture<Int?>): Int? {
        // Wait for the CompletableFuture to complete and return its Int value.
        return future.get(FUTURE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        // Close the current block.
    }

    // -------------------------------------------------------------
    // Helper method: wait for a nullable JSONObject future with timeout.
    // -------------------------------------------------------------
    private fun awaitJson(future: CompletableFuture<out JSONObject?>): JSONObject? {
        // Wait for the CompletableFuture to complete and return its JSONObject value.
        return future.get(FUTURE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        // Close the current block.
    }

    // -------------------------------------------------------------
    // Helper method: wait for a Map<String,Int> future with timeout.
    // -------------------------------------------------------------
    private fun awaitMap(future: CompletableFuture<Map<String, Int>>): Map<String, Int> {
        // Wait for the CompletableFuture to complete and return its Map value.
        return future.get(FUTURE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        // Close the current block.
    }

    // Helper method to drain any leftover requests from previous async calls.
    private fun drainRequests() {
        // Read requests until the queue is empty.
        while (true) {
            // Read the next HTTP request received by MockWebServer into leftover.
            val leftover = mockWebServer.takeRequest(100, TimeUnit.MILLISECONDS) ?: break
            // Print the current debug information to the test output.
            println("⚠️ Drained leftover request: ${leftover.method} ${leftover.path}")
            // Close the current block.
        }
        // Close the current block.
    }

    // =============================================================
    // TESTS FOR: register(User user)
    // =============================================================

    // Test that register returns true when server responds with 201 Created.
    // Mark the following function as a JUnit test.
    @Test
    // Declare the register_success_returnsTrueAndSendsCorrectBody test function.
    fun register_success_returnsTrueAndSendsCorrectBody() {
        // Enqueue a fake HTTP response with status 201 and success message.
        mockWebServer.enqueue(
            // Create a new fake HTTP response.
            MockResponse()
                // Set the fake HTTP response status code to 201.
                .setResponseCode(201)
                // Set the body returned by the fake HTTP response.
                .setBody("User created successfully")
            // Close the current function call.
        )

        // Create a sample User object with test data.
        val user = User("john", "1234", 25, "John Doe")

        // Call RestClient.register.
        val future = RestClient.register(user)

        // Wait for the result.
        val result = awaitBoolean(future)

        // Assert successful registration.
        assertTrue(result)

        // Drain the queue and keep the last request.
        var request: RecordedRequest? = null
        // Keep looping until the code explicitly breaks out of the loop.
        while (true) {
            // Read the next HTTP request received by MockWebServer into nextRequest.
            val nextRequest = mockWebServer.takeRequest(100, TimeUnit.MILLISECONDS) ?: break
            // Keep this request as the latest request captured from MockWebServer.
            request = nextRequest
            // Close the current block.
        }

        // Make sure we captured a request.
        assertNotNull("Expected at least one HTTP request", request)

        // Convert request to a non-null value after the null assertion.
        val capturedRequest = request!!

        // Print the current debug information to the test output.
        println("DEBUG register path = ${capturedRequest.path}")
        // Print the current debug information to the test output.
        println("DEBUG register method = ${capturedRequest.method}")

        // Assert that the HTTP method is POST or PUT.
        val method = capturedRequest.method
        // Assert that the tested condition is true.
        assertTrue(
            // Provide the string value used by the current expression.
            "Expected HTTP method POST or PUT but was: $method",
            // Execute this Kotlin statement as part of the current test flow.
            method == "POST" || method == "PUT"
            // Close the current function call.
        )

        // Assert that the request path matches the signup endpoint.
        assertEquals("/myapp/api/users/signup", capturedRequest.path)

        // Read and parse request body.
        val body = capturedRequest.body.readUtf8()
        // Parse the JSON text and store the JSONObject in obj.
        val obj = JSONObject(body)

        // Assert JSON data.
        assertEquals("john", obj.getString("userName"))
        // Assert that the actual value matches the expected value.
        assertEquals("1234", obj.getString("password"))
        // Assert that the actual value matches the expected value.
        assertEquals("John Doe", obj.getString("fullName"))
        // Assert that the actual value matches the expected value.
        assertEquals(25, obj.getInt("age"))
        // Close the current block.
    }

    // Test that register returns false when server responds with 409 Conflict.
    // Mark the following function as a JUnit test.
    @Test
    // Declare the register_conflict_returnsFalse test function.
    fun register_conflict_returnsFalse() {
        // Enqueue a fake HTTP response.
        mockWebServer.enqueue(
            // Create a new fake HTTP response.
            MockResponse()
                // Set the fake HTTP response status code to 409.
                .setResponseCode(409)
                // Set the body returned by the fake HTTP response.
                .setBody("Username already exists")
            // Close the current function call.
        )

        // Create duplicate user.
        val user = User("john", "1234", 25, "John Doe")

        // Call register.
        val future = RestClient.register(user)

        // Wait for result.
        val result = awaitBoolean(future)

        // Registration should fail.
        assertFalse(result)
        // Close the current block.
    }

    // =============================================================
    // TESTS FOR: login(String username, String password)
    // =============================================================

    // Test that login returns a User object when server responds with 200.
    // Mark the following function as a JUnit test.
    @Test
    // Declare the login_success_returnsUserObject test function.
    fun login_success_returnsUserObject() {
        // Build fake login response.
        val jsonBody =
            // Provide the string value used by the current expression.
            "{ \"token\":\"test-jwt-token\", \"userName\":\"john\", \"age\":25, \"fullName\":\"John Doe\" }"

        // Enqueue successful response.
        mockWebServer.enqueue(
            // Create a new fake HTTP response.
            MockResponse()
                // Set the fake HTTP response status code to 200.
                .setResponseCode(200)
                // Set the body returned by the fake HTTP response.
                .setBody(jsonBody)
                // Add the specified header to the fake HTTP response.
                .addHeader("Content-Type", "application/json")
            // Close the current function call.
        )

        // Call login.
        val future = RestClient.login("john", "1234")

        // Wait for User result.
        val user = future.get(FUTURE_TIMEOUT_SECONDS, TimeUnit.SECONDS)

        // Assert user exists.
        assertNotNull(user)

        // Assert user data.
        assertEquals("john", user!!.userName)
        // Assert that the actual value matches the expected value.
        assertEquals(25, user.age)
        // Assert that the actual value matches the expected value.
        assertEquals("John Doe", user.fullName)

        // Assert stored JWT.
        assertEquals("test-jwt-token", RestClient.getAuthToken())

        // Read request.
        val request = mockWebServer.takeRequest(FUTURE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        // Assert that the tested value is not null.
        assertNotNull(request)

        // Convert request to a non-null value after the null assertion.
        val capturedRequest = request!!

        // Assert request method and path.
        assertEquals("POST", capturedRequest.method)
        // Assert that the actual value matches the expected value.
        assertEquals("/myapp/api/users/login", capturedRequest.path)

        // Verify request body.
        val requestBody = capturedRequest.body.readUtf8()
        // Parse the JSON text and store the JSONObject in sent.
        val sent = JSONObject(requestBody)

        // Assert that the actual value matches the expected value.
        assertEquals("john", sent.getString("userName"))
        // Assert that the actual value matches the expected value.
        assertEquals("1234", sent.getString("password"))
        // Close the current block.
    }

    // Test unauthorized login.
    // Mark the following function as a JUnit test.
    @Test
    // Declare the login_unauthorized_sendsCorrectRequestAndReturnsNull test function.
    fun login_unauthorized_sendsCorrectRequestAndReturnsNull() {
        // Enqueue unauthorized response.
        mockWebServer.enqueue(
            // Create a new fake HTTP response.
            MockResponse()
                // Set the fake HTTP response status code to 401.
                .setResponseCode(401)
                // Set the body returned by the fake HTTP response.
                .setBody("Invalid username or password")
            // Close the current function call.
        )

        // Call login with wrong credentials.
        val future = RestClient.login("john", "wrong")

        // Wait for result.
        val user = future.get(FUTURE_TIMEOUT_SECONDS, TimeUnit.SECONDS)

        // Login should return null.
        assertNull(user)

        // Read request.
        val request = mockWebServer.takeRequest(FUTURE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        // Assert that the tested value is not null.
        assertNotNull(request)

        // Convert request to a non-null value after the null assertion.
        val capturedRequest = request!!

        // Assert method and path.
        assertEquals("POST", capturedRequest.method)
        // Assert that the actual value matches the expected value.
        assertEquals("/myapp/api/users/login", capturedRequest.path)

        // Verify request JSON.
        val body = capturedRequest.body.readUtf8()
        // Parse the JSON text and store the JSONObject in obj.
        val obj = JSONObject(body)

        // Assert that the actual value matches the expected value.
        assertEquals("john", obj.getString("userName"))
        // Assert that the actual value matches the expected value.
        assertEquals("wrong", obj.getString("password"))
        // Close the current block.
    }

    // =============================================================
    // TESTS FOR: patchUser(String username, Map<String,Object> updates)
    // =============================================================

    // Mark the following function as a JUnit test.
    @Test
    // Declare the patchUser_success_sendsPatchAndReturnsTrue test function.
    fun patchUser_success_sendsPatchAndReturnsTrue() {
        // Set authentication token.
        RestClient.setAuthToken("test-jwt-token")

        // Enqueue success response.
        mockWebServer.enqueue(
            // Create a new fake HTTP response.
            MockResponse()
                // Set the fake HTTP response status code to 200.
                .setResponseCode(200)
                // Set the body returned by the fake HTTP response.
                .setBody("{\"fullName\":\"Patched Name\"}")
                // Add the specified header to the fake HTTP response.
                .addHeader("Content-Type", "application/json")
            // Close the current function call.
        )

        // Create updates map.
        val updates = LinkedHashMap<String, Any>()
        // Add the new fullName value to the update map.
        updates["fullName"] = "Patched Name"

        // Call patchUser.
        val future = RestClient.patchUser("john", updates)

        // Wait for result.
        val result = awaitBoolean(future)

        // Assert success.
        assertTrue(result)

        // Read request.
        val request = mockWebServer.takeRequest(FUTURE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        // Assert that the tested value is not null.
        assertNotNull(request)

        // Convert request to a non-null value after the null assertion.
        val capturedRequest = request!!

        // Assert that the actual value matches the expected value.
        assertEquals("PATCH", capturedRequest.method)
        // Assert that the actual value matches the expected value.
        assertEquals("/myapp/api/users/john", capturedRequest.path)
        // Assert that the actual value matches the expected value.
        assertEquals("Bearer test-jwt-token", capturedRequest.getHeader("Authorization"))
        // Close the current block.
    }

    // =============================================================
    // TESTS FOR: deleteUser(String username)
    // =============================================================

    // Mark the following function as a JUnit test.
    @Test
    // Declare the deleteUser_success_sendsDeleteAndReturnsTrue test function.
    fun deleteUser_success_sendsDeleteAndReturnsTrue() {
        // Set authentication token.
        RestClient.setAuthToken("test-jwt-token")

        // Enqueue successful response.
        mockWebServer.enqueue(
            // Create a new fake HTTP response.
            MockResponse()
                // Set the fake HTTP response status code to 200.
                .setResponseCode(200)
                // Set the body returned by the fake HTTP response.
                .setBody("User deleted")
            // Close the current function call.
        )

        // Call deleteUser.
        val future = RestClient.deleteUser("john")

        // Wait for result.
        val result = awaitBoolean(future)

        // Assert success.
        assertTrue(result)

        // Read request.
        val request = mockWebServer.takeRequest(FUTURE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        // Assert that the tested value is not null.
        assertNotNull(request)

        // Convert request to a non-null value after the null assertion.
        val capturedRequest = request!!

        // Assert that the actual value matches the expected value.
        assertEquals("DELETE", capturedRequest.method)
        // Assert that the actual value matches the expected value.
        assertEquals("/myapp/api/users/john", capturedRequest.path)
        // Assert that the actual value matches the expected value.
        assertEquals("Bearer test-jwt-token", capturedRequest.getHeader("Authorization"))
        // Close the current block.
    }

    // =============================================================
    // TESTS FOR: headUser(String username)
    // =============================================================

    // Mark the following function as a JUnit test.
    @Test
    // Declare the headUser_success_sendsRequestAndReturnsTrue test function.
    fun headUser_success_sendsRequestAndReturnsTrue() {
        // Clear leftover requests.
        drainRequests()

        // Set authentication token.
        RestClient.setAuthToken("test-jwt-token")

        // Enqueue successful response.
        mockWebServer.enqueue(
            // Create a new fake HTTP response.
            MockResponse()
                // Set the fake HTTP response status code to 200.
                .setResponseCode(200)
            // Close the current function call.
        )

        // Call headUser.
        val future = RestClient.headUser("john")

        // Wait for result.
        val result = awaitBoolean(future)

        // Assert success.
        assertTrue(result)

        // Read request.
        val request = mockWebServer.takeRequest(FUTURE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        // Assert that the tested value is not null.
        assertNotNull(request)

        // Convert request to a non-null value after the null assertion.
        val capturedRequest = request!!

        // Print the current debug information to the test output.
        println("DEBUG headUser method = ${capturedRequest.method}")

        // Assert path and JWT.
        assertEquals("/myapp/api/users/john", capturedRequest.path)
        // Assert that the actual value matches the expected value.
        assertEquals("Bearer test-jwt-token", capturedRequest.getHeader("Authorization"))
        // Close the current block.
    }

    // =============================================================
    // TESTS FOR: updateBmi(String username, double bmi)
    // =============================================================

    // Mark the following function as a JUnit test.
    @Test
    // Declare the updateBmi_success_sendsRequestWithQueryAndReturnsTrue test function.
    fun updateBmi_success_sendsRequestWithQueryAndReturnsTrue() {
        // Clear leftover requests.
        drainRequests()

        // Set authentication token.
        RestClient.setAuthToken("test-jwt-token")

        // Enqueue successful response.
        mockWebServer.enqueue(
            // Create a new fake HTTP response.
            MockResponse()
                // Set the fake HTTP response status code to 200.
                .setResponseCode(200)
                // Set the body returned by the fake HTTP response.
                .setBody("BMI updated successfully")
            // Close the current function call.
        )

        // Call updateBmi.
        val future = RestClient.updateBmi("john", 23.5)

        // Wait for result.
        val result = awaitBoolean(future)

        // Assert success.
        assertTrue(result)

        // Read request.
        val request = mockWebServer.takeRequest(FUTURE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        // Assert that the tested value is not null.
        assertNotNull(request)

        // Convert request to a non-null value after the null assertion.
        val capturedRequest = request!!

        // Verify request.
        assertEquals("PATCH", capturedRequest.method)
        // Assert that the actual value matches the expected value.
        assertEquals("/myapp/api/users/john/bmi?bmi=23.5", capturedRequest.path)
        // Assert that the actual value matches the expected value.
        assertEquals("Bearer test-jwt-token", capturedRequest.getHeader("Authorization"))

        // Body should be empty.
        val body = capturedRequest.body.readUtf8()
        // Assert that the tested condition is true.
        assertTrue(
            // Provide the string value used by the current expression.
            "Expected empty body for updateBmi PATCH request",
            // Execute this Kotlin statement as part of the current test flow.
            body.isEmpty()
            // Close the current function call.
        )
        // Close the current block.
    }

    // =============================================================
    // TESTS FOR: getBmi(String username)
    // =============================================================

    // Mark the following function as a JUnit test.
    @Test
    // Declare the getBmi_success_returnsParsedDouble test function.
    fun getBmi_success_returnsParsedDouble() {
        // Clear leftover requests.
        drainRequests()

        // Set authentication token.
        RestClient.setAuthToken("test-jwt-token")

        // Enqueue BMI response.
        mockWebServer.enqueue(
            // Create a new fake HTTP response.
            MockResponse()
                // Set the fake HTTP response status code to 200.
                .setResponseCode(200)
                // Set the body returned by the fake HTTP response.
                .setBody("{\"userName\":\"john\",\"bmi\":22.7}")
                // Add the specified header to the fake HTTP response.
                .addHeader("Content-Type", "application/json")
            // Close the current function call.
        )

        // Call getBmi.
        val future = RestClient.getBmi("john")

        // Wait for result.
        val bmi = awaitDouble(future)

        // Assert BMI exists and has correct value.
        assertNotNull(bmi)
        // Assert that the actual value matches the expected value.
        assertEquals(22.7, bmi!!, 0.0001)

        // Read request.
        val request = mockWebServer.takeRequest(FUTURE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        // Assert that the tested value is not null.
        assertNotNull(request)

        // Convert request to a non-null value after the null assertion.
        val capturedRequest = request!!

        // Assert that the actual value matches the expected value.
        assertEquals("GET", capturedRequest.method)
        // Assert that the actual value matches the expected value.
        assertEquals("/myapp/api/users/john", capturedRequest.path)
        // Assert that the actual value matches the expected value.
        assertEquals("Bearer test-jwt-token", capturedRequest.getHeader("Authorization"))
        // Close the current block.
    }

    // =============================================================
    // TESTS FOR: updateWater(String username, int amount)
    // =============================================================

    // Mark the following function as a JUnit test.
    @Test
    // Declare the updateWater_success_sendsRequestWithAmountAndReturnsTrue test function.
    fun updateWater_success_sendsRequestWithAmountAndReturnsTrue() {
        // Clear leftover requests.
        drainRequests()

        // Set authentication token.
        RestClient.setAuthToken("test-jwt-token")

        // Enqueue successful response.
        mockWebServer.enqueue(
            // Create a new fake HTTP response.
            MockResponse()
                // Set the fake HTTP response status code to 200.
                .setResponseCode(200)
                // Set the body returned by the fake HTTP response.
                .setBody("Water updated successfully")
            // Close the current function call.
        )

        // Call updateWater.
        val future = RestClient.updateWater("john", 400)

        // Wait for result.
        val result = awaitBoolean(future)

        // Assert success.
        assertTrue(result)

        // Read request.
        val request = mockWebServer.takeRequest(FUTURE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        // Assert that the tested value is not null.
        assertNotNull(request)

        // Convert request to a non-null value after the null assertion.
        val capturedRequest = request!!

        // Assert that the actual value matches the expected value.
        assertEquals("PATCH", capturedRequest.method)
        // Assert that the actual value matches the expected value.
        assertEquals("/myapp/api/users/john/water?amount=400", capturedRequest.path)
        // Assert that the actual value matches the expected value.
        assertEquals("Bearer test-jwt-token", capturedRequest.getHeader("Authorization"))

        // Body should be empty.
        val body = capturedRequest.body.readUtf8()
        // Assert that the tested condition is true.
        assertTrue(
            // Provide the string value used by the current expression.
            "Expected empty body for updateWater PATCH request",
            // Execute this Kotlin statement as part of the current test flow.
            body.isEmpty()
            // Close the current function call.
        )
        // Close the current block.
    }

    // =============================================================
    // TESTS FOR: getWater(String username)
    // =============================================================

    // Mark the following function as a JUnit test.
    @Test
    // Declare the getWater_success_returnsJsonObject test function.
    fun getWater_success_returnsJsonObject() {
        // Clear leftover requests.
        drainRequests()

        // Set authentication token.
        RestClient.setAuthToken("test-jwt-token")

        // Enqueue water response.
        mockWebServer.enqueue(
            // Create a new fake HTTP response.
            MockResponse()
                // Set the fake HTTP response status code to 200.
                .setResponseCode(200)
                // Set the body returned by the fake HTTP response.
                .setBody("{\"todayWater\":1200,\"yesterdayWater\":800}")
                // Add the specified header to the fake HTTP response.
                .addHeader("Content-Type", "application/json")
            // Close the current function call.
        )

        // Call getWater.
        val future = RestClient.getWater("john")

        // Wait for JSON.
        val obj = awaitJson(future)

        // Assert object exists.
        assertNotNull(obj)

        // Assert water totals.
        assertEquals(1200, obj!!.getInt("todayWater"))
        // Assert that the actual value matches the expected value.
        assertEquals(800, obj.getInt("yesterdayWater"))

        // Read request.
        val request = mockWebServer.takeRequest(FUTURE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        // Assert that the tested value is not null.
        assertNotNull(request)

        // Convert request to a non-null value after the null assertion.
        val capturedRequest = request!!

        // Assert that the actual value matches the expected value.
        assertEquals("GET", capturedRequest.method)
        // Assert that the actual value matches the expected value.
        assertEquals("/myapp/api/users/john/water", capturedRequest.path)
        // Assert that the actual value matches the expected value.
        assertEquals("Bearer test-jwt-token", capturedRequest.getHeader("Authorization"))

        // GET body should be empty.
        val body = capturedRequest.body.readUtf8()
        // Assert that the tested condition is true.
        assertTrue(
            // Provide the string value used by the current expression.
            "Expected empty body for getWater GET request",
            // Execute this Kotlin statement as part of the current test flow.
            body.isEmpty()
            // Close the current function call.
        )
        // Close the current block.
    }

    // =============================================================
    // TESTS FOR: getWaterHistoryMap(String username, int days)
    // =============================================================

    // Mark the following function as a JUnit test.
    @Test
    // Declare the getWaterHistoryMap_success_returnsJsonWithDates test function.
    fun getWaterHistoryMap_success_returnsJsonWithDates() {
        // Clear leftover requests.
        drainRequests()

        // Set authentication token.
        RestClient.setAuthToken("test-jwt-token")

        // Build fake history response.
        val body = "{ \"2025-09-29\": 1200, \"2025-09-28\": 2000 }"

        // Enqueue response.
        mockWebServer.enqueue(
            // Create a new fake HTTP response.
            MockResponse()
                // Set the fake HTTP response status code to 200.
                .setResponseCode(200)
                // Set the body returned by the fake HTTP response.
                .setBody(body)
                // Add the specified header to the fake HTTP response.
                .addHeader("Content-Type", "application/json")
            // Close the current function call.
        )

        // Call getWaterHistoryMap.
        val future = RestClient.getWaterHistoryMap("john", 2)

        // Wait for JSON.
        val obj = awaitJson(future)

        // Assert object exists.
        assertNotNull(obj)

        // Assert history values.
        assertEquals(1200, obj!!.getInt("2025-09-29"))
        // Assert that the actual value matches the expected value.
        assertEquals(2000, obj.getInt("2025-09-28"))

        // Read request.
        val request = mockWebServer.takeRequest(FUTURE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        // Assert that the tested value is not null.
        assertNotNull(request)

        // Convert request to a non-null value after the null assertion.
        val capturedRequest = request!!

        // Assert that the actual value matches the expected value.
        assertEquals("GET", capturedRequest.method)
        // Assert that the actual value matches the expected value.
        assertEquals("/myapp/api/users/john/waterHistoryMap?days=2", capturedRequest.path)
        // Assert that the actual value matches the expected value.
        assertEquals("Bearer test-jwt-token", capturedRequest.getHeader("Authorization"))

        // GET body should be empty.
        val reqBody = capturedRequest.body.readUtf8()
        // Assert that the tested condition is true.
        assertTrue(
            // Provide the string value used by the current expression.
            "Expected empty body for getWaterHistoryMap GET request",
            // Execute this Kotlin statement as part of the current test flow.
            reqBody.isEmpty()
            // Close the current function call.
        )
        // Close the current block.
    }

    // =============================================================
    // TESTS FOR: getWeeklyAverages(String username)
    // =============================================================

    // Mark the following function as a JUnit test.
    @Test
    // Declare the getWeeklyAverages_success_returnsMap test function.
    fun getWeeklyAverages_success_returnsMap() {
        // Clear leftover requests.
        drainRequests()

        // Set authentication token.
        RestClient.setAuthToken("test-jwt-token")

        // Build fake weekly averages response.
        val body =
            // Provide the string value used by the current expression.
            "{ \"Week 1\": 1000, \"Week 2\": 1500, \"Week 3\": 2000, \"Week 4\": 2500 }"

        // Enqueue response.
        mockWebServer.enqueue(
            // Create a new fake HTTP response.
            MockResponse()
                // Set the fake HTTP response status code to 200.
                .setResponseCode(200)
                // Set the body returned by the fake HTTP response.
                .setBody(body)
                // Add the specified header to the fake HTTP response.
                .addHeader("Content-Type", "application/json")
            // Close the current function call.
        )

        // Call getWeeklyAverages.
        val future = RestClient.getWeeklyAverages("john")

        // Wait for map.
        val map = awaitMap(future)

        // Assert map values.
        assertNotNull(map)
        // Assert that the actual value matches the expected value.
        assertEquals(4, map.size)
        // Assert that the actual value matches the expected value.
        assertEquals(1000, map["Week 1"])
        // Assert that the actual value matches the expected value.
        assertEquals(1500, map["Week 2"])
        // Assert that the actual value matches the expected value.
        assertEquals(2000, map["Week 3"])
        // Assert that the actual value matches the expected value.
        assertEquals(2500, map["Week 4"])

        // Read request.
        val request = mockWebServer.takeRequest(FUTURE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        // Assert that the tested value is not null.
        assertNotNull(request)

        // Convert request to a non-null value after the null assertion.
        val capturedRequest = request!!

        // Assert that the actual value matches the expected value.
        assertEquals("GET", capturedRequest.method)
        // Assert that the actual value matches the expected value.
        assertEquals("/myapp/api/users/john/weeklyAverages", capturedRequest.path)
        // Assert that the actual value matches the expected value.
        assertEquals("Bearer test-jwt-token", capturedRequest.getHeader("Authorization"))

        // GET body should be empty.
        val reqBody = capturedRequest.body.readUtf8()
        // Assert that the tested condition is true.
        assertTrue(
            // Provide the string value used by the current expression.
            "Expected empty body for getWeeklyAverages GET request",
            // Execute this Kotlin statement as part of the current test flow.
            reqBody.isEmpty()
            // Close the current function call.
        )
        // Close the current block.
    }

    // =============================================================
    // TESTS FOR: getGoal(String username) and setGoal(String username,int)
    // =============================================================

    // Mark the following function as a JUnit test.
    @Test
    // Declare the goal_setAndGet_flowWorks test function.
    fun goal_setAndGet_flowWorks() {
        // Clear leftover requests.
        drainRequests()

        // Set authentication token.
        RestClient.setAuthToken("test-jwt-token")

        // Enqueue response for setGoal.
        mockWebServer.enqueue(
            // Create a new fake HTTP response.
            MockResponse()
                // Set the fake HTTP response status code to 200.
                .setResponseCode(200)
                // Set the body returned by the fake HTTP response.
                .setBody("{\"status\":\"OK\"}")
                // Add the specified header to the fake HTTP response.
                .addHeader("Content-Type", "application/json")
            // Close the current function call.
        )

        // Enqueue response for getGoal.
        mockWebServer.enqueue(
            // Create a new fake HTTP response.
            MockResponse()
                // Set the fake HTTP response status code to 200.
                .setResponseCode(200)
                // Set the body returned by the fake HTTP response.
                .setBody("{\"goalMl\":3400}")
                // Add the specified header to the fake HTTP response.
                .addHeader("Content-Type", "application/json")
            // Close the current function call.
        )

        // --------- 1) setGoal REQUEST + RESPONSE ---------

        // Call setGoal.
        val setFuture = RestClient.setGoal("john", 3400)

        // Wait for result.
        val setResult = awaitBoolean(setFuture)

        // Assert success.
        assertTrue(setResult)

        // Read setGoal request.
        val setRequest = mockWebServer.takeRequest(FUTURE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        // Assert that the tested value is not null.
        assertNotNull(setRequest)

        // Convert setRequest to a non-null value after the null assertion.
        val capturedSetRequest = setRequest!!

        // Assert that the actual value matches the expected value.
        assertEquals("PUT", capturedSetRequest.method)
        // Assert that the actual value matches the expected value.
        assertEquals("/myapp/api/users/john/goal?goalMl=3400", capturedSetRequest.path)
        // Assert that the actual value matches the expected value.
        assertEquals("Bearer test-jwt-token", capturedSetRequest.getHeader("Authorization"))

        // Body should be empty.
        val setBody = capturedSetRequest.body.readUtf8()
        // Assert that the tested condition is true.
        assertTrue(
            // Provide the string value used by the current expression.
            "Expected empty body for setGoal PUT request",
            // Execute this Kotlin statement as part of the current test flow.
            setBody.isEmpty()
            // Close the current function call.
        )

        // --------- 2) getGoal REQUEST + RESPONSE ---------

        // Call getGoal.
        val getFuture = RestClient.getGoal("john")

        // Wait for result.
        val goalObj = awaitJson(getFuture)

        // Assert goal JSON.
        assertNotNull(goalObj)
        // Assert that the actual value matches the expected value.
        assertEquals(3400, goalObj!!.getInt("goalMl"))

        // Read getGoal request.
        val getRequest = mockWebServer.takeRequest(FUTURE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        // Assert that the tested value is not null.
        assertNotNull(getRequest)

        // Convert getRequest to a non-null value after the null assertion.
        val capturedGetRequest = getRequest!!

        // Assert that the actual value matches the expected value.
        assertEquals("GET", capturedGetRequest.method)
        // Assert that the actual value matches the expected value.
        assertEquals("/myapp/api/users/john/goal", capturedGetRequest.path)
        // Assert that the actual value matches the expected value.
        assertEquals("Bearer test-jwt-token", capturedGetRequest.getHeader("Authorization"))

        // GET body should be empty.
        val getBody = capturedGetRequest.body.readUtf8()
        // Assert that the tested condition is true.
        assertTrue(
            // Provide the string value used by the current expression.
            "Expected empty body for getGoal GET request",
            // Execute this Kotlin statement as part of the current test flow.
            getBody.isEmpty()
            // Close the current function call.
        )
        // Close the current block.
    }

    // =============================================================
    // TESTS FOR: getBmiDistribution()
    // =============================================================

    // Mark the following function as a JUnit test.
    @Test
    // Declare the getBmiDistribution_success_returnsJson test function.
    fun getBmiDistribution_success_returnsJson() {
        // Clear leftover requests.
        drainRequests()

        // Build fake distribution.
        val body =
            // Provide the string value used by the current expression.
            "{ \"Underweight\": 2, \"Normal\": 5, \"Overweight\": 3, \"Obese\": 1 }"

        // Enqueue response.
        mockWebServer.enqueue(
            // Create a new fake HTTP response.
            MockResponse()
                // Set the fake HTTP response status code to 200.
                .setResponseCode(200)
                // Set the body returned by the fake HTTP response.
                .setBody(body)
                // Add the specified header to the fake HTTP response.
                .addHeader("Content-Type", "application/json")
            // Close the current function call.
        )

        // Call getBmiDistribution.
        val future = RestClient.getBmiDistribution()

        // Wait for JSON.
        val obj = awaitJson(future)

        // Assert response.
        assertNotNull(obj)
        // Assert that the actual value matches the expected value.
        assertEquals(5, obj!!.getInt("Normal"))
        // Assert that the actual value matches the expected value.
        assertEquals(3, obj.getInt("Overweight"))

        // Read request.
        val request = mockWebServer.takeRequest(FUTURE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        // Assert that the tested value is not null.
        assertNotNull(request)

        // Convert request to a non-null value after the null assertion.
        val capturedRequest = request!!

        // Assert that the actual value matches the expected value.
        assertEquals("GET", capturedRequest.method)
        // Assert that the actual value matches the expected value.
        assertEquals("/myapp/api/users/stats/bmiDistribution", capturedRequest.path)

        // GET body should be empty.
        val reqBody = capturedRequest.body.readUtf8()
        // Assert that the tested condition is true.
        assertTrue(
            // Provide the string value used by the current expression.
            "Expected empty body for getBmiDistribution GET request",
            // Execute this Kotlin statement as part of the current test flow.
            reqBody.isEmpty()
            // Close the current function call.
        )
        // Close the current block.
    }

    // =============================================================
    // TESTS FOR: getCalories(String username) and setCalories(...)
    // =============================================================

    // Mark the following function as a JUnit test.
    @Test
    // Declare the calories_setAndGet_flowWorks test function.
    fun calories_setAndGet_flowWorks() {
        // Clear leftover requests.
        drainRequests()

        // Set authentication token.
        RestClient.setAuthToken("test-jwt-token")

        // Enqueue initial GET response.
        mockWebServer.enqueue(
            // Create a new fake HTTP response.
            MockResponse()
                // Set the fake HTTP response status code to 200.
                .setResponseCode(200)
                // Set the body returned by the fake HTTP response.
                .setBody("{\"calories\":0}")
                // Add the specified header to the fake HTTP response.
                .addHeader("Content-Type", "application/json")
            // Close the current function call.
        )

        // Enqueue PUT response.
        mockWebServer.enqueue(
            // Create a new fake HTTP response.
            MockResponse()
                // Set the fake HTTP response status code to 204.
                .setResponseCode(204)
            // Close the current function call.
        )

        // Enqueue second GET response.
        mockWebServer.enqueue(
            // Create a new fake HTTP response.
            MockResponse()
                // Set the fake HTTP response status code to 200.
                .setResponseCode(200)
                // Set the body returned by the fake HTTP response.
                .setBody("{\"calories\":1500}")
                // Add the specified header to the fake HTTP response.
                .addHeader("Content-Type", "application/json")
            // Close the current function call.
        )

        // --------- 1) FIRST GET /calories ---------

        // Call RestClient.getCalories and store its asynchronous result in getInitialFuture.
        val getInitialFuture = RestClient.getCalories("john")

        // Wait for getInitialFuture to complete and store the result in initial.
        val initial = awaitInteger(getInitialFuture)

        // Assert that the tested value is not null.
        assertNotNull(initial)
        // Assert that the actual value matches the expected value.
        assertEquals(0, initial)

        // Read the next HTTP request received by MockWebServer into firstGet.
        val firstGet = mockWebServer.takeRequest(FUTURE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        // Assert that the tested value is not null.
        assertNotNull(firstGet)

        // Convert firstGet to a non-null value after the null assertion.
        val capturedFirstGet = firstGet!!

        // Assert that the actual value matches the expected value.
        assertEquals("GET", capturedFirstGet.method)
        // Assert that the actual value matches the expected value.
        assertEquals("/myapp/api/users/john/calories", capturedFirstGet.path)
        // Assert that the actual value matches the expected value.
        assertEquals("Bearer test-jwt-token", capturedFirstGet.getHeader("Authorization"))

        // Read the HTTP request body as UTF-8 text into firstGetBody.
        val firstGetBody = capturedFirstGet.body.readUtf8()
        // Assert that the tested condition is true.
        assertTrue(
            // Provide the string value used by the current expression.
            "Expected empty body for first getCalories GET request",
            // Execute this Kotlin statement as part of the current test flow.
            firstGetBody.isEmpty()
            // Close the current function call.
        )

        // --------- 2) PUT /calories?calories=1500 ---------

        // Call RestClient.setCalories and store its asynchronous result in setFuture.
        val setFuture = RestClient.setCalories("john", 1500)

        // Wait for setFuture to complete and store the result in setResult.
        val setResult = awaitBoolean(setFuture)

        // Assert that the tested condition is true.
        assertTrue(setResult)

        // Read the next HTTP request received by MockWebServer into putReq.
        val putReq = mockWebServer.takeRequest(FUTURE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        // Assert that the tested value is not null.
        assertNotNull(putReq)

        // Convert putReq to a non-null value after the null assertion.
        val capturedPutReq = putReq!!

        // Assert that the actual value matches the expected value.
        assertEquals("PUT", capturedPutReq.method)
        // Assert that the actual value matches the expected value.
        assertEquals("/myapp/api/users/john/calories?calories=1500", capturedPutReq.path)
        // Assert that the actual value matches the expected value.
        assertEquals("Bearer test-jwt-token", capturedPutReq.getHeader("Authorization"))

        // Read the HTTP request body as UTF-8 text into putBody.
        val putBody = capturedPutReq.body.readUtf8()
        // Assert that the tested condition is true.
        assertTrue(
            // Provide the string value used by the current expression.
            "Expected empty body for setCalories PUT request",
            // Execute this Kotlin statement as part of the current test flow.
            putBody.isEmpty()
            // Close the current function call.
        )

        // --------- 3) SECOND GET /calories ---------

        // Call RestClient.getCalories and store its asynchronous result in getAfterFuture.
        val getAfterFuture = RestClient.getCalories("john")

        // Wait for getAfterFuture to complete and store the result in updated.
        val updated = awaitInteger(getAfterFuture)

        // Assert that the tested value is not null.
        assertNotNull(updated)
        // Assert that the actual value matches the expected value.
        assertEquals(1500, updated)

        // Read the next HTTP request received by MockWebServer into secondGet.
        val secondGet = mockWebServer.takeRequest(FUTURE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        // Assert that the tested value is not null.
        assertNotNull(secondGet)

        // Convert secondGet to a non-null value after the null assertion.
        val capturedSecondGet = secondGet!!

        // Assert that the actual value matches the expected value.
        assertEquals("GET", capturedSecondGet.method)
        // Assert that the actual value matches the expected value.
        assertEquals("/myapp/api/users/john/calories", capturedSecondGet.path)
        // Assert that the actual value matches the expected value.
        assertEquals("Bearer test-jwt-token", capturedSecondGet.getHeader("Authorization"))

        // Read the HTTP request body as UTF-8 text into secondGetBody.
        val secondGetBody = capturedSecondGet.body.readUtf8()
        // Assert that the tested condition is true.
        assertTrue(
            // Provide the string value used by the current expression.
            "Expected empty body for second getCalories GET request",
            // Execute this Kotlin statement as part of the current test flow.
            secondGetBody.isEmpty()
            // Close the current function call.
        )
        // Close the current block.
    }

    // =============================================================
    // FAILURE TESTS FOR: register(User user)
    // =============================================================

    // Mark the following function as a JUnit test.
    @Test
    // Declare the register_serverError_returnsFalse test function.
    fun register_serverError_returnsFalse() {
        // Clear leftover requests.
        drainRequests()

        // Enqueue server error.
        mockWebServer.enqueue(
            // Create a new fake HTTP response.
            MockResponse()
                // Set the fake HTTP response status code to 500.
                .setResponseCode(500)
                // Set the body returned by the fake HTTP response.
                .setBody("Internal error")
            // Close the current function call.
        )

        // Create test user.
        val user = User("john", "1234", 25, "John Doe")

        // Call register.
        val future = RestClient.register(user)

        // Wait for result.
        val result = awaitBoolean(future)

        // Assert failure.
        assertFalse(result)

        // Read request.
        val request = mockWebServer.takeRequest(FUTURE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        // Assert that the tested value is not null.
        assertNotNull(request)

        // Convert request to a non-null value after the null assertion.
        val capturedRequest = request!!

        // Assert that the actual value matches the expected value.
        assertEquals("POST", capturedRequest.method)
        // Assert that the actual value matches the expected value.
        assertEquals("/myapp/api/users/signup", capturedRequest.path)

        // Verify request JSON.
        val body = capturedRequest.body.readUtf8()
        // Parse the JSON text and store the JSONObject in obj.
        val obj = JSONObject(body)

        // Assert that the actual value matches the expected value.
        assertEquals("john", obj.getString("userName"))
        // Assert that the actual value matches the expected value.
        assertEquals("1234", obj.getString("password"))
        // Assert that the actual value matches the expected value.
        assertEquals("John Doe", obj.getString("fullName"))
        // Assert that the actual value matches the expected value.
        assertEquals(25, obj.getInt("age"))
        // Close the current block.
    }

    // =============================================================
    // FAILURE TESTS FOR: login(String username, String password)
    // =============================================================

    // Mark the following function as a JUnit test.
    @Test
    // Declare the login_notFound_returnsNull test function.
    fun login_notFound_returnsNull() {
        // Clear leftover requests.
        drainRequests()

        // Enqueue 404 response.
        mockWebServer.enqueue(
            // Create a new fake HTTP response.
            MockResponse()
                // Set the fake HTTP response status code to 404.
                .setResponseCode(404)
                // Set the body returned by the fake HTTP response.
                .setBody("User not found")
            // Close the current function call.
        )

        // Call login.
        val future = RestClient.login("ghost", "pwd")

        // Wait for User result.
        val user = future.get(FUTURE_TIMEOUT_SECONDS, TimeUnit.SECONDS)

        // Assert null.
        assertNull(user)

        // Read request.
        val request = mockWebServer.takeRequest(FUTURE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        // Assert that the tested value is not null.
        assertNotNull(request)

        // Convert request to a non-null value after the null assertion.
        val capturedRequest = request!!

        // Assert that the actual value matches the expected value.
        assertEquals("POST", capturedRequest.method)
        // Assert that the actual value matches the expected value.
        assertEquals("/myapp/api/users/login", capturedRequest.path)

        // Check JSON body.
        val body = capturedRequest.body.readUtf8()
        // Parse the JSON text and store the JSONObject in obj.
        val obj = JSONObject(body)

        // Assert that the actual value matches the expected value.
        assertEquals("ghost", obj.getString("userName"))
        // Assert that the actual value matches the expected value.
        assertEquals("pwd", obj.getString("password"))
        // Close the current block.
    }

    // =============================================================
    // FAILURE TESTS FOR: patchUser(String username, Map<String,Object> updates)
    // =============================================================

    // Mark the following function as a JUnit test.
    @Test
    // Declare the patchUser_notFound_returnsFalse test function.
    fun patchUser_notFound_returnsFalse() {
        // Clear leftover requests.
        drainRequests()

        // Set authentication token.
        RestClient.setAuthToken("test-jwt-token")

        // Enqueue 404 response.
        mockWebServer.enqueue(
            // Create a new fake HTTP response.
            MockResponse()
                // Set the fake HTTP response status code to 404.
                .setResponseCode(404)
                // Set the body returned by the fake HTTP response.
                .setBody("User not found")
            // Close the current function call.
        )

        // Create updates map.
        val updates = LinkedHashMap<String, Any>()
        // Add the new fullName value to the update map.
        updates["fullName"] = "No One"

        // Call patchUser.
        val future = RestClient.patchUser("ghost", updates)

        // Wait for result.
        val result = awaitBoolean(future)

        // Assert failure.
        assertFalse(result)

        // Read request.
        val request = mockWebServer.takeRequest(FUTURE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        // Assert that the tested value is not null.
        assertNotNull(request)

        // Convert request to a non-null value after the null assertion.
        val capturedRequest = request!!

        // Assert that the actual value matches the expected value.
        assertEquals("PATCH", capturedRequest.method)
        // Assert that the actual value matches the expected value.
        assertEquals("/myapp/api/users/ghost", capturedRequest.path)
        // Assert that the actual value matches the expected value.
        assertEquals("Bearer test-jwt-token", capturedRequest.getHeader("Authorization"))

        // Verify body.
        val body = capturedRequest.body.readUtf8()
        // Parse the JSON text and store the JSONObject in obj.
        val obj = JSONObject(body)

        // Assert that the actual value matches the expected value.
        assertEquals("No One", obj.getString("fullName"))
        // Assert that the actual value matches the expected value.
        assertEquals(1, obj.length())
        // Close the current block.
    }

    // =============================================================
    // FAILURE TESTS FOR: deleteUser(String username)
    // =============================================================

    // Mark the following function as a JUnit test.
    @Test
    // Declare the deleteUser_notFound_returnsFalse test function.
    fun deleteUser_notFound_returnsFalse() {
        // Clear leftover requests.
        drainRequests()

        // Set authentication token.
        RestClient.setAuthToken("test-jwt-token")

        // Enqueue 404 response.
        mockWebServer.enqueue(
            // Create a new fake HTTP response.
            MockResponse()
                // Set the fake HTTP response status code to 404.
                .setResponseCode(404)
                // Set the body returned by the fake HTTP response.
                .setBody("User not found")
            // Close the current function call.
        )

        // Call deleteUser.
        val future = RestClient.deleteUser("ghost")

        // Wait for result.
        val result = awaitBoolean(future)

        // Assert failure.
        assertFalse(result)

        // Read request.
        val request = mockWebServer.takeRequest(FUTURE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        // Assert that the tested value is not null.
        assertNotNull(request)

        // Convert request to a non-null value after the null assertion.
        val capturedRequest = request!!

        // Assert that the actual value matches the expected value.
        assertEquals("DELETE", capturedRequest.method)
        // Assert that the actual value matches the expected value.
        assertEquals("/myapp/api/users/ghost", capturedRequest.path)
        // Assert that the actual value matches the expected value.
        assertEquals("Bearer test-jwt-token", capturedRequest.getHeader("Authorization"))

        // DELETE body should be empty.
        val body = capturedRequest.body.readUtf8()
        // Assert that the tested condition is true.
        assertTrue(
            // Provide the string value used by the current expression.
            "Expected empty body for deleteUser DELETE request",
            // Execute this Kotlin statement as part of the current test flow.
            body.isEmpty()
            // Close the current function call.
        )
        // Close the current block.
    }

    // =============================================================
    // FAILURE TESTS FOR: headUser(String username)
    // =============================================================

    // Mark the following function as a JUnit test.
    @Test
    // Declare the headUser_notFound_returnsFalse test function.
    fun headUser_notFound_returnsFalse() {
        // Clear leftover requests.
        drainRequests()

        // Set authentication token.
        RestClient.setAuthToken("test-jwt-token")

        // Enqueue 404 response.
        mockWebServer.enqueue(
            // Create a new fake HTTP response.
            MockResponse()
                // Set the fake HTTP response status code to 404.
                .setResponseCode(404)
            // Close the current function call.
        )

        // Call headUser.
        val future = RestClient.headUser("ghost")

        // Wait for result.
        val result = awaitBoolean(future)

        // Assert failure.
        assertFalse(result)

        // Read request.
        val request = mockWebServer.takeRequest(FUTURE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        // Assert that the tested value is not null.
        assertNotNull(request)

        // Convert request to a non-null value after the null assertion.
        val capturedRequest = request!!

        // Assert that the actual value matches the expected value.
        assertEquals("HEAD", capturedRequest.method)
        // Assert that the actual value matches the expected value.
        assertEquals("/myapp/api/users/ghost", capturedRequest.path)
        // Assert that the actual value matches the expected value.
        assertEquals("Bearer test-jwt-token", capturedRequest.getHeader("Authorization"))

        // HEAD should not send body.
        val body = capturedRequest.body.readUtf8()
        // Assert that the tested condition is true.
        assertTrue(body.isEmpty())
        // Close the current block.
    }

    // =============================================================
    // FAILURE TESTS FOR: updateBmi(String username, double bmi)
    // =============================================================

    // Mark the following function as a JUnit test.
    @Test
    // Declare the updateBmi_notFound_returnsFalse test function.
    fun updateBmi_notFound_returnsFalse() {
        // Clear leftover requests.
        drainRequests()

        // Set authentication token.
        RestClient.setAuthToken("test-jwt-token")

        // Enqueue 404 response.
        mockWebServer.enqueue(
            // Create a new fake HTTP response.
            MockResponse()
                // Set the fake HTTP response status code to 404.
                .setResponseCode(404)
                // Set the body returned by the fake HTTP response.
                .setBody("User not found")
            // Close the current function call.
        )

        // Call updateBmi.
        val future = RestClient.updateBmi("ghost", 21.5)

        // Wait for result.
        val result = awaitBoolean(future)

        // Assert failure.
        assertFalse(result)

        // Read request.
        val request = mockWebServer.takeRequest(FUTURE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        // Assert that the tested value is not null.
        assertNotNull(request)

        // Convert request to a non-null value after the null assertion.
        val capturedRequest = request!!

        // Allow PATCH or POST like the original test.
        val method = capturedRequest.method
        // Assert that the tested condition is true.
        assertTrue(
            // Provide the string value used by the current expression.
            "Expected HTTP method PATCH or POST but was: $method",
            // Execute this Kotlin statement as part of the current test flow.
            method == "PATCH" || method == "POST"
            // Close the current function call.
        )

        // Assert that the actual value matches the expected value.
        assertEquals("/myapp/api/users/ghost/bmi?bmi=21.5", capturedRequest.path)
        // Assert that the actual value matches the expected value.
        assertEquals("Bearer test-jwt-token", capturedRequest.getHeader("Authorization"))

        // Body should be empty.
        val body = capturedRequest.body.readUtf8()
        // Assert that the tested condition is true.
        assertTrue(body.isEmpty())
        // Close the current block.
    }

    // =============================================================
    // FAILURE TESTS FOR: getBmi(String username)
    // =============================================================

    // Mark the following function as a JUnit test.
    @Test
    // Declare the getBmi_notFound_returnsNull test function.
    fun getBmi_notFound_returnsNull() {
        // Clear leftover requests.
        drainRequests()

        // Set authentication token.
        RestClient.setAuthToken("test-jwt-token")

        // Enqueue 404 response.
        mockWebServer.enqueue(
            // Create a new fake HTTP response.
            MockResponse()
                // Set the fake HTTP response status code to 404.
                .setResponseCode(404)
                // Set the body returned by the fake HTTP response.
                .setBody("User not found")
            // Close the current function call.
        )

        // Call getBmi.
        val future = RestClient.getBmi("ghost")

        // Wait for result.
        val bmi = awaitDouble(future)

        // Assert null.
        assertNull(bmi)

        // Read request.
        val request = mockWebServer.takeRequest(FUTURE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        // Assert that the tested value is not null.
        assertNotNull(request)

        // Convert request to a non-null value after the null assertion.
        val capturedRequest = request!!

        // Assert that the actual value matches the expected value.
        assertEquals("GET", capturedRequest.method)
        // Assert that the actual value matches the expected value.
        assertEquals("/myapp/api/users/ghost", capturedRequest.path)
        // Assert that the actual value matches the expected value.
        assertEquals("Bearer test-jwt-token", capturedRequest.getHeader("Authorization"))

        // GET body should be empty.
        val body = capturedRequest.body.readUtf8()
        // Assert that the tested condition is true.
        assertTrue(body.isEmpty())
        // Close the current block.
    }

    // =============================================================
    // FAILURE TESTS FOR: updateWater(String username, int amount)
    // =============================================================

    // Mark the following function as a JUnit test.
    @Test
    // Declare the updateWater_notFound_returnsFalse test function.
    fun updateWater_notFound_returnsFalse() {
        // Clear leftover requests.
        drainRequests()

        // Set authentication token.
        RestClient.setAuthToken("test-jwt-token")

        // Enqueue 404 response.
        mockWebServer.enqueue(
            // Create a new fake HTTP response.
            MockResponse()
                // Set the fake HTTP response status code to 404.
                .setResponseCode(404)
                // Set the body returned by the fake HTTP response.
                .setBody("User not found")
            // Close the current function call.
        )

        // Call updateWater.
        val future = RestClient.updateWater("ghost", 400)

        // Wait for result.
        val result = awaitBoolean(future)

        // Assert failure.
        assertFalse(result)

        // Read request.
        val request = mockWebServer.takeRequest(FUTURE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        // Assert that the tested value is not null.
        assertNotNull(request)

        // Convert request to a non-null value after the null assertion.
        val capturedRequest = request!!

        // Allow PATCH or POST like the original test.
        val method = capturedRequest.method
        // Assert that the tested condition is true.
        assertTrue(
            // Provide the string value used by the current expression.
            "Expected HTTP method PATCH or POST but was: $method",
            // Execute this Kotlin statement as part of the current test flow.
            method == "PATCH" || method == "POST"
            // Close the current function call.
        )

        // Assert that the actual value matches the expected value.
        assertEquals("/myapp/api/users/ghost/water?amount=400", capturedRequest.path)
        // Assert that the actual value matches the expected value.
        assertEquals("Bearer test-jwt-token", capturedRequest.getHeader("Authorization"))

        // Body should be empty.
        val body = capturedRequest.body.readUtf8()
        // Assert that the tested condition is true.
        assertTrue(body.isEmpty())
        // Close the current block.
    }

    // =============================================================
    // FAILURE TESTS FOR: getWater(String username)
    // =============================================================

    // Mark the following function as a JUnit test.
    @Test
    // Declare the getWater_notFound_returnsNull test function.
    fun getWater_notFound_returnsNull() {
        // Clear leftover requests.
        drainRequests()

        // Set authentication token.
        RestClient.setAuthToken("test-jwt-token")

        // Enqueue 404 response.
        mockWebServer.enqueue(
            // Create a new fake HTTP response.
            MockResponse()
                // Set the fake HTTP response status code to 404.
                .setResponseCode(404)
                // Set the body returned by the fake HTTP response.
                .setBody("User not found")
            // Close the current function call.
        )

        // Call getWater.
        val future = RestClient.getWater("ghost")

        // Wait for result.
        val obj = awaitJson(future)

        // Assert null.
        assertNull(obj)

        // Read request.
        val request = mockWebServer.takeRequest(FUTURE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        // Assert that the tested value is not null.
        assertNotNull(request)

        // Convert request to a non-null value after the null assertion.
        val capturedRequest = request!!

        // Assert that the actual value matches the expected value.
        assertEquals("GET", capturedRequest.method)
        // Assert that the actual value matches the expected value.
        assertEquals("/myapp/api/users/ghost/water", capturedRequest.path)
        // Assert that the actual value matches the expected value.
        assertEquals("Bearer test-jwt-token", capturedRequest.getHeader("Authorization"))

        // Body should be empty.
        val body = capturedRequest.body.readUtf8()
        // Assert that the tested condition is true.
        assertTrue(body.isEmpty())
        // Close the current block.
    }

    // =============================================================
    // FAILURE TESTS FOR: getWaterHistoryMap(String username, int days)
    // =============================================================

    // Mark the following function as a JUnit test.
    @Test
    // Declare the getWaterHistoryMap_notFound_returnsNull test function.
    fun getWaterHistoryMap_notFound_returnsNull() {
        // Clear leftover requests.
        drainRequests()

        // Set authentication token.
        RestClient.setAuthToken("test-jwt-token")

        // Enqueue 404 response.
        mockWebServer.enqueue(
            // Create a new fake HTTP response.
            MockResponse()
                // Set the fake HTTP response status code to 404.
                .setResponseCode(404)
                // Set the body returned by the fake HTTP response.
                .setBody("User not found")
            // Close the current function call.
        )

        // Call getWaterHistoryMap.
        val future = RestClient.getWaterHistoryMap("ghost", 7)

        // Wait for result.
        val obj = awaitJson(future)

        // Assert null.
        assertNull(obj)

        // Read request.
        val request = mockWebServer.takeRequest(FUTURE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        // Assert that the tested value is not null.
        assertNotNull(request)

        // Convert request to a non-null value after the null assertion.
        val capturedRequest = request!!

        // Assert that the actual value matches the expected value.
        assertEquals("GET", capturedRequest.method)
        // Assert that the actual value matches the expected value.
        assertEquals("/myapp/api/users/ghost/waterHistoryMap?days=7", capturedRequest.path)
        // Assert that the actual value matches the expected value.
        assertEquals("Bearer test-jwt-token", capturedRequest.getHeader("Authorization"))

        // Body should be empty.
        val body = capturedRequest.body.readUtf8()
        // Assert that the tested condition is true.
        assertTrue(body.isEmpty())
        // Close the current block.
    }

    // =============================================================
    // FAILURE TESTS FOR: getWeeklyAverages(String username)
    // =============================================================

    // Mark the following function as a JUnit test.
    @Test
    // Declare the getWeeklyAverages_serverError_returnsEmptyMap test function.
    fun getWeeklyAverages_serverError_returnsEmptyMap() {
        // Clear leftover requests.
        drainRequests()

        // Set authentication token.
        RestClient.setAuthToken("test-jwt-token")

        // Enqueue server error.
        mockWebServer.enqueue(
            // Create a new fake HTTP response.
            MockResponse()
                // Set the fake HTTP response status code to 500.
                .setResponseCode(500)
                // Set the body returned by the fake HTTP response.
                .setBody("Server error")
            // Close the current function call.
        )

        // Call getWeeklyAverages.
        val future = RestClient.getWeeklyAverages("john")

        // Wait for result.
        val map = awaitMap(future)

        // Assert empty map.
        assertNotNull(map)
        // Assert that the tested condition is true.
        assertTrue(map.isEmpty())

        // Read request.
        val request = mockWebServer.takeRequest(FUTURE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        // Assert that the tested value is not null.
        assertNotNull(request)

        // Convert request to a non-null value after the null assertion.
        val capturedRequest = request!!

        // Assert that the actual value matches the expected value.
        assertEquals("GET", capturedRequest.method)
        // Assert that the actual value matches the expected value.
        assertEquals("/myapp/api/users/john/weeklyAverages", capturedRequest.path)
        // Assert that the actual value matches the expected value.
        assertEquals("Bearer test-jwt-token", capturedRequest.getHeader("Authorization"))

        // GET body should be empty.
        val body = capturedRequest.body.readUtf8()
        // Assert that the tested condition is true.
        assertTrue(body.isEmpty())
        // Close the current block.
    }

    // =============================================================
    // FAILURE TESTS FOR: getGoal(String username)
    // =============================================================

    // Mark the following function as a JUnit test.
    @Test
    // Declare the getGoal_notFound_completesExceptionally test function.
    fun getGoal_notFound_completesExceptionally() {
        // Clear leftover requests.
        drainRequests()

        // Set authentication token.
        RestClient.setAuthToken("test-jwt-token")

        // Enqueue 404 response.
        mockWebServer.enqueue(
            // Create a new fake HTTP response.
            MockResponse()
                // Set the fake HTTP response status code to 404.
                .setResponseCode(404)
                // Set the body returned by the fake HTTP response.
                .setBody("{}")
            // Close the current function call.
        )

        // Call getGoal.
        val future = RestClient.getGoal("ghost")

        // Define a flag to indicate that an exception was thrown.
        var threw = false

        // Wait for result and expect an exception.
        try {
            // Execute this Kotlin statement as part of the current test flow.
            awaitJson(future)
            // Catch the expected exception from the preceding operation.
        } catch (_: Exception) {
            // Record that the expected exception was thrown.
            threw = true
            // Close the current block.
        }

        // Assert exceptional completion.
        assertTrue(threw)

        // Read request.
        val request = mockWebServer.takeRequest(FUTURE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        // Assert that the tested value is not null.
        assertNotNull(request)

        // Convert request to a non-null value after the null assertion.
        val capturedRequest = request!!

        // Assert that the actual value matches the expected value.
        assertEquals("GET", capturedRequest.method)
        // Assert that the actual value matches the expected value.
        assertEquals("/myapp/api/users/ghost/goal", capturedRequest.path)
        // Assert that the actual value matches the expected value.
        assertEquals("Bearer test-jwt-token", capturedRequest.getHeader("Authorization"))

        // GET body should be empty.
        val body = capturedRequest.body.readUtf8()
        // Assert that the tested condition is true.
        assertTrue(body.isEmpty())
        // Close the current block.
    }

    // =============================================================
    // FAILURE TESTS FOR: setGoal(String username, int goalMl)
    // =============================================================

    // Mark the following function as a JUnit test.
    @Test
    // Declare the setGoal_invalidValue_returnsFalse test function.
    fun setGoal_invalidValue_returnsFalse() {
        // Clear leftover requests.
        drainRequests()

        // Set authentication token.
        RestClient.setAuthToken("test-jwt-token")

        // Enqueue bad request response.
        mockWebServer.enqueue(
            // Create a new fake HTTP response.
            MockResponse()
                // Set the fake HTTP response status code to 400.
                .setResponseCode(400)
                // Set the body returned by the fake HTTP response.
                .setBody("{\"status\":\"INVALID_OR_NOT_FOUND\"}")
            // Close the current function call.
        )

        // Call setGoal.
        val future = RestClient.setGoal("john", 100)

        // Wait for result.
        val result = awaitBoolean(future)

        // Assert failure.
        assertFalse(result)

        // Read request.
        val request = mockWebServer.takeRequest(FUTURE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        // Assert that the tested value is not null.
        assertNotNull(request)

        // Convert request to a non-null value after the null assertion.
        val capturedRequest = request!!

        // Assert that the actual value matches the expected value.
        assertEquals("PUT", capturedRequest.method)
        // Assert that the actual value matches the expected value.
        assertEquals("/myapp/api/users/john/goal?goalMl=100", capturedRequest.path)
        // Assert that the actual value matches the expected value.
        assertEquals("Bearer test-jwt-token", capturedRequest.getHeader("Authorization"))

        // Body should be empty.
        val body = capturedRequest.body.readUtf8()
        // Assert that the tested condition is true.
        assertTrue(body.isEmpty())
        // Close the current block.
    }

    // =============================================================
    // FAILURE TESTS FOR: getBmiDistribution()
    // =============================================================

    // Mark the following function as a JUnit test.
    @Test
    // Declare the getBmiDistribution_serverError_returnsNull test function.
    fun getBmiDistribution_serverError_returnsNull() {
        // Clear leftover requests.
        drainRequests()

        // Enqueue server error.
        mockWebServer.enqueue(
            // Create a new fake HTTP response.
            MockResponse()
                // Set the fake HTTP response status code to 500.
                .setResponseCode(500)
                // Set the body returned by the fake HTTP response.
                .setBody("Error")
            // Close the current function call.
        )

        // Call getBmiDistribution.
        val future = RestClient.getBmiDistribution()

        // Wait for result.
        val obj = awaitJson(future)

        // Assert null.
        assertNull(obj)

        // Read request.
        val request = mockWebServer.takeRequest(FUTURE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        // Assert that the tested value is not null.
        assertNotNull(request)

        // Convert request to a non-null value after the null assertion.
        val capturedRequest = request!!

        // Assert that the actual value matches the expected value.
        assertEquals("GET", capturedRequest.method)
        // Assert that the actual value matches the expected value.
        assertEquals("/myapp/api/users/stats/bmiDistribution", capturedRequest.path)

        // GET body should be empty.
        val body = capturedRequest.body.readUtf8()
        // Assert that the tested condition is true.
        assertTrue(body.isEmpty())
        // Close the current block.
    }

    // =============================================================
    // FAILURE TESTS FOR: getCalories(String username)
    // =============================================================

    // Mark the following function as a JUnit test.
    @Test
    // Declare the getCalories_badRequest_returnsNull test function.
    fun getCalories_badRequest_returnsNull() {
        // Clear leftover requests.
        drainRequests()

        // Set authentication token.
        RestClient.setAuthToken("test-jwt-token")

        // Enqueue bad request response.
        mockWebServer.enqueue(
            // Create a new fake HTTP response.
            MockResponse()
                // Set the fake HTTP response status code to 400.
                .setResponseCode(400)
                // Set the body returned by the fake HTTP response.
                .setBody("Bad request")
            // Close the current function call.
        )

        // Call getCalories.
        val future = RestClient.getCalories("john")

        // Wait for result.
        val value = awaitInteger(future)

        // Assert null.
        assertNull(value)

        // Read request.
        val request = mockWebServer.takeRequest(FUTURE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        // Assert that the tested value is not null.
        assertNotNull(request)

        // Convert request to a non-null value after the null assertion.
        val capturedRequest = request!!

        // Assert that the actual value matches the expected value.
        assertEquals("GET", capturedRequest.method)
        // Assert that the actual value matches the expected value.
        assertEquals("/myapp/api/users/john/calories", capturedRequest.path)
        // Assert that the actual value matches the expected value.
        assertEquals("Bearer test-jwt-token", capturedRequest.getHeader("Authorization"))

        // GET body should be empty.
        val body = capturedRequest.body.readUtf8()
        // Assert that the tested condition is true.
        assertTrue(body.isEmpty())
        // Close the current block.
    }

    // =============================================================
    // FAILURE TESTS FOR: setCalories(String username, int calories)
    // =============================================================

    // Mark the following function as a JUnit test.
    @Test
    // Declare the setCalories_badRequest_returnsFalse test function.
    fun setCalories_badRequest_returnsFalse() {
        // Clear leftover requests.
        drainRequests()

        // Set authentication token.
        RestClient.setAuthToken("test-jwt-token")

        // Enqueue bad request response.
        mockWebServer.enqueue(
            // Create a new fake HTTP response.
            MockResponse()
                // Set the fake HTTP response status code to 400.
                .setResponseCode(400)
                // Set the body returned by the fake HTTP response.
                .setBody("Invalid calories")
            // Close the current function call.
        )

        // Call setCalories.
        val future = RestClient.setCalories("john", -10)

        // Wait for result.
        val result = awaitBoolean(future)

        // Assert failure.
        assertFalse(result)

        // Read request.
        val request = mockWebServer.takeRequest(FUTURE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        // Assert that the tested value is not null.
        assertNotNull(request)

        // Convert request to a non-null value after the null assertion.
        val capturedRequest = request!!

        // Assert that the actual value matches the expected value.
        assertEquals("PUT", capturedRequest.method)
        // Assert that the actual value matches the expected value.
        assertEquals("/myapp/api/users/john/calories?calories=-10", capturedRequest.path)
        // Assert that the actual value matches the expected value.
        assertEquals("Bearer test-jwt-token", capturedRequest.getHeader("Authorization"))

        // PUT body should be empty.
        val body = capturedRequest.body.readUtf8()
        // Assert that the tested condition is true.
        assertTrue(body.isEmpty())
        // Close the current block.
    }
}
