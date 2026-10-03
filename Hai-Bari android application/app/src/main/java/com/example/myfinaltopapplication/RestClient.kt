package com.example.myfinaltopapplication
// Import Android logging.
import android.util.Log
// Import OkHttp classes for HTTP requests and responses.
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okio.Buffer
import org.json.JSONObject
// Import exception handling for input/output operations.
import java.io.IOException
import java.util.Collections
import java.util.LinkedHashMap
// Import CompletableFuture for asynchronous programming.
import java.util.concurrent.CompletableFuture

/**
 * RestClient - class that manages communication between
 * the Android app and the Spring Boot backend server.
 * Provides methods for user registration, login, update, delete,
 * BMI management, and water tracking.
 */
@Suppress("unused")
object RestClient {
    // Base URL of the backend server (10.0.2.2 = localhost for Android emulator).
    private const val BASE_URL = "https://10.0.2.2:8443/myapp/api/users"

    // Define JSON MediaType for sending JSON data.
    private val JSON: MediaType = "application/json; charset=utf-8".toMediaType()

    // JWT returned by the server after a successful login.
    //
    // The token is used for authenticated requests.
    // It is never the user's password.
    private var storedAuthToken: String? = null

    // ---------------------------------------------------------------------
    // Returns the JWT currently stored by RestClient.
    // ---------------------------------------------------------------------
    @JvmStatic
    fun getAuthToken(): String? {
        return storedAuthToken
    }

    // ---------------------------------------------------------------------
    // Restores a previously saved JWT.
    //
    // This is useful when the Android application is recreated and the token
    // was previously stored in SharedPreferences.
    // ---------------------------------------------------------------------
    @JvmStatic
    fun setAuthToken(token: String?) {
        storedAuthToken = token
    }

    // ---------------------------------------------------------------------
    // Removes the current JWT.
    //
    // This should be called when the user logs out.
    // ---------------------------------------------------------------------
    @JvmStatic
    fun clearAuthToken() {
        storedAuthToken = null
    }

    // Reusable HTTP client for all network requests.
    // Built with an interceptor that prints detailed logs for each request and response.
    private var client = OkHttpClient.Builder()
        .addInterceptor { chain ->
            // Capture the outgoing request.
            val request = chain.request()

            // Start timer for performance measurement.
            val t1 = System.nanoTime()
            // Log request details.
            Log.d("HTTP", "➡️ Sending ${request.method} request to ${request.url}")

            // Get the request body.
            val requestBody = request.body

            // If request has a body, log its contents.
            if (requestBody != null) {
                // Create a buffer to capture the request body.
                val buffer = Buffer()
                // Write the request body to the buffer.
                requestBody.writeTo(buffer)
                // Log the captured request body.
                Log.d("HTTP", "📤 Request body: ${buffer.readUtf8()}")
            }

            // Proceed with the request and capture the response.
            val response = chain.proceed(request)

            // End timer for performance measurement.
            val t2 = System.nanoTime()
            // Log response details.
            Log.d("HTTP", "⬅️ Received response for ${response.request.url} in ${(t2 - t1) / 1e6}ms, code = ${response.code}")

            // Print response body (peek to avoid consuming original stream).
            val responseBody = response.peekBody(Long.MAX_VALUE)
            // Log response body.
            Log.d("HTTP", "📥 Response body: ${responseBody.string()}")

            // Return the response so the client can use it.
            response
        }
        .build()

    // =========================================================
    // REGISTER (POST /api/users/signup)
    // =========================================================
    @JvmStatic
    fun register(user: User): CompletableFuture<Boolean> {
        // Create a CompletableFuture to return the result asynchronously.
        val future = CompletableFuture<Boolean>()

        try {
            // Build JSON object with user details.
            val json = JSONObject()
            // Add username.
            json.put("userName", user.userName)
            // Add password.
            json.put("password", user.password)
            // Add full name.
            json.put("fullName", user.fullName)
            // Add age.
            json.put("age", user.age)

            // Create HTTP request body from JSON.
            val body = json.toString().toRequestBody(JSON)

            // Build POST request for /signup endpoint.
            val request = Request.Builder()
                .url("$BASE_URL/signup")
                .post(body)
                .build()

            // Send request asynchronously.
            client.newCall(request).enqueue(callbackBoolean(future))
        } catch (e: Exception) {
            // If building JSON fails, complete with false.
            future.complete(false)
        }

        // Return CompletableFuture.
        return future
    }

    // =========================================================
    // LOGIN (POST /api/users/login)
    // =========================================================
    @JvmStatic
    fun login(username: String, password: String): CompletableFuture<User?> {
        // Future object that will complete with User if login succeeds.
        val future = CompletableFuture<User?>()

        try {
            // Build JSON object with the login credentials.
            val json = JSONObject()

            // Add the username to the request body.
            json.put("userName", username)

            // Add the raw password to the request body.
            // The password is still required for authentication,
            // but it will no longer be expected in the server response.
            json.put("password", password)

            // Create the HTTP request body from the JSON object.
            val body = json.toString().toRequestBody(JSON)

            // Build the POST request for the login endpoint.
            val request = Request.Builder()
                .url("$BASE_URL/login")
                .post(body)
                .build()

            // Send the request asynchronously.
            client.newCall(request).enqueue(object : Callback {

                override fun onFailure(call: Call, e: IOException) {
                    // Log the network error.
                    Log.e("HTTP", "❌ LOGIN request failed: ${e.message}")

                    // Complete with null when the request fails.
                    future.complete(null)
                }

                override fun onResponse(call: Call, response: Response) {
                    // Read the response body as a string.
                    val responseBody = response.body.string()

                    // Log the HTTP response code.
                    Log.d("HTTP", "⬅️ LOGIN response code: ${response.code}")

                    // Log the response body.
                    Log.d("HTTP", "⬅️ LOGIN response body: $responseBody")

                    // Continue only when the server returned a successful response.
                    if (response.isSuccessful) {
                        try {
                            // Parse the response body into a JSON object.
                            val obj = JSONObject(responseBody)

                            // Read the JWT returned by the server.
                            // Store the JWT for future authenticated requests.
                            storedAuthToken = obj.getString("token")

                            // Create the User object from the response data.
                            //
                            // The password is intentionally not read from the
                            // response because the Android application does not
                            // need the password after authentication succeeds.
                            val user = User(
                                obj.getString("userName"),
                                obj.getInt("age"),
                                obj.getString("fullName")
                            )

                            // Complete the future with the authenticated user.
                            future.complete(user)
                        } catch (e: Exception) {
                            // Log any JSON parsing error.
                            Log.e("HTTP", "❌ LOGIN parsing error: ${e.message}")

                            // Complete with null when parsing fails.
                            future.complete(null)
                        }
                    } else {
                        // Complete with null when the server rejects the login.
                        future.complete(null)
                    }
                }
            })
        } catch (e: Exception) {
            // Log unexpected errors while preparing the request.
            Log.e("HTTP", "❌ LOGIN exception: ${e.message}")

            // Complete with null on a general error.
            future.complete(null)
        }

        // Return the future immediately.
        return future
    }

    // =========================================================
    // PATCH (PATCH /api/users/{username})
    // =========================================================
    @JvmStatic
    fun patchUser(username: String, updates: Map<String, Any>): CompletableFuture<Boolean> {
        // CompletableFuture for result.
        val future = CompletableFuture<Boolean>()

        try {
            // Convert updates map into JSON object.
            val json = JSONObject(updates)

            // Create request body.
            val body = json.toString().toRequestBody(JSON)

            // Build PATCH request.
            val request = Request.Builder()
                .header("Authorization", "Bearer $storedAuthToken")
                .url("$BASE_URL/$username")
                .patch(body)
                .build()

            // Send async request.
            client.newCall(request).enqueue(callbackBoolean(future))
        } catch (e: Exception) {
            // On error -> false.
            future.complete(false)
        }

        // Return future.
        return future
    }

    // =========================================================
    // DELETE (DELETE /api/users/{username})
    // =========================================================
    @JvmStatic
    fun deleteUser(username: String): CompletableFuture<Boolean> {
        // Future for result.
        val future = CompletableFuture<Boolean>()

        // Build DELETE request.
        val request = Request.Builder()
            .header("Authorization", "Bearer $storedAuthToken")
            .url("$BASE_URL/$username")
            .delete()
            .build()

        // Send async request.
        client.newCall(request).enqueue(callbackBoolean(future))

        // Return future.
        return future
    }

    // =========================================================
    // HEAD (HEAD /api/users/{username})
    // =========================================================
    @JvmStatic
    fun headUser(username: String): CompletableFuture<Boolean> {
        // Future for result.
        val future = CompletableFuture<Boolean>()

        // Build HEAD request.
        val request = Request.Builder()
            .header("Authorization", "Bearer $storedAuthToken")
            .url("$BASE_URL/$username")
            .head()
            .build()

        // Send async request.
        client.newCall(request).enqueue(callbackBoolean(future))

        // Return future.
        return future
    }

    // =========================================================
    // UPDATE BMI (PATCH /api/users/{username}/bmi?bmi=...)
    // =========================================================
    @JvmStatic
    fun updateBmi(username: String, bmi: Double): CompletableFuture<Boolean> {
        // Future for result.
        val future = CompletableFuture<Boolean>()

        // Build PATCH request with query param bmi.
        val request = Request.Builder()
            .header("Authorization", "Bearer $storedAuthToken")
            .url("$BASE_URL/$username/bmi?bmi=$bmi")
            .patch(ByteArray(0).toRequestBody(null))
            .build()

        // Send async request.
        client.newCall(request).enqueue(callbackBoolean(future))

        // Return future.
        return future
    }

    // =========================================================
    // GET BMI (GET /api/users/{username})
    // =========================================================
    @JvmStatic
    fun getBmi(username: String): CompletableFuture<Double?> {
        // Future for result.
        val future = CompletableFuture<Double?>()

        // Build GET request.
        val request = Request.Builder()
            .header("Authorization", "Bearer $storedAuthToken")
            .url("$BASE_URL/$username")
            .get()
            .build()

        // Send async request.
        client.newCall(request).enqueue(object : Callback {

            override fun onFailure(call: Call, e: IOException) {
                // If request fails.
                future.complete(null)
            }

            override fun onResponse(call: Call, response: Response) {
                // If response is success.
                if (response.isSuccessful) {
                    try {
                        // Read response body.
                        val body = response.body.string()

                        // Parse JSON.
                        val obj = JSONObject(body)

                        // Check if bmi exists.
                        if (obj.has("bmi")) {
                            // Complete future with BMI.
                            future.complete(obj.getDouble("bmi"))
                        } else {
                            // BMI not found -> complete with null.
                            future.complete(null)
                        }
                    } catch (e: Exception) {
                        // Complete future with null.
                        future.complete(null)
                    }
                } else {
                    // Complete future with null.
                    future.complete(null)
                }
            }
        })

        // Return future.
        return future
    }

    // =========================================================
    // UPDATE WATER (PATCH /api/users/{username}/water?amount=...)
    // Sends a PATCH request to update the user's water intake.
    // =========================================================
    @JvmStatic
    fun updateWater(username: String, amount: Int): CompletableFuture<Boolean> {
        // Future that will hold the result of the network call.
        val future = CompletableFuture<Boolean>()

        // Build the request URL.
        val url = "$BASE_URL/$username/water?amount=$amount"

        // Debug logs before sending request.
        Log.d("HTTP", "➡️ Sending PATCH request to $url")
        Log.d("HTTP", "📤 Request body: (empty)")

        // Build the PATCH request with an empty body.
        val request = Request.Builder()
            .header("Authorization", "Bearer $storedAuthToken")
            .url(url)
            .patch(ByteArray(0).toRequestBody(null))
            .build()

        // Execute the request asynchronously.
        client.newCall(request).enqueue(object : Callback {

            override fun onFailure(call: Call, e: IOException) {
                // Log error if request fails.
                Log.e("HTTP", "❌ updateWater request failed: ${e.message}")
                // Complete future with false.
                future.complete(false)
            }

            override fun onResponse(call: Call, response: Response) {
                // Capture status code.
                val code = response.code
                // Capture response body as string.
                val body = response.body.string()

                // Debug logs for response.
                Log.d("HTTP", "⬅️ Received response for updateWater, code = $code")
                Log.d("HTTP", "📥 Response body: $body")

                // Complete the future depending on success.
                if (response.isSuccessful) {
                    // Log success.
                    Log.d("DEBUG", "✅ updateWater worked!")
                    // Complete future with true.
                    future.complete(true)
                } else {
                    // Log failure.
                    Log.d("DEBUG", "❌ updateWater failed with code $code")
                    // Complete future with false.
                    future.complete(false)
                }
            }
        })

        // Return the future immediately (async result will be set later).
        return future
    }

    // =========================================================
    // GET WATER (GET /api/users/{username}/water)
    // =========================================================
    @JvmStatic
    fun getWater(username: String): CompletableFuture<JSONObject?> {
        // Future for result.
        val future = CompletableFuture<JSONObject?>()

        // Build GET request.
        val request = Request.Builder()
            .header("Authorization", "Bearer $storedAuthToken")
            .url("$BASE_URL/$username/water")
            .get()
            .build()

        // Send async request.
        client.newCall(request).enqueue(object : Callback {

            override fun onFailure(call: Call, e: IOException) {
                // On failure.
                // Log error.
                Log.e("HTTP", "❌ getWater request failed: ${e.message}")
                // Complete future with null.
                future.complete(null)
            }

            override fun onResponse(call: Call, response: Response) {
                // If success.
                if (response.isSuccessful) {
                    try {
                        // Read body.
                        val body = response.body.string()

                        // Parse into JSONObject.
                        val obj = JSONObject(body)

                        // Complete with object.
                        future.complete(obj)
                    } catch (e: Exception) {
                        // Complete future with null.
                        future.complete(null)
                    }
                } else {
                    // Complete future with null.
                    future.complete(null)
                }
            }
        })

        // Return future.
        return future
    }

    // =========================================================
    // Helper: completes a Boolean future.
    // =========================================================
    private fun callbackBoolean(future: CompletableFuture<Boolean>): Callback {
        return object : Callback {

            override fun onFailure(call: Call, e: IOException) {
                // Log error.
                Log.e("HTTP", "❌ Request failed: ${e.message}")
                // On failure -> false.
                future.complete(false)
            }

            override fun onResponse(call: Call, response: Response) {
                // Read response body.
                val body = response.body.string()

                // Log response details.
                Log.d("HTTP", "⬅️ Response code: ${response.code}")
                Log.d("HTTP", "⬅️ Response body: $body")

                // On success -> true if status is 200-299.
                future.complete(response.isSuccessful)
            }
        }
    }

    // =========================================================
    // Helper: completes a JSONObject future.
    // =========================================================
    @Suppress("unused")
    private fun callbackJson(future: CompletableFuture<JSONObject?>): Callback {
        return object : Callback {

            override fun onFailure(call: Call, e: IOException) {
                // On failure -> null.
                future.complete(null)
            }

            override fun onResponse(call: Call, response: Response) {
                // If success.
                if (response.isSuccessful) {
                    try {
                        // Read body.
                        val body = response.body.string()

                        // Parse into JSONObject.
                        val obj = JSONObject(body)

                        // Complete future with object.
                        future.complete(obj)
                    } catch (e: Exception) {
                        // Complete future with null.
                        future.complete(null)
                    }
                } else {
                    // Complete future with null.
                    future.complete(null)
                }
            }
        }
    }

    // ---------------------------------------------------------------------
    // GET WATER HISTORY MAP
    // Calls: GET /api/users/{username}/waterHistoryMap?days=7
    // Returns: JSONObject {"2025-09-29":1200, "2025-09-28":2000, ...}
    // ---------------------------------------------------------------------
    @JvmStatic
    fun getWaterHistoryMap(username: String, days: Int): CompletableFuture<JSONObject?> {
        // Future for async result.
        val future = CompletableFuture<JSONObject?>()

        // Build the URL for water history map.
        val url = "$BASE_URL/$username/waterHistoryMap?days=$days"

        // Build GET request.
        val request = Request.Builder()
            .header("Authorization", "Bearer $storedAuthToken")
            .url(url)
            .get()
            .build()

        // Send request asynchronously.
        client.newCall(request).enqueue(object : Callback {

            override fun onFailure(call: Call, e: IOException) {
                // Log error and complete future with null.
                Log.e("HTTP", "❌ getWaterHistoryMap failed", e)
                // Complete future with null.
                future.complete(null)
            }

            override fun onResponse(call: Call, response: Response) {
                try {
                    // If response is not successful, complete with null.
                    if (!response.isSuccessful) {
                        // Log error and complete with null.
                        Log.e("HTTP", "❌ getWaterHistoryMap error code=${response.code}")
                        // Complete future with null.
                        future.complete(null)
                        // Stop processing.
                        return
                    }

                    // Read body and log response.
                    val body = response.body.string()
                    // Log response body.
                    Log.d("HTTP", "📥 getWaterHistoryMap response=$body")

                    // Parse JSON and complete future.
                    future.complete(JSONObject(body))
                } catch (e: Exception) {
                    // Handle JSON parse errors.
                    Log.e("HTTP", "❌ getWaterHistoryMap parse error", e)
                    // Complete future with null.
                    future.complete(null)
                } finally {
                    // Close response.
                    response.close()
                }
            }
        })

        // Return future.
        return future
    }

    // -------------------------------------------------------------
    // getWeeklyAverages
    // Calls: GET {BASE_URL}/{username}/weeklyAverages
    // Returns: CompletableFuture<Map<String, Integer>>
    // -------------------------------------------------------------
    @JvmStatic
    fun getWeeklyAverages(username: String): CompletableFuture<Map<String, Int>> {
        // Future for async result.
        val future = CompletableFuture<Map<String, Int>>()

        // Build the URL for weekly water history map.
        val url = "$BASE_URL/$username/weeklyAverages"

        // Build GET request.
        val request = Request.Builder()
            .header("Authorization", "Bearer $storedAuthToken")
            .url(url)
            .get()
            .build()

        // Send request asynchronously.
        client.newCall(request).enqueue(object : Callback {

            override fun onFailure(call: Call, e: IOException) {
                // Log error and complete with exception.
                Log.w("HTTP", "weeklyAverages onFailure: ${e.message}")
                // Complete future with exception.
                future.completeExceptionally(e)
            }

            override fun onResponse(call: Call, response: Response) {
                try {
                    // Handle non-OK responses.
                    if (!response.isSuccessful) {
                        // Parse body into string.
                        val resp = response.body.string()
                        // Log.
                        Log.w("HTTP", "⚠️ Non-OK weeklyAverages: ${response.code} body=$resp")
                        // Complete future with empty map.
                        future.complete(Collections.emptyMap())

                        // Stop processing.
                        return
                    }

                    // Parse JSON response.
                    val json = response.body.string()
                    val obj = JSONObject(json)

                    // Use LinkedHashMap to preserve order.
                    val map = LinkedHashMap<String, Int>()
                    val keys = obj.keys()

                    // Iterate over JSON keys and populate map.
                    while (keys.hasNext()) {
                        // Get next key.
                        val k = keys.next()
                        // Add key-value pair to map.
                        map[k] = obj.optInt(k, 0)
                    }

                    // Complete future with parsed map.
                    future.complete(map)
                } catch (e: Exception) {
                    // Handle parsing exceptions.
                    future.completeExceptionally(e)
                }
            }
        })

        // Return future.
        return future
    }

    // -------------------------------------------------------------
    // getGoal
    // Calls: GET {BASE_URL}/{username}/goal
    // Returns: CompletableFuture<JSONObject>
    // -------------------------------------------------------------
    @JvmStatic
    fun getGoal(username: String): CompletableFuture<JSONObject> {
        // Future for async result (JSONObject response).
        val future = CompletableFuture<JSONObject>()

        // Build the URL for goal.
        val url = "$BASE_URL/$username/goal"

        // Build GET request.
        val req = Request.Builder()
            .header("Authorization", "Bearer $storedAuthToken")
            .url(url)
            .get()
            .build()

        // Send request asynchronously.
        client.newCall(req).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                // Complete with exception if request fails.
                future.completeExceptionally(e)
            }

            override fun onResponse(call: Call, response: Response) {
                // Check whether HTTP response is successful.
                if (!response.isSuccessful) {
                    // If HTTP response is not OK, complete with exception.
                    future.completeExceptionally(IOException("HTTP ${response.code}"))
                    // Stop processing.
                    return
                }

                try {
                    // Read response body as string.
                    val body = response.body.string()

                    // Complete future with parsed JSONObject.
                    future.complete(JSONObject(body))
                } catch (ex: Exception) {
                    // On parse error, complete exceptionally.
                    future.completeExceptionally(ex)
                }
            }
        })

        // Return future.
        return future
    }

    // -------------------------------------------------------------
    // setGoal
    // Calls: PUT {BASE_URL}/{username}/goal?goalMl={goalMl}
    // Returns: CompletableFuture<Boolean>
    // -------------------------------------------------------------
    @JvmStatic
    fun setGoal(username: String, goalMl: Int): CompletableFuture<Boolean> {
        // Future for async result (true if success, false otherwise).
        val future = CompletableFuture<Boolean>()

        // Build the URL for goal.
        val url = "$BASE_URL/$username/goal?goalMl=$goalMl"

        // Build PUT request with empty body (query params carry data).
        val req = Request.Builder()
            .header("Authorization", "Bearer $storedAuthToken")
            .url(url)
            .put(ByteArray(0).toRequestBody(null))
            .build()

        // Send request asynchronously.
        client.newCall(req).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                // Complete with exception if request fails.
                future.completeExceptionally(e)
            }

            override fun onResponse(call: Call, response: Response) {
                // Complete with true if response is successful, otherwise false.
                future.complete(response.isSuccessful)
            }
        })

        // Return future.
        return future
    }

    // ---------------------------------------------------------------------
    // GET BMI DISTRIBUTION (GLOBAL)
    // Calls: GET {BASE_URL}/stats/bmiDistribution
    // Returns: CompletableFuture<JSONObject> like:
    // {"Underweight":3,"Normal":12,"Overweight":5,"Obese":2}
    // ---------------------------------------------------------------------
    @JvmStatic
    fun getBmiDistribution(): CompletableFuture<JSONObject?> {
        // Future for async result.
        val future = CompletableFuture<JSONObject?>()

        // Build URL for the statistics endpoint.
        val url = "$BASE_URL/stats/bmiDistribution"

        // Build GET request.
        val request = Request.Builder()
            .url(url)
            .get()
            .build()

        // Send async request.
        client.newCall(request).enqueue(object : Callback {

            override fun onFailure(call: Call, e: IOException) {
                // Log error and complete with null.
                Log.e("HTTP", "❌ getbmidistribution failed: ${e.message}")
                // Complete future with null.
                future.complete(null)
            }

            override fun onResponse(call: Call, response: Response) {
                try {
                    // If not successful -> return null.
                    if (!response.isSuccessful) {
                        // Parse body into string.
                        val resp = response.body.string()
                        // Log.
                        Log.w("HTTP", "⚠️ Non-OK getbmidistribution: ${response.code} body=$resp")
                        // Complete with null.
                        future.complete(null)
                        // Stop processing.
                        return
                    }

                    // Read JSON body as string.
                    val json = response.body.string()

                    // Log response.
                    Log.d("HTTP", "📥 getbmidistribution response=$json")

                    // Parse into JSONObject and complete future with it.
                    future.complete(JSONObject(json))
                } catch (ex: Exception) {
                    // Log error.
                    Log.e("HTTP", "❌ getbmidistribution parse error", ex)
                    // Complete with null.
                    future.complete(null)
                }
            }
        })

        // Return future.
        return future
    }

    // =========================================================
    // GET CALORIES (GET /api/users/{username}/calories)
    // Returns: CompletableFuture<Integer>
    // - On success: the calories value from server (can be 0+)
    // - On failure: null
    // =========================================================
    @JvmStatic
    fun getCalories(username: String): CompletableFuture<Int?> {
        // Create future that will hold the Integer result (or null on error).
        val future = CompletableFuture<Int?>()

        // Build GET request to /{username}/calories.
        val request = Request.Builder()
            .header("Authorization", "Bearer $storedAuthToken")
            // Set target URL for calories endpoint.
            .url("$BASE_URL/$username/calories")
            // Use GET method.
            .get()
            // Build the request object.
            .build()

        // Execute request asynchronously using OkHttp client.
        client.newCall(request).enqueue(object : Callback {

            override fun onFailure(call: Call, e: IOException) {
                // Log error in case of network failure.
                Log.e("HTTP", "❌ getCalories request failed: ${e.message}")
                // Complete future with null to indicate failure.
                future.complete(null)
            }

            override fun onResponse(call: Call, response: Response) {
                try {
                    // If HTTP response is not in 200-299 range.
                    if (!response.isSuccessful) {
                        // Log warning with HTTP code.
                        Log.w("HTTP", "⚠️ getCalories non-OK HTTP: ${response.code}")
                        // Complete future with null (error).
                        future.complete(null)
                        // Stop processing.
                        return
                    }

                    // Read response body as string (JSON text).
                    val body = response.body.string()
                    // Log body for debugging.
                    Log.d("HTTP", "📥 getCalories body: $body")

                    // Parse JSON string into JSONObject.
                    val obj = JSONObject(body)
                    // Extract "calories" field, default = 0 if missing.
                    val cals = obj.optInt("calories", 0)

                    // Complete future with parsed calories value.
                    future.complete(cals)
                } catch (ex: Exception) {
                    // Log parsing error.
                    Log.e("HTTP", "❌ getCalories parse error: ${ex.message}")
                    // Complete future with null on exception.
                    future.complete(null)
                }
            }
        })

        // Return future immediately (result will arrive asynchronously).
        return future
    }

    // =========================================================
    // SET CALORIES (PUT /api/users/{username}/calories?calories=...)
    // Returns: CompletableFuture<Boolean>
    // - true  -> server accepted and updated
    // - false -> bad request / user not found / other non-2xx
    // =========================================================
    @JvmStatic
    fun setCalories(username: String, calories: Int): CompletableFuture<Boolean> {
        // Create future that will hold true/false result.
        val future = CompletableFuture<Boolean>()

        // Build URL with query parameter ?calories=...
        val url = "$BASE_URL/$username/calories?calories=$calories"

        // Build PUT request with empty body (data passes via query param).
        val request = Request.Builder()
            .header("Authorization", "Bearer $storedAuthToken")
            // Set target URL for update calories endpoint.
            .url(url)
            // Use PUT method with empty body.
            .put(ByteArray(0).toRequestBody(null))
            // Build the request object.
            .build()

        // Execute request asynchronously.
        client.newCall(request).enqueue(object : Callback {

            override fun onFailure(call: Call, e: IOException) {
                // Log error on network failure.
                Log.e("HTTP", "❌ setCalories request failed: ${e.message}")
                // Complete future with false to indicate failure.
                future.complete(false)
            }

            override fun onResponse(call: Call, response: Response) {
                // Log HTTP status code for debugging.
                Log.d("HTTP", "⬅️ setCalories response code: ${response.code}")
                // Complete future with true if response is in 200-299 range.
                future.complete(response.isSuccessful)
            }
        })

        // Return future immediately (result will be available later).
        return future
    }
}