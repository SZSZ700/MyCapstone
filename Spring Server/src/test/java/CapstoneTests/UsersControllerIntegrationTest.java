// Define the package for this integration test class
package CapstoneTests;
import org.example.CapstoneProject.Application;
// Import JUnit 5 test annotations
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
// Import the lifecycle enum for @TestInstance
import org.junit.jupiter.api.TestInstance.Lifecycle;
// Import static assertions from JUnit for readability
import static org.junit.jupiter.api.Assertions.*;
// Import the Spring Boot test annotation to load the full application context
import org.springframework.boot.test.context.SpringBootTest;
// Import Autowired to inject beans into this test class
import org.springframework.beans.factory.annotation.Autowired;
// Import UserService to prepare and clean up test users through the service layer
import org.example.CapstoneProject.service.UserService;
import org.example.CapstoneProject.service.JwtService;
// Import the User model used in requests and responses
import org.example.CapstoneProject.model.User;
// Import Spring's TestRestTemplate for real HTTP calls to the running server
import org.springframework.boot.test.web.client.TestRestTemplate;
// Import ResponseEntity and HttpStatus for inspecting HTTP responses
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
// Import HttpMethod for non-GET/POST HTTP verbs (PUT, PATCH, DELETE, HEAD)
import org.springframework.http.HttpMethod;
// Import HttpEntity to send request bodies for PUT/PATCH
import org.springframework.http.HttpEntity;
// Import Java TimeUnit for waiting on asynchronous service operations in helpers
import java.util.concurrent.TimeUnit;
// Import Java utilities for maps and collections
import java.util.*;
import org.example.CapstoneProject.dto.UserResponse;
import org.example.CapstoneProject.dto.LoginResponse;
import org.springframework.security.crypto.password.PasswordEncoder;

// UsersControllerIntegrationTest is an end-to-end integration test class that
// verifies the REST API exposed by UsersController using a real embedded
// Spring Boot web server. The tests use TestRestTemplate to perform real HTTP
// requests (GET, POST, PUT, PATCH, DELETE, HEAD) against the /api/users
// endpoints and assert on both the HTTP status codes and response bodies.
// Test users are prepared and cleaned up through UserService, while the
// controller itself is always exercised through HTTP. In this way, the class
// validates request mappings, URL paths, query parameters, JSON
// serialization/deserialization and the complete controller-to-service-to-
// repository integration from the client's point of view.

// RestTemplate is a Spring HTTP client that allows us to call REST endpoints
// in a simple, type-safe way. Instead of manually opening connections,
// writing JSON, and parsing responses, we use RestTemplate to send HTTP
// requests (GET, POST, PUT, PATCH, DELETE, etc.) and automatically map
// the response into Java objects such as String, User, or Map. In this
// test class we use TestRestTemplate (a specialized version for tests) to
// simulate a real client calling our running Spring Boot server, so we can
// verify the full HTTP behavior of our controllers end-to-end.

// Mark this class as a Spring Boot integration test (loads the full context + web server)
@SuppressWarnings({"ConstantValue", "JavaPrintToLogpoint"})
@SpringBootTest(
        classes = Application.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
// Use a single test instance for the whole class (allows @BeforeAll non-static)
@TestInstance(Lifecycle.PER_CLASS)
public class UsersControllerIntegrationTest {

    // Inject TestRestTemplate to perform real HTTP calls to the running application
    @Autowired
    private TestRestTemplate restTemplate;

    // Inject UserService to prepare and clean up test data.
    @Autowired
    private UserService userService;

    // Inject PasswordEncoder so test users that are inserted directly
    // through UserService are stored with BCrypt passwords.
    @Autowired
    private PasswordEncoder passwordEncoder;

    // Inject JwtService so controller integration requests can use real JWT tokens.
    @Autowired
    private JwtService jwtService;

    // Define a constant timeout in seconds for service helper calls
    private final long TIMEOUT_SECONDS = 20L;

    // Keep track of all usernames created during this test class
    // Use a synchronized set to avoid duplicate entries and be safe if tests run in parallel.
    private final Set<String> createdUsernames = Collections.synchronizedSet(new HashSet<>());

    // --------------------------- HELPER METHODS ---------------------------

    // Helper method to build a basic User instance with required fields
    private User buildUser(String username, String password) {
        // Create a new User instance
        var user = new User();
        // Set the username field for this user
        user.setUserName(username);
        // Set the password field for this user
        user.setPassword(password);
        // set full name for clarity in debug
        user.setFullName("Test User " + username);
        // set an age for this user
        user.setAge(25);

        // Return the prepared user object
        return user;
    }

    // ---------------------------------------------------------------------
    // Creates a test user directly through UserService.
    //
    // Because createUser() is a low-level creation method and does not
    // perform authentication logic, the password is encoded here before
    // the user is stored.
    //
    // This keeps test data consistent with real users created through
    // the signup endpoint.
    // ---------------------------------------------------------------------
    @SuppressWarnings("UnusedReturnValue")
    private User createUserInFirebase(String username, String password) throws Exception {
        // Create a new User object with the raw test password.
        var user = buildUser(username, password);

        // Encode the raw password with BCrypt before storing
        // the user directly through UserService.
        user.setPassword(passwordEncoder.encode(password));

        // Persist the user.
        var future = userService.createUser(user);

        // Wait for the asynchronous operation to complete.
        var created = future.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);

        // Fail immediately if the test user could not be created.
        assertTrue(
                created,
                "Failed to create test user in Firebase: " + username
        );

        // Remember the username so it can be deleted after the tests.
        if (created) {
            createdUsernames.add(username);
        }

        // Return the stored user object.
        return user;
    }

    // Helper method to delete a test user safely through UserService
    private void deleteUserInFirebase(String username) {
        try {
            // Call deleteUser on UserService
            var future = userService.deleteUser(username);
            // Wait for the async result with a timeout
            @SuppressWarnings("unused")
            var deleted = future.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (Exception e) {
            // Log a warning but do not fail the whole test suite because of cleanup
            System.out.println("WARN cleanup failed for username=" + username
                    + " message=" + e.getMessage());
        }
    }

    // ---------------------------------------------------------------------
    // Returns the username that should be used to generate a JWT for the
    // current protected request. Public endpoints return null.
    //
    // For user-specific routes, the first path segment after /api/users/
    // is the username and therefore must match the JWT subject.
    // ---------------------------------------------------------------------
    private String getJwtUsernameForRequest(String path) {
        // basePath = "/api/users"
        var basePath = "/api/users";

        // the base index of the path, e.g. "/api/users/health" -> 0
        var baseIndex = path.indexOf(basePath);

        // if the path does not start with basePath, return null
        if (baseIndex == -1) { return null; }

        // store the full API path starting from "/api/users"
        // e.g. "/api/users/health" -> "/api/users/health"
        var apiPath = path.substring(baseIndex);

        // if the path equals one of the public endpoint paths, return null
        // because public endpoints do not require JWT authentication.
        // these endpoints also do not contain a username path variable
        // that needs to match the JWT subject.
        if ("/api/users/health".equals(apiPath)
                || "/api/users/signup".equals(apiPath)
                || "/api/users/login".equals(apiPath)
                || "/api/users/stats/bmiDistribution".equals(apiPath)) {
            return null;
        }

        // if the path is exactly "/api/users", there is no username
        // inside the URL that can be extracted as the JWT subject.
        //
        // return a fixed test username so the interceptor can still
        // generate a valid JWT for this protected endpoint.
        if ("/api/users".equals(apiPath)) { return "controller-integration-test"; }

        // define the prefix that appears before every username
        // in the protected user-specific endpoints.
        //
        // example:
        // "/api/users/john/water"
        //              ^
        //              everything after this prefix starts with the username
        var prefix = "/api/users/";

        // if the API path does not start with the expected prefix,
        // it is not a normal user-specific endpoint, so return null.
        if (!apiPath.startsWith(prefix)) { return null; }

        // remove "/api/users/" from the beginning of the path.
        //
        // example:
        // "/api/users/john/water"
        // -> "john/water"
        var remainingPath = apiPath.substring(prefix.length());

        // search for the first "/" after the username.
        //
        // example:
        // "john/water"
        //      ^
        //      slashIndex = 4
        //
        // for a path such as "/api/users/john",
        // remainingPath is only "john", so no "/" will be found.
        var slashIndex = remainingPath.indexOf('/');

        // if there is no "/" after the username,
        // the remaining path itself is the username.
        //
        // example:
        // "/api/users/john"
        // -> remainingPath = "john"
        // -> return "john"
        if (slashIndex == -1) { return remainingPath; }

        // if another path segment exists after the username,
        // return only the part before the first "/".
        //
        // example:
        // "/api/users/john/water"
        // -> remainingPath = "john/water"
        // -> slashIndex = 4
        // -> substring(0, 4) = "john"
        return remainingPath.substring(0, slashIndex);
    }

    // --------------------------- BASIC SETUP (OPTIONAL) ---------------------------

    // Optional setup method to ensure the context is ready before tests
    // this methoud catch the outgoing requests and intercept them to
    // add a JWT to the Authorization header.
    // The interceptor is added to the RestTemplate's interceptors list
    // and will be executed before the request is sent.
    @BeforeAll
    public void beforeAll() {
        // Print a debug message indicating that UsersControllerIntegrationTest started
        System.out.println("DEBUG UsersControllerIntegrationTest");

        // Add a test interceptor that attaches a real JWT to protected requests.
        // Public endpoints remain unchanged and receive no Authorization header.
        this.restTemplate.getRestTemplate().getInterceptors().add((request, body, execution) -> {
            // call the getJwtUsernameForRequest() helper method to determine
            // the username that should be used for the JWT generation
            // from the current request.
            var username = getJwtUsernameForRequest(request.getURI().getPath());

            if (username != null && !username.isBlank()) {
                // call jwtService.generateToken(username) to generate a JWT
                // for the user.
                String token = jwtService.generateToken(username);
                // Attach the JWT to the Authorization header.
                request.getHeaders().setBearerAuth(token);
            }

            // Continue with the request execution.
            return execution.execute(request, body);
        });
    }

    @AfterAll
    public void cleanupAllTestUsers() {
        // Iterate over a snapshot to avoid concurrent modification while cleaning up.
        for (String username : new ArrayList<>(this.createdUsernames)) {
            deleteUserInFirebase(username);
        }
    }

    // --------------------------- HEALTH CHECK TEST ---------------------------

    // Test that the /api/users/health endpoint returns 200 OK with body "OK"
    @Test
    public void health_returnsOk() {
        // Perform a GET request to /api/users/health and expect a String body
        var response = this.restTemplate.getForEntity("/api/users/health", String.class);

        // Assert that the HTTP status is 200 OK
        assertEquals(HttpStatus.OK, response.getStatusCode());
        // Assert that the response body is exactly "OK"
        assertEquals("OK", response.getBody());
    }

    // --------------------------- SIGNUP TESTS ---------------------------

    // Test that signup creates a new user with a BCrypt password
    // and rejects duplicate usernames.
    @Test
    public void signup_createsUserAndRejectsDuplicate() throws Exception {
        // Build a unique username.
        var username = "signupController_" + System.currentTimeMillis();
        // Keep the raw password that a real client would send.
        var rawPassword = "signupPass";

        // Build the signup request.
        var user = buildUser(username, rawPassword);

        // Send the first signup request.
        var firstResponse = this.restTemplate.postForEntity(
                "/api/users/signup",
                user,
                String.class
        );

        // Verify successful creation.
        assertEquals(HttpStatus.CREATED, firstResponse.getStatusCode());

        assertEquals("User created successfully", firstResponse.getBody());

        // Remember the username for cleanup.
        this.createdUsernames.add(username);

        // Read the stored user directly from the service.
        var storedUser = userService.getUser(username).get(TIMEOUT_SECONDS, TimeUnit.SECONDS);

        // Verify that the user exists.
        assertNotNull(storedUser);

        // The raw password must never be stored directly.
        assertNotEquals(rawPassword, storedUser.getPassword());

        // Verify that the stored BCrypt hash matches the original password.
        assertTrue(passwordEncoder.matches(rawPassword, storedUser.getPassword()));

        // Send the same signup request again.
        var secondResponse = this.restTemplate.postForEntity(
                "/api/users/signup",
                user,
                String.class
        );

        // Duplicate usernames must be rejected.
        assertEquals(HttpStatus.CONFLICT, secondResponse.getStatusCode());

        assertEquals("Username already exists", secondResponse.getBody());
    }

    // --------------------------- LOGIN TESTS ---------------------------

    // Test that login succeeds with the correct raw password
    // when the stored password is a BCrypt hash.
    @Test
    public void login_withCorrectCredentials_returnsUserJson() throws Exception {
        // Build a unique username.
        var username = "loginOk_" + System.currentTimeMillis();
        // Create the test user with a BCrypt encoded password.
        createUserInFirebase(username, "pass1");

        // Build the login request.
        // Login requests must contain the raw password because
        // PasswordEncoder.matches() compares it with the stored hash.
        var loginRequestUser = new User();
        loginRequestUser.setUserName(username);
        loginRequestUser.setPassword("pass1");

        // Send the login request and deserialize the response
        // into LoginResponse because login now also returns a JWT.
        var response = this.restTemplate.postForEntity(
                "/api/users/login",
                loginRequestUser,
                LoginResponse.class
        );

        // Verify successful authentication.
        assertEquals(HttpStatus.OK, response.getStatusCode());

        var body = response.getBody();

        assertNotNull(body);

        // Verify that login returned a JWT.
        assertNotNull(body.getToken());
        assertFalse(body.getToken().isBlank());

        // Verify the public user information.
        assertEquals(username, body.getUserName());

        assertEquals(25, body.getAge());

        assertEquals("Test User " + username, body.getFullName());
    }

    // Test that login returns 401 Unauthorized when the raw password
    // does not match the stored BCrypt password.
    @Test
    public void login_withWrongPassword_returns401() throws Exception {
        // Build a unique username.
        var username = "loginBad_" + System.currentTimeMillis();

        // Create the user with a BCrypt encoded password.
        createUserInFirebase(username, "realPass");

        // Build a login request with an incorrect raw password.
        var loginRequestUser = new User();
        loginRequestUser.setUserName(username);
        loginRequestUser.setPassword("wrongPass");

        // Send the login request.
        var response = this.restTemplate.postForEntity(
                "/api/users/login",
                loginRequestUser,
                String.class
        );

        // Authentication must fail.
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());

        assertEquals("Invalid username or password", response.getBody());
    }

    // --------------------------- GET ALL USERS TEST ---------------------------

    // Test that getAllUsers returns an array and contains at least one user
    @Test
    public void getAllUsers_returnsArrayAndContainsAtLeastOneUser() throws Exception {
        var username = "allUsers_" + System.currentTimeMillis();

        // Create a BCrypt-backed test user.
        createUserInFirebase(username, "p");

        // Public API returns UserResponse objects,
        // which intentionally do not expose passwords.
        var response = this.restTemplate.getForEntity(
                "/api/users",
                UserResponse[].class
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());

        var users = response.getBody();

        assertNotNull(users);

        assertTrue(
                Arrays.stream(users).anyMatch(user -> username.equals(user.getUserName())),
                "Expected getAllUsers response to include created test user: " + username
        );
    }

    // --------------------------- GET USER TESTS ---------------------------

    @Test
    public void getUser_existingUser_returnsUserJson() throws Exception {
        // Build a unique username for this test.
        var username = "getUserOk_" + System.currentTimeMillis();

        // Create the user with a BCrypt encoded password.
        createUserInFirebase(username, "p");

        // Perform a GET request and expect a UserResponse object.
        //
        // UserResponse intentionally does not expose the password field.
        var response = this.restTemplate.getForEntity(
                "/api/users/" + username,
                UserResponse.class
        );

        // Assert that the request succeeded.
        assertEquals(HttpStatus.OK, response.getStatusCode());

        // Extract the response body.
        var body = response.getBody();

        // Assert that the response body is not null.
        assertNotNull(body);

        // Assert that the returned username matches the created user.
        assertEquals(username, body.getUserName());

        // Assert that the returned full name matches the created user.
        assertEquals("Test User " + username, body.getFullName());

        // Assert that the returned age matches the created user.
        assertEquals(25, body.getAge());
    }

    // Test that getUser returns 404 and error message when user does not exist
    @Test
    public void getUser_nonExistingUser_returns404() {
        // Build a username that definitely does not exist
        String username = "noSuchUser_" + System.currentTimeMillis();

        // Perform a GET request to /api/users/{username} expecting String body
        ResponseEntity<String> response = this.restTemplate.getForEntity("/api/users/" + username, String.class);

        // Assert that HTTP status is 404 Not Found
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        // Assert that body is "User not found"
        assertEquals("User not found", response.getBody());
    }

    // --------------------------- UPDATE (PUT) USER TESTS ---------------------------

    // Test that updateUser replaces entire record and returns updated user for existing user
    @Test
    public void updateUser_existingUser_replacesRecord() throws Exception {
        // Build a unique username for this test.
        var username = "updateUserOk_" + System.currentTimeMillis();

        // Create the initial user with a BCrypt encoded password.
        createUserInFirebase(username, "origPass");

        // Build the updated user object with a new raw password.
        //
        // The raw password is intentionally sent because the service
        // is responsible for encoding it before saving it.
        var updated = buildUser(username, "newPass");

        // Update the full name.
        updated.setFullName("Updated Name");

        // Update the age.
        updated.setAge(30);

        // Wrap the updated user inside an HTTP entity.
        var entity = new HttpEntity<>(updated);

        // Perform the PUT request.
        //
        // The endpoint now returns UserResponse instead of User
        // so the password is never exposed in the HTTP response.
        var response = this.restTemplate.exchange(
                "/api/users/{username}",
                HttpMethod.PUT,
                entity,
                UserResponse.class,
                username
        );

        // Assert that the update succeeded.
        assertEquals(HttpStatus.OK, response.getStatusCode());

        // Extract the public response body.
        var body = response.getBody();

        // Assert that the response body is not null.
        assertNotNull(body);

        // Assert that the username remains unchanged.
        assertEquals(username, body.getUserName());

        // Assert that the full name was updated.
        assertEquals("Updated Name", body.getFullName());

        // Assert that the age was updated.
        assertEquals(30, body.getAge());

        // Read the complete stored user directly through UserService
        // so the internal password hash can be verified.
        var storedUser = userService.getUser(username).get(TIMEOUT_SECONDS, TimeUnit.SECONDS);

        // Assert that the stored user exists.
        assertNotNull(storedUser);

        // Assert that the raw password was not stored directly.
        assertNotEquals("newPass", storedUser.getPassword());

        // Assert that the stored BCrypt hash matches the new raw password.
        assertTrue(passwordEncoder.matches("newPass", storedUser.getPassword()));
    }

    // Test that updateUser returns 404 when user does not exist
    @Test
    public void updateUser_nonExistingUser_returns404() {
        // Build a username that does not exist
        var username = "updateNoSuch_" + System.currentTimeMillis();

        // Build an updated user body for this non-existing username
        var updated = buildUser(username, "p");
        // Wrap the updated user into HttpEntity for the request body
        var entity = new HttpEntity<>(updated);

        // Perform a PUT request to /api/users/{username} expecting String body
        var response = this.restTemplate.exchange(
                "/api/users/{username}",
                HttpMethod.PUT,
                entity,
                String.class,
                username
        );

        // Assert that HTTP status is 404 Not Found
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        // Assert that body is "User not found"
        assertEquals("User not found", response.getBody());
    }

    // --------------------------- PATCH USER TESTS ---------------------------

    // Test that patchUser updates a single field (fullName) for an existing user
    @Test
    public void patchUser_existingUser_updatesField() throws Exception {
        // Build a unique username for this test.
        var username = "patchUserOk_" + System.currentTimeMillis();

        // Create the initial user with a BCrypt encoded password.
        createUserInFirebase(username, "p");

        // Create a map containing only the field that should be updated.
        Map<String, Object> updates = new HashMap<>();

        // Update only the fullName field.
        updates.put("fullName", "Patched Name");

        // Wrap the PATCH body inside an HTTP entity.
        var entity = new HttpEntity<>(updates);

        // Perform the PATCH request.
        //
        // The endpoint returns UserResponse so private fields
        // such as password are not exposed.
        var response = this.restTemplate.exchange(
                "/api/users/{username}",
                HttpMethod.PATCH,
                entity,
                UserResponse.class,
                username
        );

        // Assert that the PATCH request succeeded.
        assertEquals(HttpStatus.OK, response.getStatusCode());

        // Extract the response body.
        var body = response.getBody();

        // Assert that the response body is not null.
        assertNotNull(body);

        // Assert that the requested field was updated.
        assertEquals("Patched Name", body.getFullName());

        // Assert that the username remains unchanged.
        assertEquals(username, body.getUserName());
    }

    @Test
    public void patchUser_password_encodesPasswordAndAllowsLogin() throws Exception {
        // Build a unique username for this test.
        var username = "patchPassword_" + System.currentTimeMillis();

        // Create the initial user with an existing BCrypt password.
        createUserInFirebase(username, "oldPass");

        // Create a partial update map containing a new raw password.
        Map<String, Object> updates = new HashMap<>();

        // Add the new raw password to the PATCH body.
        //
        // UserService should encode this password before
        // passing it to the repository.
        updates.put("password", "patchedPass");

        // Wrap the update map inside an HTTP entity.
        var entity = new HttpEntity<>(updates);

        // Perform the PATCH request.
        //
        // The response uses UserResponse so the password
        // is not exposed back to the client.
        var patchResponse = this.restTemplate.exchange(
                "/api/users/{username}",
                HttpMethod.PATCH,
                entity,
                UserResponse.class,
                username
        );

        // Assert that the password update succeeded.
        assertEquals(HttpStatus.OK, patchResponse.getStatusCode());

        // Assert that a valid response body was returned.
        assertNotNull(patchResponse.getBody());

        // Read the complete user directly through UserService
        // so the stored password hash can be inspected.
        var storedUser = userService.getUser(username).get(TIMEOUT_SECONDS, TimeUnit.SECONDS);

        // Assert that the user still exists.
        assertNotNull(storedUser);

        // Assert that the raw password was not stored directly.
        assertNotEquals("patchedPass", storedUser.getPassword());

        // Assert that the BCrypt hash stored in Firebase
        // correctly matches the new raw password.
        assertTrue(passwordEncoder.matches("patchedPass", storedUser.getPassword()));

        // Build a login request using the new raw password.
        var loginRequest = new User();

        // Set the username.
        loginRequest.setUserName(username);

        // Set the new raw password.
        loginRequest.setPassword("patchedPass");

        // Perform login using the newly patched password.
        var loginResponse = this.restTemplate.postForEntity(
                "/api/users/login",
                loginRequest,
                LoginResponse.class
        );

        // Assert that authentication succeeds.
        assertEquals(HttpStatus.OK, loginResponse.getStatusCode());

        // Assert that the login response body is not null.
        assertNotNull(loginResponse.getBody());

        // Assert that login returned a JWT after the password update.
        assertNotNull(loginResponse.getBody().getToken());
        assertFalse(loginResponse.getBody().getToken().isBlank());

        // Assert that the authenticated username is correct.
        assertEquals(username, loginResponse.getBody().getUserName());
    }

    // Test that patchUser returns 404 Not Found when user does not exist
    @Test
    public void patchUser_nonExistingUser_returns404() {
        // Build a username that does not exist
        var username = "patchNoSuch_" + System.currentTimeMillis();

        // Build an updates map for this non-existing user
        Map<String, Object> updates = new HashMap<>();
        // Put some dummy field into updates
        updates.put("fullName", "Someone");
        // Wrap the updates map into HttpEntity
        var entity = new HttpEntity<>(updates);

        // Perform a PATCH request to /api/users/{username} expecting String body
        var response = this.restTemplate.exchange(
                "/api/users/{username}",
                HttpMethod.PATCH,
                entity,
                String.class,
                username
        );

        // Assert that HTTP status is 404 Not Found
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        // Assert that body is "User not found"
        assertEquals("User not found", response.getBody());
    }

    // --------------------------- DELETE USER TESTS ---------------------------

    // Test that deleteUser deletes an existing user and returns 200 with message
    @Test
    public void deleteUser_existingUser_returnsOk() throws Exception {
        // Build a unique username for this test
        var username = "deleteUserOk_" + System.currentTimeMillis();
        // Create the user through the service layer
        createUserInFirebase(username, "p");

        // Perform a DELETE request to /api/users/{username} expecting String body
        var response = this.restTemplate.exchange(
                "/api/users/{username}",
                HttpMethod.DELETE,
                null,
                String.class,
                username
        );

        // Assert that HTTP status is 200 OK
        assertEquals(HttpStatus.OK, response.getStatusCode());
        // Assert that body is "User deleted"
        assertEquals("User deleted", response.getBody());
    }

    // Test that deleteUser returns 404 when user does not exist
    @Test
    public void deleteUser_nonExistingUser_returns404() {
        // Build a username that does not exist
        var username = "deleteNoSuch_" + System.currentTimeMillis();

        // Perform a DELETE request to /api/users/{username} expecting String body
        var response = this.restTemplate.exchange(
                "/api/users/{username}",
                HttpMethod.DELETE,
                null,
                String.class,
                username
        );

        // Assert that HTTP status is 404 Not Found
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        // Assert that body is "User not found"
        assertEquals("User not found", response.getBody());
    }

    // --------------------------- HEAD USER TESTS ---------------------------

    // Test that headUser returns 200 OK when user exists (no body)
    @Test
    public void headUser_existingUser_returns200() throws Exception {
        // Build a unique username and create user in Firebase
        var username = "headUserOk_" + System.currentTimeMillis();
        // Create this user through the service layer
        createUserInFirebase(username, "p");

        // Perform a HEAD request to /api/users/{username} expecting no body
        var response = this.restTemplate.exchange(
                "/api/users/{username}",
                HttpMethod.HEAD,
                null,
                Void.class,
                username
        );

        // Assert that HTTP status is 200 OK
        assertEquals(HttpStatus.OK, response.getStatusCode());
        // Assert that the body is null (no content)
        assertNull(response.getBody());
    }

    // Test that headUser returns 404 Not Found when user does not exist
    @Test
    public void headUser_nonExistingUser_returns404() {
        // Build a username that does not exist
        var username = "headNoSuch_" + System.currentTimeMillis();

        // Perform a HEAD request to /api/users/{username}
        var response = this.restTemplate.exchange(
                "/api/users/{username}",
                HttpMethod.HEAD,
                null,
                Void.class,
                username
        );

        // Assert that HTTP status is 404 Not Found
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    // --------------------------- UPDATE BMI TESTS ---------------------------

    // Test that updateBmi returns 200 OK with success message for existing user
    @Test
    public void updateBmi_existingUser_returns200() throws Exception {
        // Build a unique username for this test
        var username = "bmiOk_" + System.currentTimeMillis();
        // Create the user through the service layer
        createUserInFirebase(username, "p");

        // Build URL with query parameter for bmi
        var url = "/api/users/" + username + "/bmi?bmi=23.5";

        // Perform a PATCH request to /api/users/{username}/bmi expecting String body
        var response = this.restTemplate.exchange(url, HttpMethod.PATCH, null, String.class);

        // Assert that HTTP status is 200 OK
        assertEquals(HttpStatus.OK, response.getStatusCode());
        // Assert that body contains confirmation message
        assertEquals("BMI updated successfully", response.getBody());
    }

    // Test that updateBmi returns 404 Not Found when user does not exist
    @Test
    public void updateBmi_nonExistingUser_returns404() {
        // Build a username that does not exist
        var username = "bmiNoSuch_" + System.currentTimeMillis();
        // Build URL with query parameter for bmi
        var url = "/api/users/" + username + "/bmi?bmi=23.5";

        // Perform a PATCH request to /api/users/{username}/bmi expecting String body
        var response = this.restTemplate.exchange(url, HttpMethod.PATCH, null, String.class);

        // Assert that HTTP status is 404 Not Found
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        // Assert that body contains "User not found"
        assertEquals("User not found", response.getBody());
    }

    // --------------------------- WATER MODULE TESTS ---------------------------

    // Test that updateWater and getWater work together via controller endpoints
    @Test
    public void updateWater_and_getWater_flowForExistingUser() throws Exception {
        // Build a unique username for this test
        var username = "waterOk_" + System.currentTimeMillis();
        // Create the user through the service layer
        createUserInFirebase(username, "p");

        // Perform a GET request to read initial water values
        var beforeResponse = this.restTemplate.getForEntity(
                "/api/users/" + username + "/water",
                Map.class
        );

        // Assert that HTTP status is 200 OK
        assertEquals(HttpStatus.OK, beforeResponse.getStatusCode());
        // Extract the response body as a Map
        @SuppressWarnings("unchecked")
        Map<String, Object> beforeBody = beforeResponse.getBody();
        // Assert that the map is not null
        assertNotNull(beforeBody);
        // Extract today's water as a Number and convert to long
        var todayBeforeNumber = (Number) beforeBody.getOrDefault("todayWater", 0);
        var todayBefore = todayBeforeNumber.longValue();

        // Define an amount of water to add
        var addedAmount = 400;

        // Build URL for PATCH request with amount parameter
        var patchUrl = "/api/users/" + username + "/water?amount=" + addedAmount;

        // Perform PATCH request to update water
        var patchResponse = this.restTemplate.exchange(
                patchUrl,
                HttpMethod.PATCH,
                null,
                String.class
        );

        // Assert that HTTP status is 200 OK
        assertEquals(HttpStatus.OK, patchResponse.getStatusCode());
        // Assert that body is "Water updated successfully"
        assertEquals("Water updated successfully", patchResponse.getBody());

        // Perform another GET request to read updated water totals
        var afterResponse = this.restTemplate.getForEntity(
                "/api/users/" + username + "/water",
                Map.class
        );

        // Assert that HTTP status is 200 OK
        assertEquals(HttpStatus.OK, afterResponse.getStatusCode());
        // Extract body as Map
        @SuppressWarnings("unchecked")
        Map<String, Object> afterBody = afterResponse.getBody();
        // Assert that body is not null
        assertNotNull(afterBody);
        // Extract today's water after update
        var todayAfterNumber = (Number) afterBody.getOrDefault("todayWater", 0);
        var todayAfter = todayAfterNumber.longValue();

        // Assert that today's water increased exactly by the added amount
        assertEquals(todayBefore + addedAmount, todayAfter);
    }

    // Test that getWater returns 404 for a non-existing user
    @Test
    public void getWater_nonExistingUser_returns404() {
        // Build a username that does not exist
        var username = "waterNoSuch_" + System.currentTimeMillis();

        // Perform a GET request to /api/users/{username}/water expecting String body
        var response = this.restTemplate.getForEntity(
                "/api/users/" + username + "/water",
                String.class
        );

        // Assert that HTTP status is 404 Not Found
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        // Assert that body is "User not found"
        assertEquals("User not found", response.getBody());
    }

    // --------------------------- WATER HISTORY MAP TESTS ---------------------------

    // Test that getWaterHistoryMap returns a JSON map with exactly "days" entries for existing user
    @Test
    public void getWaterHistoryMap_existingUser_returnsMapWithRequestedDays() throws Exception {
        // Build a unique username for this test
        var username = "waterHistoryOk_" + System.currentTimeMillis();
        // Create the user through the service layer
        createUserInFirebase(username, "p");
        // Define how many days we want
        var days = 5;
        // Build URL for GET request with days query parameter
        var url = "/api/users/" + username + "/waterHistoryMap?days=" + days;

        // Perform GET request expecting a Map body
        var response = this.restTemplate.getForEntity(url, Map.class);

        // Assert that HTTP status is 200 OK
        assertEquals(HttpStatus.OK, response.getStatusCode());
        // Extract response body as Map
        @SuppressWarnings("unchecked")
        Map<String, Object> body = response.getBody();
        // Assert that body is not null
        assertNotNull(body);
        // Assert that the map contains exactly "days" entries
        assertEquals(days, body.size());
    }

    // Test that getWaterHistoryMap returns 404 when user does not exist
    @Test
    public void getWaterHistoryMap_nonExistingUser_returns404() {
        // Build a username that does not exist
        var username = "waterHistoryNoSuch_" + System.currentTimeMillis();
        // Build URL with days parameter
        var url = "/api/users/" + username + "/waterHistoryMap?days=3";

        // Perform GET request expecting String body
        var response = this.restTemplate.getForEntity(url, String.class);

        // Assert that HTTP status is 404 Not Found
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        // Assert that body is "User not found"
        assertEquals("User not found", response.getBody());
    }

    // --------------------------- WEEKLY AVERAGES TESTS ---------------------------

    // Test that getWeeklyAverages returns 200 OK and a map for an existing user
    @Test
    public void getWeeklyAverages_existingUser_returnsMap() throws Exception {
        // Build a unique username for this test
        var username = "weeklyAvgOk_" + System.currentTimeMillis();
        // Create the user through the service layer
        createUserInFirebase(username, "p");
        // add some water so at least one week has non-zero average
        var waterUrl = "/api/users/" + username + "/water?amount=300";

        // Perform PATCH request to add water
        var waterResponse = this.restTemplate.exchange(
                waterUrl,
                HttpMethod.PATCH,
                null,
                String.class
        );

        // Assert that water update succeeded with status 200 OK
        assertEquals(HttpStatus.OK, waterResponse.getStatusCode());

        // Build URL for GET request to weekly averages endpoint
        var url = "/api/users/" + username + "/weeklyAverages";

        // Perform GET request expecting Map<String,Integer> body (as raw Map)
        var response = this.restTemplate.getForEntity(url, Map.class);

        // Assert that HTTP status is 200 OK
        assertEquals(HttpStatus.OK, response.getStatusCode());
        // Extract body as Map
        @SuppressWarnings("unchecked")
        Map<String, Object> body = response.getBody();
        // Assert that body is not null
        assertNotNull(body);
        // Assert that there are exactly 4 entries (Week 1..Week 4)
        assertEquals(4, body.size());
    }

    // Test that getWeeklyAverages returns 404 and empty map for non-existing user
    @Test
    public void getWeeklyAverages_nonExistingUser_returns404() {
        // Build a username that does not exist
        var username = "weeklyAvgNoSuch_" + System.currentTimeMillis();
        // Build URL for GET request
        var url = "/api/users/" + username + "/weeklyAverages";

        // Perform GET request expecting a JSON map body
        var response = this.restTemplate.exchange(
                url,
                HttpMethod.GET,
                null,
                Map.class
        );

        // Assert that HTTP status is 404 Not Found
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        // Assert that response body is an empty JSON object (parsed as map)
        @SuppressWarnings("unchecked")
        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        assertTrue(body.isEmpty());
    }

    // --------------------------- GOAL MODULE TESTS ---------------------------

    // Test that setGoal updates the goal and getGoal reads the same value via controller
    @Test
    public void goal_setAndGet_flowForExistingUser() throws Exception {
        // Build a unique username for this test
        var username = "goalOk_" + System.currentTimeMillis();
        // Create the user through the service layer
        createUserInFirebase(username, "p");
        // Define a valid goalMl value
        var newGoal = 3400;
        // Build URL for PUT request with goalMl parameter
        var setUrl = "/api/users/" + username + "/goal?goalMl=" + newGoal;

        // Perform PUT request expecting Map body
        var setResponse = this.restTemplate.exchange(
                setUrl,
                HttpMethod.PUT,
                null,
                Map.class
        );

        // Assert that HTTP status is 200 OK
        assertEquals(HttpStatus.OK, setResponse.getStatusCode());
        // Extract body as Map
        @SuppressWarnings("unchecked")
        Map<String, Object> setBody = setResponse.getBody();
        // Assert that body is not null
        assertNotNull(setBody);
        // Assert that status field is "OK"
        assertEquals("OK", setBody.get("status"));

        // Build URL for GET request to read the goal
        var getUrl = "/api/users/" + username + "/goal";
        // Perform GET request expecting Map body
        var getResponse = this.restTemplate.getForEntity(getUrl, Map.class);

        // Assert that HTTP status is 200 OK
        assertEquals(HttpStatus.OK, getResponse.getStatusCode());
        // Extract body as Map
        @SuppressWarnings("unchecked")
        Map<String, Object> getBody = getResponse.getBody();
        // Assert that body is not null
        assertNotNull(getBody);
        // Extract goalMl as Number and compare with newGoal
        var goalNumber = (Number) getBody.get("goalMl");
        // Assert that goalMl equals the newGoal value
        assertEquals(newGoal, goalNumber.intValue());
    }

    // Test that setGoal with invalid value returns 400 Bad Request and proper status
    @Test
    public void setGoal_invalidValue_returnsBadRequest() throws Exception {
        // Build a unique username for this test
        var username = "goalInvalid_" + System.currentTimeMillis();
        // Create the user through the service layer
        createUserInFirebase(username, "p");

        // Build URL for PUT request with invalid goalMl value (too low)
        var url = "/api/users/" + username + "/goal?goalMl=100";
        // Perform PUT request expecting Map body
        var response = this.restTemplate.exchange(
                url,
                HttpMethod.PUT,
                null,
                Map.class
        );

        // Assert that HTTP status is 400 Bad Request
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        // Extract body as Map
        @SuppressWarnings("unchecked")
        Map<String, Object> body = response.getBody();
        // Assert that body is not null
        assertNotNull(body);
        // Assert that status field equals "INVALID_OR_NOT_FOUND"
        assertEquals("INVALID_OR_NOT_FOUND", body.get("status"));
    }

    // --------------------------- BMI DISTRIBUTION TEST ---------------------------

    // Test that getBmiDistribution returns 200 OK and a JSON map
    @Test
    public void getBmiDistribution_returnsOkWithMap() {
        // Build URL for GET request to BMI distribution endpoint
        var url = "/api/users/stats/bmiDistribution";

        // Perform GET request expecting Map body
        var response = this.restTemplate.getForEntity(url, Map.class);

        // Assert that HTTP status is 200 OK
        assertEquals(HttpStatus.OK, response.getStatusCode());
        // Extract body as Map
        @SuppressWarnings("unchecked")
        Map<String, Object> body = response.getBody();
        // Assert that body is not null (can be empty map, but not null)
        assertNotNull(body);
    }

    // --------------------------- CALORIES MODULE TESTS ---------------------------

    // Test that updateCalories and getCalories behave correctly for valid and invalid values
    @Test
    public void calories_updateAndGet_flowWithValidAndInvalidValues() throws Exception {
        // Build a unique username for this test
        var username = "caloriesOk_" + System.currentTimeMillis();
        // Create the user through the service layer
        createUserInFirebase(username, "p");

        // Build URL for initial GET request to calories endpoint
        var getInitialUrl = "/api/users/" + username + "/calories";
        // Perform initial GET request expecting Map body
        var initialResponse = this.restTemplate.getForEntity(getInitialUrl, Map.class);

        // Assert that HTTP status is 200 OK
        assertEquals(HttpStatus.OK, initialResponse.getStatusCode());
        // Extract initial body as Map
        @SuppressWarnings("unchecked")
        Map<String, Object> initialBody = initialResponse.getBody();
        // Assert that body is not null
        assertNotNull(initialBody);
        // Extract "calories" field as Number
        var initialCalories = (Number) initialBody.getOrDefault("calories", 0);
        // Assert that initial calories are 0
        assertEquals(0, initialCalories.intValue());

        // Build URL for valid PUT request to update calories to 1500
        var putValidUrl = "/api/users/" + username + "/calories?calories=1500";

        // Perform PUT request with valid value expecting no content
        var validResponse = this.restTemplate.exchange(
                putValidUrl,
                HttpMethod.PUT,
                null,
                Void.class
        );

        // Assert that HTTP status is 204 No Content
        assertEquals(HttpStatus.NO_CONTENT, validResponse.getStatusCode());

        // Perform GET request again to verify updated calories
        var afterValidResponse = this.restTemplate.getForEntity(getInitialUrl, Map.class);

        // Assert that HTTP status is 200 OK
        assertEquals(HttpStatus.OK, afterValidResponse.getStatusCode());
        // Extract body as Map
        @SuppressWarnings("unchecked")
        Map<String, Object> afterValidBody = afterValidResponse.getBody();
        // Assert that body is not null
        assertNotNull(afterValidBody);
        // Extract "calories" after valid update
        var afterValidCalories = (Number) afterValidBody.getOrDefault("calories", 0);
        // Assert that calories are now 1500
        assertEquals(1500, afterValidCalories.intValue());

        // Build URL for PUT request with invalid negative calories
        var putInvalidLowUrl = "/api/users/" + username + "/calories?calories=-10";

        // Perform PUT request expecting bad request
        var invalidLowResponse = this.restTemplate.exchange(
                putInvalidLowUrl,
                HttpMethod.PUT,
                null,
                Void.class
        );

        // Assert that HTTP status is 400 Bad Request
        assertEquals(HttpStatus.BAD_REQUEST, invalidLowResponse.getStatusCode());

        // Build URL for PUT request with excessively high invalid calories
        var putInvalidHighUrl = "/api/users/" + username + "/calories?calories=50000";

        // Perform PUT request expecting bad request again
        var invalidHighResponse = this.restTemplate.exchange(
                putInvalidHighUrl,
                HttpMethod.PUT,
                null,
                Void.class
        );

        // Assert that HTTP status is 400 Bad Request
        assertEquals(HttpStatus.BAD_REQUEST, invalidHighResponse.getStatusCode());

        // Perform GET request again to verify calories did not change after invalid updates
        var afterInvalidResponse = this.restTemplate.getForEntity(getInitialUrl, Map.class);

        // Assert that HTTP status is 200 OK
        assertEquals(HttpStatus.OK, afterInvalidResponse.getStatusCode());
        // Extract body as Map
        @SuppressWarnings("unchecked")
        Map<String, Object> afterInvalidBody = afterInvalidResponse.getBody();
        // Assert that body is not null
        assertNotNull(afterInvalidBody);
        // Extract "calories" after invalid updates
        var afterInvalidCalories = (Number) afterInvalidBody.getOrDefault("calories", 0);
        // Assert that calories are still 1500 (unchanged)
        assertEquals(1500, afterInvalidCalories.intValue());
    }
}