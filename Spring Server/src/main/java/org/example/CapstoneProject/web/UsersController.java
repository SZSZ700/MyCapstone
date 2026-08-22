// Define the package where this controller belongs
package org.example.CapstoneProject.web;
// Import the User model (POJO with username, password, age, fullName)
import org.example.CapstoneProject.dto.LoginRequest;
import org.example.CapstoneProject.dto.SignupRequest;
import org.example.CapstoneProject.dto.UserResponse;
import org.example.CapstoneProject.dto.UpdateUserRequest;
import org.example.CapstoneProject.dto.WaterResponse;
import org.example.CapstoneProject.dto.GoalResponse;
import org.example.CapstoneProject.dto.CaloriesResponse;
import org.example.CapstoneProject.dto.GoalUpdateResponse;
import org.example.CapstoneProject.model.User;
// Import the Firebase service that handles database operations
import org.example.CapstoneProject.service.*;
// Import Spring framework classes for HTTP status and response handling
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
// Import Java utility classes
import java.util.*;
import java.util.concurrent.CompletableFuture;   // For async non-blocking calls
import java.util.stream.Collectors;
import jakarta.validation.Valid;
import org.example.CapstoneProject.dto.LoginResponse;


// NOTE:
// I use thenApply (not thenApplyAsync) because this continuation is very light:
// it only wraps the service result into a ResponseEntity.
// thenApply runs on the same thread that completes the Future, which is fine here,
// and we don't need an extra thread-pool hop.

// If I would do heavy work inside the Firebase callback
// (or inside thenApply that runs on the same thread), I can block Firebase’s thread(s).

// When Firebase’s thread pool gets busy / blocked
// (e.g., too many callbacks waiting, or callbacks doing long computations / I/O)
// I may get: slow responses, delays, sometimes timeouts
// It’s not my own thread that I'm blocking
// — I'm blocking a shared Firebase callback thread that serves many callbacks
// across the app

// -------------------------------------------------------------------------
// Marks this class as a REST controller → all methods return JSON by default
// -------------------------------------------------------------------------
@RestController
// Base URL for all endpoints in this controller
@RequestMapping("/api/users")
public class UsersController {
    // ---------------------------------------------------------------------
    // Reference to UserService for user-related business logic.
    // ---------------------------------------------------------------------
    private final UserService userService;

    // ---------------------------------------------------------------------
    // Reference to AuthenticationService for authentication-related
    // business logic such as signup and login.
    // ---------------------------------------------------------------------
    private final AuthenticationService authenticationService;

    // ---------------------------------------------------------------------
    // Reference to WaterService for water-related business logic.
    // ---------------------------------------------------------------------
    private final WaterService waterService;

    // ---------------------------------------------------------------------
    // Reference to UserHealthService for BMI and calorie-related
    // business logic.
    // ---------------------------------------------------------------------
    private final UserHealthService userHealthService;

    // ---------------------------------------------------------------------
    // Reference to StatisticsService for global statistical business logic.
    // ---------------------------------------------------------------------
    private final StatisticsService statisticsService;

    // ---------------------------------------------------------------------
    // Reference to JwtService for creating JWT tokens after successful login.
    // ---------------------------------------------------------------------
    private final JwtService jwtService;


    // ---------------------------------------------------------------------
    // Constructor for dependency injection.
    //
    // Spring automatically injects the required services.
    // ---------------------------------------------------------------------
    public UsersController(
            UserService userService,
            AuthenticationService authenticationService,
            WaterService waterService,
            UserHealthService userHealthService,
            StatisticsService statisticsService,
            JwtService jwtService) {

        // Store the user service reference.
        this.userService = userService;

        // Store the authentication service reference.
        this.authenticationService = authenticationService;

        // Store the water service reference.
        this.waterService = waterService;

        // Store the user health service reference.
        this.userHealthService = userHealthService;

        // store the statistics service reference.
        this.statisticsService = statisticsService;

        // Store the JWT service reference.
        this.jwtService = jwtService;
    }

    // ---------------------------------------------------------------------
    // HEALTH CHECK (GET /api/users/health)
    // Simple endpoint to verify the server is alive
    // ---------------------------------------------------------------------
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("OK");
    }


    // =========================================================
    // SIGNUP (POST /api/users/signup)
    // Android → RestClient.register(user) → here
    // =========================================================
    @PostMapping("/signup")
    public CompletableFuture<ResponseEntity<String>> signup(@Valid @RequestBody SignupRequest signupRequest) {
        // Create a User model from the signup request data.
        var user = new User();
        // Copy the username from the request.
        user.setUserName(signupRequest.getUserName());
        // Copy the password from the request.
        user.setPassword(signupRequest.getPassword());
        // Copy the full name from the request.
        user.setFullName(signupRequest.getFullName());
        // Copy the age from the request.
        user.setAge(signupRequest.getAge());

        // Call authenticationService.signup() which checks username and creates user.
        return authenticationService.signup(user).thenApply(result -> {
            if ("User created successfully".equals(result)) {
                // Return HTTP 201 if success.
                return ResponseEntity
                        .status(HttpStatus.CREATED)
                        .body(result);
            } else if ("Username already exists".equals(result)) {
                // Return HTTP 409 if username exists.
                return ResponseEntity
                        .status(HttpStatus.CONFLICT)
                        .body(result);
            } else {
                // Return HTTP 500 for generic errors.
                return ResponseEntity
                        .status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(result);
            }
        });
    }

    // =========================================================
    // LOGIN (POST /api/users/login)
    // Android → RestClient.login(username,password) → here
    // =========================================================
    @PostMapping("/login")
    public CompletableFuture<ResponseEntity<?>> login(@Valid @RequestBody LoginRequest loginRequest) {
        // Extract username and password from the request body.
        var username = loginRequest.getUserName();
        var password = loginRequest.getPassword();

        // Validate the credentials through AuthenticationService.
        return authenticationService.login(username, password).thenApply(user -> {
            // Reject invalid credentials.
            if (user == null) {
                return ResponseEntity
                        .status(HttpStatus.UNAUTHORIZED)
                        .body("Invalid username or password");
            }

            // Generate a JWT for the authenticated user.
            String token = jwtService.generateToken(user.getUserName());

            // Create the login response containing the JWT and public user data.
            var response = new LoginResponse(
                    token,
                    user.getUserName(),
                    user.getAge(),
                    user.getFullName(),
                    user.getBmi()
            );

            // Return HTTP 200 with the JWT and public user information.
            return ResponseEntity.ok(response);
        });
    }

    // ---------------------------------------------------------------------
    // GET ALL USERS (GET /api/users)
    // Returns a list of all users stored in Firebase
    // ---------------------------------------------------------------------
    @GetMapping
    public CompletableFuture<ResponseEntity<List<UserResponse>>> getAllUsers() {

        // Call UserService to fetch all users.
        return userService.getAllUsers().thenApply(users -> {

            // Convert each internal User model into a UserResponse DTO.
            var response = users.stream()
                    .map(user -> new UserResponse(
                            user.getUserName(),
                            user.getAge(),
                            user.getFullName(),
                            user.getBmi()
                    ))
                    .collect(Collectors.toList());

            // Return HTTP 200 with the user response list.
            return ResponseEntity.ok(response);
        });
    }

    // ---------------------------------------------------------------------
    // GET USER BY USERNAME (GET /api/users/{username})
    // Retrieves a single user by username
    // ---------------------------------------------------------------------
    @GetMapping("/{username}")
    public CompletableFuture<ResponseEntity<?>> getUser(
            @PathVariable("username") String username) {

        // Call UserService to fetch a specific user.
        return userService.getUser(username).thenApply(user -> {
            // If user not found, return 404 response.
            if (user == null) {
                return ResponseEntity
                        .status(HttpStatus.NOT_FOUND)
                        .body("User not found");
            }

            // Create a response DTO from the internal User model.
            var response = new UserResponse(
                    user.getUserName(),
                    user.getAge(),
                    user.getFullName(),
                    user.getBmi()
            );

            // Return HTTP 200 with the user response DTO.
            return ResponseEntity.ok(response);
        });
    }

    // ---------------------------------------------------------------------
    // UPDATE USER (PUT /api/users/{username})
    //
    // Updates the user's editable fields while preserving existing
    // health and water-related data.
    // ---------------------------------------------------------------------
    @PutMapping("/{username}")
    public CompletableFuture<ResponseEntity<?>> updateUser(
            @PathVariable("username") String username,
            @Valid @RequestBody UpdateUserRequest updateRequest) {

        // Create a User model from the update request data.
        var updatedUser = new User();

        // Keep the username from the path parameter.
        updatedUser.setUserName(username);

        // Copy the password from the request.
        updatedUser.setPassword(updateRequest.getPassword());

        // Copy the full name from the request.
        updatedUser.setFullName(updateRequest.getFullName());

        // Copy the age from the request.
        updatedUser.setAge(updateRequest.getAge());

        // Update the user and receive the complete updated user back.
        return userService.updateUser(username, updatedUser)
                .thenApply(savedUser -> {

                    // If no matching user exists, return 404.
                    if (savedUser == null) {
                        return ResponseEntity
                                .status(HttpStatus.NOT_FOUND)
                                .body("User not found");
                    }

                    // Create the response DTO from the complete updated user.
                    var response = new UserResponse(
                            savedUser.getUserName(),
                            savedUser.getAge(),
                            savedUser.getFullName(),
                            savedUser.getBmi()
                    );

                    // Return HTTP 200 with the updated user response.
                    return ResponseEntity.ok(response);
                });
    }

    // ---------------------------------------------------------------------
    // PATCH USER (PATCH /api/users/{username})
    // Performs a partial update of user fields
    // ---------------------------------------------------------------------
    @PatchMapping("/{username}")
    public CompletableFuture<ResponseEntity<?>> patchUser(
            @PathVariable("username") String username,
            @RequestBody Map<String, Object> updates) {
        // Call Firebase service to patch user fields
        return userService.patchUser(username, updates).thenApply(updatedUser -> {
            if (updatedUser == null) {
                // If user not found, return 404.
                return ResponseEntity
                        .status(HttpStatus.NOT_FOUND)
                        .body("User not found");
            }

            // Create a response DTO from the updated User model.
            var response = new UserResponse(
                    updatedUser.getUserName(),
                    updatedUser.getAge(),
                    updatedUser.getFullName(),
                    updatedUser.getBmi()
            );

            // Return HTTP 200 with the user response DTO.
            return ResponseEntity.ok(response);
        });
    }

    // ---------------------------------------------------------------------
    // DELETE USER (DELETE /api/users/{username})
    // Removes a user from Firebase
    // ---------------------------------------------------------------------
    @DeleteMapping("/{username}")
    public CompletableFuture<ResponseEntity<?>> deleteUser(@PathVariable("username") String username) {
        return userService.deleteUser(username).thenApply(success -> {
            if (!success) {
                return ResponseEntity
                        .status(HttpStatus.NOT_FOUND)
                        .body("User not found");
            }
            return ResponseEntity.ok("User deleted");
        });
    }

    // ---------------------------------------------------------------------
    // HEAD USER (HEAD /api/users/{username})
    // Checks if a user exists (status code only, no response body)
    // ---------------------------------------------------------------------
    @RequestMapping(value = "/{username}", method = RequestMethod.HEAD)
    public CompletableFuture<ResponseEntity<Void>> headUser(@PathVariable("username") String username) {
        // Call Firebase service to check if user exists
        return userService.exists(username).thenApply(exists -> {
            if (exists) {
                // Return 200 OK if user exists
                return ResponseEntity.ok().build();
            }
            // Return 404 if user not found
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        });
    }

    // ---------------------------------------------------------------------
    // UPDATE BMI (PATCH /api/users/{username}/bmi?bmi=...)
    // Updates the "bmi" field of a user
    // ---------------------------------------------------------------------
    @PatchMapping("/{username}/bmi")
    public CompletableFuture<ResponseEntity<?>> updateBmi(
            @PathVariable("username") String username,
            @RequestParam("bmi") double bmi) {
        // Call Firebase service to update BMI
        return userHealthService.updateBmi(username, bmi).thenApply(success -> {
            if (!success) {
                // If user not found, return 404 with error message
                return ResponseEntity
                        .status(HttpStatus.NOT_FOUND)
                        .body("User not found");
            }
            // If success, return 200 OK with confirmation message
            return ResponseEntity.ok("BMI updated successfully");
        });
    }

    // ---------------------------------------------------------------------
    // UPDATE WATER (PATCH /api/users/{username}/water?amount=...)
    // Adds a water entry for today in the user's waterLog
    // ---------------------------------------------------------------------
    @PatchMapping("/{username}/water")
    public CompletableFuture<ResponseEntity<?>> updateWater(
            @PathVariable("username") String username,
            @RequestParam("amount") int amount) {
        // Call Firebase service to update water log
        return waterService.updateWater(username, amount).thenApply(success -> {
            if (!success) {
                // If user not found or slots are full, return 404 with error message
                return ResponseEntity
                        .status(HttpStatus.NOT_FOUND)
                        .body("User not found or error");
            }
            // If success, return 200 OK with confirmation message
            return ResponseEntity.ok("Water updated successfully");
        });
    }


    // ---------------------------------------------------------------------
    // GET WATER (GET /api/users/{username}/water)
    //
    // Returns today's and yesterday's water totals.
    // ---------------------------------------------------------------------
    @GetMapping("/{username}/water")
    public CompletableFuture<ResponseEntity<?>> getWater(
            @PathVariable("username") String username) {

        // Call WaterService to get today's and yesterday's water amounts.
        return waterService.getWater(username).thenApply(result -> {
            // If user not found, return 404.
            if (result == null) {
                return ResponseEntity
                        .status(HttpStatus.NOT_FOUND)
                        .body("User not found");
            }

            // Create a response DTO from the returned water data.
            var response = new WaterResponse(
                    result.optLong("todayWater", 0),
                    result.optLong("yesterdayWater", 0)
            );

            // Return HTTP 200 with the water response DTO.
            return ResponseEntity.ok(response);
        });
    }

    // ---------------------------------------------------------------------
    // GET WATER HISTORY MAP (GET /api/users/{username}/waterHistoryMap?days=7)
    // Returns JSON object like: {"2025-09-29":4600, "2025-09-28":0, ...}
    // ---------------------------------------------------------------------
    @GetMapping("/{username}/waterHistoryMap")
    public CompletableFuture<ResponseEntity<?>> getWaterHistoryMap(
            @PathVariable("username") String username,
            @RequestParam(name = "days", defaultValue = "7") int days) {
        // Call Firebase service to get the water history map
        return waterService.getWaterHistoryMap(username, days).thenApply(result -> {
                    // If user not found, return 404 with error message
                    if (result == null) {
                        return ResponseEntity
                                .status(HttpStatus.NOT_FOUND)
                                .body("User not found");
                    }

                    // Return 200 OK with the history map
                    return ResponseEntity.ok(result);
                });
    }

    // -------------------------------------------------------------
    // GET /api/users/{username}/weeklyAverages
    // Returns a JSON map with day labels as keys and averages as values
    // Example: {"Mon": 6200, "Tue": 2740, ...}
    // -------------------------------------------------------------
    // UsersController
    @GetMapping("/{username}/weeklyAverages")
    public CompletableFuture<ResponseEntity<Map<String, Integer>>> getWeeklyAverages(
            @PathVariable("username") String username) {
        // Call Firebase service to get weekly averages
        // Initially returns an empty FUTURE (container for async result)
        return waterService.getWeeklyAverages(username)
                .thenApply(result -> {
                    // If result is null or empty, return 404 with an empty map
                    if (result == null || result.isEmpty()) {
                        // ResponseEntity is returned asynchronously once Future is completed
                        return ResponseEntity
                                .status(HttpStatus.NOT_FOUND)
                                .body(Collections.emptyMap());
                    }

                    // Return 200 OK with the result map
                    return ResponseEntity.ok(result);
                });

        // Execution is placed into Spring's internal async waiting queue
    }


    // -------------------------------------------
    // GET /api/users/{username}/goal
    // Returns JSON: {"goalMl": 2600}
    // -------------------------------------------
    @GetMapping("/{username}/goal")
    public CompletableFuture<ResponseEntity<GoalResponse>> getGoal(
            @PathVariable("username") String username) {

        // Call WaterService to get the user's water goal.
        return waterService.getGoalMl(username)
                .thenApply(goal -> {

                    // Create a response DTO with the goal value.
                    var response = new GoalResponse(goal);

                    // Return HTTP 200 with the goal response DTO.
                    return ResponseEntity.ok(response);
                })
                .exceptionally(ex -> {
                    // On failure return 500 Internal Server Error.
                    System.err.println("ERROR getGoal -> " + ex.getMessage());

                    return ResponseEntity
                            .status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .body(null);
                });
    }

    // ------------------------------------------------------
    // PUT /api/users/{username}/goal?goalMl=2600  -> 200/400
    // ------------------------------------------------------
    @PutMapping("/{username}/goal")
    public CompletableFuture<ResponseEntity<GoalUpdateResponse>> setGoal(
            @PathVariable("username") String username,
            @RequestParam("goalMl") int goalMl) {

        // Call WaterService to update the user's water goal.
        return waterService.updateGoalMl(username, goalMl)
                .thenApply(ok -> {
                    if (ok) {
                        // Return HTTP 200 with success status.
                        return ResponseEntity.ok(
                                new GoalUpdateResponse("OK")
                        );
                    }

                    // Return HTTP 400 if the value is invalid or user was not found.
                    return ResponseEntity
                            .badRequest()
                            .body(
                                    new GoalUpdateResponse(
                                            "INVALID_OR_NOT_FOUND"
                                    )
                            );
                })
                .exceptionally(ex -> {
                    // On exception return 500 Internal Server Error.
                    System.err.println(
                            "ERROR setGoal -> " + ex.getMessage()
                    );

                    return ResponseEntity
                            .status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .body(
                                    new GoalUpdateResponse("ERROR")
                            );
                });
    }

    // ---------------------------------------------------------------------
    // BMI DISTRIBUTION (GET /api/users/stats/bmiDistribution)
    // Returns aggregated statistics of how many users fall into each BMI category.
    // Example response:
    // {
    //   "Underweight": 3,
    //   "Normal": 12,
    //   "Overweight": 5,
    //   "Obese": 2
    // }
    // ---------------------------------------------------------------------
    @GetMapping("/stats/bmiDistribution")
    public CompletableFuture<ResponseEntity<Map<String, Integer>>> getBmiDistribution() {
        // Call Firebase service to calculate BMI distribution
        return statisticsService.getBmiDistribution()
                .thenApply(result -> {
                    // Never null – always at least empty map
                    //noinspection Convert2MethodRef
                    return ResponseEntity.ok(result);
                })
                .exceptionally(ex -> {
                    // On failure -> 500 Internal Server Error
                    System.err.println("ERROR UsersController.getBmiDistribution -> " + ex.getMessage());

                    return ResponseEntity
                            .status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .body(Collections.emptyMap());
                });
    }

    // -------------------------------- GET CALORIES ----------------------------
    // Returns JSON: {"calories": 1800}
    @GetMapping("/{username}/calories")
    public CompletableFuture<ResponseEntity<CaloriesResponse>> getCalories(
            @PathVariable("username") String username) {

        // Call UserHealthService to get the user's calories value.
        return userHealthService.getCalories(username)
                .thenApply(cals -> {

                    // Create a response DTO with the calories value.
                    var response = new CaloriesResponse(
                            cals != null ? cals : 0
                    );

                    // Return HTTP 200 with the calories response DTO.
                    return ResponseEntity.ok(response);
                });
    }

    // -------------------------------- UPDATE CALORIES -------------------------
    // Updates the "calories" field for this user.
    // Example call: PUT /api/users/john/calories?calories=1800
    @PutMapping("/{username}/calories")
    public CompletableFuture<ResponseEntity<Void>> updateCalories(
            @PathVariable("username") String username,
            @RequestParam("calories") int calories) {

        return userHealthService.updateCalories(username, calories)
                .thenApply(success -> {
                    if (success) {
                        // 204 No Content on success
                        return ResponseEntity.noContent().build();
                    } else {
                        // If invalid value or user not found → 400 Bad Request
                        return ResponseEntity.badRequest().build();
                    }
                });
    }
}

