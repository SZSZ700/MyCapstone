@file:Suppress("PackageName")
package org.example.CapstoneProject.web
import jakarta.validation.Valid
import org.example.CapstoneProject.dto.*
import org.example.CapstoneProject.model.User
import org.example.CapstoneProject.service.*
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.concurrent.CompletableFuture

// -------------------------------------------------------------------------
// NOTE:
//
// thenApply is used because these continuations perform only light work,
// such as creating DTOs and wrapping results inside ResponseEntity.
//
// Database operations remain inside the service and repository layers.
// -------------------------------------------------------------------------

// -------------------------------------------------------------------------
// Marks this class as a REST controller.
//
// All endpoints in this controller use /api/users as their base path.
// -------------------------------------------------------------------------
@RestController
@RequestMapping("/api/users")
class UsersController(
    // Service for user-related business logic.
    private val userService: UserService,

    // Service for authentication operations such as signup and login.
    private val authenticationService: AuthenticationService,

    // Service for water-related business logic.
    private val waterService: WaterService,

    // Service for BMI and calorie-related business logic.
    private val userHealthService: UserHealthService,

    // Service for global statistical business logic.
    private val statisticsService: StatisticsService,

    // Service used to create JWT tokens after successful login.
    private val jwtService: JwtService
) {

    // ---------------------------------------------------------------------
    // HEALTH CHECK (GET /api/users/health)
    //
    // Simple endpoint used to verify that the server is running.
    // ---------------------------------------------------------------------
    @GetMapping("/health")
    fun health(): ResponseEntity<String> {
        return ResponseEntity.ok("OK")
    }

    // =========================================================
    // SIGNUP (POST /api/users/signup)
    // Android -> RestClient.register(user) -> here
    // =========================================================
    @PostMapping("/signup")
    fun signup(
        @Valid @RequestBody signupRequest: SignupRequest
    ): CompletableFuture<ResponseEntity<String>> {
        // Create a User model from the signup request data.
        val user = User()

        // Copy the signup data into the internal User model.
        user.userName = signupRequest.userName
        user.password = signupRequest.password
        user.fullName = signupRequest.fullName
        user.age = signupRequest.age

        // Ask AuthenticationService to create the user.
        return authenticationService.signup(user).thenApply { result ->
            when (result) {
                "User created successfully" -> {
                    // Return HTTP 201 when the user was created.
                    ResponseEntity
                        .status(HttpStatus.CREATED)
                        .body(result)
                }
                "Username already exists" -> {
                    // Return HTTP 409 when the username already exists.
                    ResponseEntity
                        .status(HttpStatus.CONFLICT)
                        .body(result)
                }
                else -> {
                    // Return HTTP 500 for any other signup result.
                    ResponseEntity
                        .status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(result)
                }
            }
        }
    }

    // =========================================================
    // LOGIN (POST /api/users/login)
    // Android -> RestClient.login(username, password) -> here
    // =========================================================
    @PostMapping("/login")
    fun login(
        @Valid @RequestBody loginRequest: LoginRequest
    ): CompletableFuture<ResponseEntity<*>> {
        // Extract the username and password from the request body.
        val username = loginRequest.userName
        val password = loginRequest.password

        // Validate the credentials through AuthenticationService.
        return authenticationService.login(username, password)
            .thenApply { user ->
                // Reject invalid credentials.
                if (user == null) {
                    ResponseEntity
                        .status(HttpStatus.UNAUTHORIZED)
                        .body("Invalid username or password")
                } else {
                    // Generate a JWT for the authenticated user.
                    val token = jwtService.generateToken(user.userName!!)

                    // Create the login response containing the JWT
                    // and public user information.
                    val response = LoginResponse(
                        token,
                        user.userName!!,
                        user.age,
                        user.fullName!!,
                        user.bmi
                    )

                    // Return HTTP 200 with the JWT and user information.
                    ResponseEntity.ok(response)
                }
            }
    }

    // ---------------------------------------------------------------------
    // GET ALL USERS (GET /api/users)
    //
    // Returns all users stored in the database without exposing passwords.
    // ---------------------------------------------------------------------
    @GetMapping
    fun getAllUsers(): CompletableFuture<ResponseEntity<List<UserResponse>>> {
        // Fetch all users from the service layer.
        return userService.getAllUsers().thenApply { users ->
            // Convert every internal User model into a public UserResponse DTO.
            val response = users.map { user ->
                UserResponse(
                    user.userName!!,
                    user.age,
                    user.fullName!!,
                    user.bmi
                )
            }

            // Return HTTP 200 with the user response list.
            ResponseEntity.ok(response)
        }
    }

    // ---------------------------------------------------------------------
    // GET USER BY USERNAME (GET /api/users/{username})
    //
    // Retrieves one user by username.
    // ---------------------------------------------------------------------
    @GetMapping("/{username}")
    fun getUser(
        @PathVariable("username") username: String
    ): CompletableFuture<ResponseEntity<*>> {
        // Fetch the requested user.
        return userService.getUser(username)
            .thenApply { user ->
                if (user == null) {
                    // Return HTTP 404 when no matching user exists.
                    ResponseEntity
                        .status(HttpStatus.NOT_FOUND)
                        .body("User not found")
                } else {
                    // Create a public response without exposing the password.
                    val response = UserResponse(
                        user.userName!!,
                        user.age,
                        user.fullName!!,
                        user.bmi
                    )

                    // Return HTTP 200 with the user response.
                    ResponseEntity.ok(response)
                }
            }
    }

    // ---------------------------------------------------------------------
    // PATCH USER (PATCH /api/users/{username})
    //
    // Performs a partial update of supported user fields.
    // ---------------------------------------------------------------------
    @PatchMapping("/{username}")
    fun patchUser(
        @PathVariable("username") username: String,
        @RequestBody updates: MutableMap<String, Any>
    ): CompletableFuture<ResponseEntity<*>> {
        // Send the requested updates to the service layer.
        return userService.patchUser(username, updates)
            .thenApply { updatedUser ->
                if (updatedUser == null) {
                    // Return HTTP 404 when the user was not found.
                    ResponseEntity
                        .status(HttpStatus.NOT_FOUND)
                        .body("User not found")
                } else {
                    // Create a response DTO from the updated user.
                    val response = UserResponse(
                        updatedUser.userName!!,
                        updatedUser.age,
                        updatedUser.fullName!!,
                        updatedUser.bmi
                    )

                    // Return HTTP 200 with the updated user.
                    ResponseEntity.ok(response)
                }
            }
    }

    // ---------------------------------------------------------------------
    // DELETE USER (DELETE /api/users/{username})
    //
    // Deletes the user and related data through the service layer.
    // ---------------------------------------------------------------------
    @DeleteMapping("/{username}")
    fun deleteUser(
        @PathVariable("username") username: String
    ): CompletableFuture<ResponseEntity<*>> {
        return userService.deleteUser(username)
            .thenApply { success ->
                if (!success) {
                    // Return HTTP 404 when the user was not found.
                    ResponseEntity
                        .status(HttpStatus.NOT_FOUND)
                        .body("User not found")
                } else {
                    // Return HTTP 200 when deletion succeeded.
                    ResponseEntity.ok("User deleted")
                }
            }
    }

    // ---------------------------------------------------------------------
    // HEAD USER (HEAD /api/users/{username})
    //
    // Checks whether a user exists without returning a response body.
    // ---------------------------------------------------------------------
    @RequestMapping(
        value = ["/{username}"],
        method = [RequestMethod.HEAD]
    )
    fun headUser(
        @PathVariable("username") username: String
    ): CompletableFuture<ResponseEntity<Void>> {
        // Check whether the user exists.
        return userService.exists(username).thenApply { exists ->
            if (exists) {
                // Return HTTP 200 when the user exists.
                ResponseEntity.ok().build()
            } else {
                // Return HTTP 404 when the user does not exist.
                ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .build()
            }
        }
    }

    // ---------------------------------------------------------------------
    // UPDATE BMI (PATCH /api/users/{username}/bmi?bmi=...)
    //
    // Updates the BMI value of a user.
    // ---------------------------------------------------------------------
    @PatchMapping("/{username}/bmi")
    fun updateBmi(
        @PathVariable("username") username: String,
        @RequestParam("bmi") bmi: Double
    ): CompletableFuture<ResponseEntity<*>> {
        // Update the user's BMI through the service layer.
        return userHealthService.updateBmi(username, bmi)
            .thenApply { success ->
                if (!success) {
                    // Return HTTP 404 when the user was not found.
                    ResponseEntity
                        .status(HttpStatus.NOT_FOUND)
                        .body("User not found")
                } else {
                    // Return HTTP 200 when the update succeeded.
                    ResponseEntity.ok("BMI updated successfully")
                }
            }
    }

    // ---------------------------------------------------------------------
    // UPDATE WATER (PATCH /api/users/{username}/water?amount=...)
    //
    // Adds one water entry for the user.
    // ---------------------------------------------------------------------
    @PatchMapping("/{username}/water")
    fun updateWater(
        @PathVariable("username") username: String,
        @RequestParam("amount") amount: Int
    ): CompletableFuture<ResponseEntity<*>> {
        // Add the water amount through the service layer.
        return waterService.updateWater(username, amount)
            .thenApply { success ->
                if (!success) {
                    // Return HTTP 404 when the update cannot be performed.
                    ResponseEntity
                        .status(HttpStatus.NOT_FOUND)
                        .body("User not found or error")
                } else {
                    // Return HTTP 200 when the update succeeded.
                    ResponseEntity.ok("Water updated successfully")
                }
            }
    }

    // ---------------------------------------------------------------------
    // GET WATER (GET /api/users/{username}/water)
    //
    // Returns today's and yesterday's water totals.
    // ---------------------------------------------------------------------
    @GetMapping("/{username}/water")
    fun getWater(
        @PathVariable("username") username: String
    ): CompletableFuture<ResponseEntity<*>> {
        // Fetch today's and yesterday's water totals.
        return waterService.getWater(username)
            .thenApply { result ->
                if (result == null) {
                    // Return HTTP 404 when the user was not found.
                    ResponseEntity
                        .status(HttpStatus.NOT_FOUND)
                        .body("User not found")
                } else {
                    // Create a response DTO from the returned water data.
                    val response = WaterResponse(
                        result.optLong("todayWater", 0),
                        result.optLong("yesterdayWater", 0)
                    )

                    // Return HTTP 200 with the water response.
                    ResponseEntity.ok(response)
                }
            }
    }

    // ---------------------------------------------------------------------
    // GET WATER HISTORY MAP
    // GET /api/users/{username}/waterHistoryMap?days=7
    //
    // Example:
    // {"2025-09-29":4600, "2025-09-28":0}
    // ---------------------------------------------------------------------
    @GetMapping("/{username}/waterHistoryMap")
    fun getWaterHistoryMap(
        @PathVariable("username") username: String,
        @RequestParam(name = "days", defaultValue = "7") days: Int
    ): CompletableFuture<ResponseEntity<*>> {
        // Fetch the requested water history.
        return waterService.getWaterHistoryMap(username, days)
            .thenApply { result ->
                if (result == null) {
                    // Return HTTP 404 when the user was not found.
                    ResponseEntity
                        .status(HttpStatus.NOT_FOUND)
                        .body("User not found")
                } else {
                    // Return HTTP 200 with the history map.
                    ResponseEntity.ok(result)
                }
            }
    }

    // ---------------------------------------------------------------------
    // GET /api/users/{username}/weeklyAverages
    //
    // Returns a JSON map with day labels as keys and averages as values.
    //
    // Example:
    // {"Mon":6200, "Tue":2740}
    // ---------------------------------------------------------------------
    @GetMapping("/{username}/weeklyAverages")
    fun getWeeklyAverages(
        @PathVariable("username") username: String
    ): CompletableFuture<ResponseEntity<Map<String, Int>>> {
        // Fetch the weekly averages.
        return waterService.getWeeklyAverages(username).thenApply { result ->
            if (result.isEmpty()) {
                // Return HTTP 404 with an empty map.
                ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(emptyMap())
            } else {
                // Return HTTP 200 with the result.
                ResponseEntity.ok(result)
            }
        }
    }

    // ---------------------------------------------------------------------
    // GET /api/users/{username}/goal
    //
    // Returns JSON:
    // {"goalMl":2600}
    // ---------------------------------------------------------------------
    @GetMapping("/{username}/goal")
    fun getGoal(
        @PathVariable("username") username: String
    ): CompletableFuture<ResponseEntity<GoalResponse>> {
        // Fetch the user's current water goal.
        return waterService.getGoalMl(username)
            .thenApply { goal ->
                // Create the goal response DTO.
                val response = GoalResponse(goal)

                // Return HTTP 200 with the current goal.
                ResponseEntity.ok(response)
            }
            .exceptionally { ex ->
                // Return HTTP 500 when an unexpected failure occurs.
                System.err.println("ERROR getGoal -> ${ex.message}")

                ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(null)
            }
    }

    // ---------------------------------------------------------------------
    // PUT /api/users/{username}/goal?goalMl=2600
    //
    // Updates the user's daily water goal.
    // ---------------------------------------------------------------------
    @PutMapping("/{username}/goal")
    fun setGoal(
        @PathVariable("username") username: String,
        @RequestParam("goalMl") goalMl: Int
    ): CompletableFuture<ResponseEntity<GoalUpdateResponse>> {
        // Update the user's water goal.
        return waterService.updateGoalMl(username, goalMl)
            .thenApply { success ->
                if (success) {
                    // Return HTTP 200 when the update succeeded.
                    ResponseEntity.ok(
                        GoalUpdateResponse("OK")
                    )
                } else {
                    // Return HTTP 400 when the value is invalid
                    // or the user was not found.
                    ResponseEntity
                        .badRequest()
                        .body(
                            GoalUpdateResponse("INVALID_OR_NOT_FOUND")
                        )
                }
            }
            .exceptionally { ex ->
                // Return HTTP 500 when an unexpected failure occurs.
                System.err.println("ERROR setGoal -> ${ex.message}")

                ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(
                        GoalUpdateResponse("ERROR")
                    )
            }
    }

    // ---------------------------------------------------------------------
    // BMI DISTRIBUTION (GET /api/users/stats/bmiDistribution)
    //
    // Returns the number of users in each BMI category.
    //
    // Example:
    // {
    //   "Underweight": 3,
    //   "Normal": 12,
    //   "Overweight": 5,
    //   "Obese": 2
    // }
    // ---------------------------------------------------------------------
    @GetMapping("/stats/bmiDistribution")
    fun getBmiDistribution(): CompletableFuture<ResponseEntity<Map<String, Int>>> {
        // Fetch the global BMI distribution.
        return statisticsService.getBmiDistribution()
            .thenApply { result ->
                // Return HTTP 200 with the distribution.
                ResponseEntity.ok(result)
            }
            .exceptionally { ex ->
                // Return HTTP 500 with an empty map on failure.
                System.err.println(
                    "ERROR UsersController.getBmiDistribution -> ${ex.message}"
                )

                ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(emptyMap())
            }
    }

    // ---------------------------------------------------------------------
    // GET CALORIES (GET /api/users/{username}/calories)
    //
    // Returns JSON:
    // {"calories":1800}
    // ---------------------------------------------------------------------
    @GetMapping("/{username}/calories")
    fun getCalories(
        @PathVariable("username") username: String
    ): CompletableFuture<ResponseEntity<CaloriesResponse>> {
        // Fetch the user's calories value.
        return userHealthService.getCalories(username)
            .thenApply { cals: Int? ->
                // Preserve the original fallback to zero.
                val response = CaloriesResponse(cals ?: 0)

                // Return HTTP 200 with the calories response.
                ResponseEntity.ok(response)
            }
    }

    // ---------------------------------------------------------------------
    // UPDATE CALORIES
    // PUT /api/users/{username}/calories?calories=1800
    //
    // Updates today's calories value for the user.
    // ---------------------------------------------------------------------
    @PutMapping("/{username}/calories")
    fun updateCalories(
        @PathVariable("username") username: String,
        @RequestParam("calories") calories: Int
    ): CompletableFuture<ResponseEntity<Void>> {
        return userHealthService.updateCalories(username, calories)
            .thenApply { success ->
                if (success) {
                    // Return HTTP 204 No Content when the update succeeds.
                    ResponseEntity
                        .noContent()
                        .build()
                } else {
                    // Return HTTP 400 when the value is invalid
                    // or the user was not found.
                    ResponseEntity
                        .badRequest()
                        .build()
                }
            }
    }
}