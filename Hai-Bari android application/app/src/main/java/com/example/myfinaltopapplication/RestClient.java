// Define the package this class belongs to (Android client app)
package com.example.myfinaltopapplication;
// Import OkHttp classes for HTTP requests and responses
import android.util.Log;
import androidx.annotation.NonNull;
import okhttp3.*;
import okio.Buffer;
import org.json.JSONObject;
// Import exception handling for input/output operations
import java.io.IOException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
// Import CompletableFuture for asynchronous programming
import java.util.concurrent.CompletableFuture;


/**
 * RestClient - class that manages communication between
 * the Android app and the Spring Boot backend server.
 * Provides methods for user registration, login, update, delete,
 * BMI management, and water tracking.
 */
@SuppressWarnings({"ConstantValue", "ExtractMethodRecommender", "unused"})
public class RestClient {

    // Base URL of the backend server (10.0.2.2 = localhost for Android emulator)
    private static final String BASE_URL = "https://10.0.2.2:8443/myapp/api/users";

    // Define JSON MediaType for sending JSON data
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    // JWT returned by the server after a successful login.
    //
    // The token is used for authenticated requests.
    // It is never the user's password.
    private static String authToken;

    // ---------------------------------------------------------------------
    // Returns the JWT currently stored by RestClient.
    // ---------------------------------------------------------------------
    public static String getAuthToken() { return authToken; }

    // ---------------------------------------------------------------------
    // Restores a previously saved JWT.
    //
    // This is useful when the Android application is recreated and the token
    // was previously stored in SharedPreferences.
    // ---------------------------------------------------------------------
    public static void setAuthToken(String token) { authToken = token; }

    // ---------------------------------------------------------------------
    // Removes the current JWT.
    //
    // This should be called when the user logs out.
    // ---------------------------------------------------------------------
    public static void clearAuthToken() { authToken = null; }

    // Reusable HTTP client for all network requests
    // Built with an interceptor that prints detailed logs for each request and response
    private static final OkHttpClient client = new OkHttpClient.Builder()
            .addInterceptor(chain -> {
                // Capture the outgoing request
                var request = chain.request();

                // Start timer for performance measurement
                var t1 = System.nanoTime();
                // Log request details
                Log.d("HTTP", "➡️ Sending " + request.method() + " request to " + request.url());

                // If request has a body, log its contents
                if (request.body() != null) {
                    // Create a buffer to capture the request body
                    var buffer = new Buffer();
                    // Write the request body to the buffer
                    request.body().writeTo(buffer);
                    // Log the captured request body
                    android.util.Log.d("HTTP", "📤 Request body: " + buffer.readUtf8());
                }

                // Proceed with the request and capture the response
                var response = chain.proceed(request);

                // End timer for performance measurement
                var t2 = System.nanoTime();
                Log.d("HTTP", "⬅️ Received response for " + response.request().url() +
                        " in " + ((t2 - t1) / 1e6d) + "ms, code = " + response.code());

                // Print response body (peek to avoid consuming original stream)
                var responseBody = response.peekBody(Long.MAX_VALUE);
                Log.d("HTTP", "📥 Response body: " + responseBody.string());

                // Return the response so the client can use it
                return response;
            })
            .build();

    // =========================================================
    // REGISTER (POST /api/users/signup)
    // =========================================================
    public static CompletableFuture<Boolean> register(User user) {
        // Create a CompletableFuture to return the result asynchronously
        var future = new CompletableFuture<Boolean>();

        try {
            // Build JSON object with user details
            var json = new JSONObject();
            json.put("userName", user.getUserName()); // Add username
            json.put("password", user.getPassword()); // Add password
            json.put("fullName", user.getFullName()); // Add full name
            json.put("age", user.getAge());           // Add age

            // Create HTTP request body from JSON
            var body = RequestBody.create(json.toString(), JSON);

            // Build POST request for /signup endpoint
            var request = new Request.Builder()
                    .url(BASE_URL + "/signup") // Correct endpoint for signup
                    .post(body)                // Use POST method
                    .build();

            // Send request asynchronously
            client.newCall(request).enqueue(callbackBoolean(future));

        } catch (Exception e) {
            // If building JSON fails, complete with false
            future.complete(false);
        }

        // Return CompletableFuture
        return future;
    }

    // =========================================================
    // LOGIN (POST /api/users/login)
    // =========================================================
    public static CompletableFuture<User> login(String username, String password) {

        // Future object that will complete with User if login succeeds.
        var future = new CompletableFuture<User>();

        try {
            // Build JSON object with the login credentials.
            var json = new JSONObject();

            // Add the username to the request body.
            json.put("userName", username);

            // Add the raw password to the request body.
            // The password is still required for authentication,
            // but it will no longer be expected in the server response.
            json.put("password", password);

            // Create the HTTP request body from the JSON object.
            var body = RequestBody.create(
                    json.toString(),
                    JSON
            );

            // Build the POST request for the login endpoint.
            var request = new Request.Builder()
                    .url(BASE_URL + "/login")
                    .post(body)
                    .build();

            // Send the request asynchronously.
            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(@NonNull Call call, @NonNull IOException e) {

                    // Log the network error.
                    Log.e("HTTP", "❌ LOGIN request failed: " + e.getMessage());

                    // Complete with null when the request fails.
                    future.complete(null);
                }

                @Override
                public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {

                    // Read the response body as a string.
                    var responseBody = response.body() != null
                            ? response.body().string()
                            : "null";

                    // Log the HTTP response code.
                    Log.d(
                            "HTTP",
                            "⬅️ LOGIN response code: " + response.code()
                    );

                    // Log the response body.
                    Log.d(
                            "HTTP",
                            "⬅️ LOGIN response body: " + responseBody
                    );

                    // Continue only when the server returned a successful response.
                    if (response.isSuccessful()) {

                        try {
                            // Parse the response body into a JSON object.
                            var obj = new JSONObject(responseBody);

                            // Read the JWT returned by the server.
                            // Store the JWT for future authenticated requests.
                            authToken = obj.getString("token");

                            // Create the User object from the response data.
                            //
                            // The password is intentionally not read from the
                            // response because the Android application does not
                            // need the password after authentication succeeds.
                            var user = new User(
                                    obj.getString("userName"),
                                    obj.getInt("age"),
                                    obj.getString("fullName")
                            );

                            // Complete the future with the authenticated user.
                            future.complete(user);

                        } catch (Exception e) {
                            // Log any JSON parsing error.
                            Log.e(
                                    "HTTP",
                                    "❌ LOGIN parsing error: " + e.getMessage()
                            );

                            // Complete with null when parsing fails.
                            future.complete(null);
                        }

                    } else {
                        // Complete with null when the server rejects the login.
                        future.complete(null);
                    }
                }
            });

        } catch (Exception e) {

            // Log unexpected errors while preparing the request.
            Log.e(
                    "HTTP",
                    "❌ LOGIN exception: " + e.getMessage()
            );

            // Complete with null on a general error.
            future.complete(null);
        }

        // Return the future immediately.
        return future;
    }

    // =========================================================
    // UPDATE (PUT /api/users/{username})
    // =========================================================
    public static CompletableFuture<Boolean> updateUser(String username, User updatedUser) {
        // CompletableFuture for async result
        var future = new CompletableFuture<Boolean>();

        try {
            // Build JSON with updated user info
            var json = new JSONObject();
            json.put("userName", updatedUser.getUserName());
            json.put("password", updatedUser.getPassword());
            json.put("fullName", updatedUser.getFullName());
            json.put("age", updatedUser.getAge());

            // Create request body
            var body = RequestBody.create(json.toString(), JSON);

            // Build PUT request
            var request = new Request.Builder()
                    .header("Authorization", "Bearer " + authToken)
                    .url(BASE_URL + "/" + username) // Endpoint with username path
                    .put(body)                      // Use PUT method
                    .build();

            // Send async request
            client.newCall(request).enqueue(callbackBoolean(future));

        } catch (Exception e) {
            // On error -> complete false
            future.complete(false);
        }

        // Return future
        return future;
    }

    // =========================================================
    // PATCH (PATCH /api/users/{username})
    // =========================================================
    public static CompletableFuture<Boolean> patchUser(String username, Map<String, Object> updates) {
        // CompletableFuture for result
        var future = new CompletableFuture<Boolean>();

        try {
            // Convert updates map into JSON object
            var json = new JSONObject(updates);

            // Create request body
            var body = RequestBody.create(json.toString(), JSON);

            // Build PATCH request
            var request = new Request.Builder()
                    .header("Authorization", "Bearer " + authToken)
                    .url(BASE_URL + "/" + username) // Endpoint with username
                    .patch(body)                    // Use PATCH
                    .build();

            // Send async request
            client.newCall(request).enqueue(callbackBoolean(future));

        } catch (Exception e) {
            // On error -> false
            future.complete(false);
        }

        // Return future
        return future;
    }

    // =========================================================
    // DELETE (DELETE /api/users/{username})
    // =========================================================
    public static CompletableFuture<Boolean> deleteUser(String username) {
        // Future for result
        var future = new CompletableFuture<Boolean>();

        // Build DELETE request
        var request = new Request.Builder()
                .header("Authorization", "Bearer " + authToken)
                .url(BASE_URL + "/" + username) // Endpoint
                .delete()                       // Use DELETE
                .build();

        // Send async request
        client.newCall(request).enqueue(callbackBoolean(future));

        // Return future
        return future;
    }

    // =========================================================
    // HEAD (HEAD /api/users/{username})
    // =========================================================
    public static CompletableFuture<Boolean> headUser(String username) {
        // Future for result
        var future = new CompletableFuture<Boolean>();

        // Build HEAD request
        var request = new Request.Builder()
                .header("Authorization", "Bearer " + authToken)
                .url(BASE_URL + "/" + username) // Endpoint
                .head()                         // Use HEAD
                .build();

        // Send async request
        client.newCall(request).enqueue(callbackBoolean(future));

        // Return future
        return future;
    }

    // =========================================================
    // UPDATE BMI (PATCH /api/users/{username}/bmi?bmi=...)
    // =========================================================
    public static CompletableFuture<Boolean> updateBmi(String username, double bmi) {
        // Future for result
        var future = new CompletableFuture<Boolean>();

        // Build PATCH request with query param bmi
        var request = new Request.Builder()
                .header("Authorization", "Bearer " + authToken)
                .url(BASE_URL + "/" + username + "/bmi?bmi=" + bmi) // Add BMI as query
                .patch(RequestBody.create(new byte[0], null)) // Empty body required
                .build();

        // Send async request
        client.newCall(request).enqueue(callbackBoolean(future));

        // Return future
        return future;
    }

    // =========================================================
    // GET BMI (GET /api/users/{username})
    // =========================================================
    public static CompletableFuture<Double> getBmi(String username) {
        // Future for result
        var future = new CompletableFuture<Double>();

        // Build GET request
        var request = new Request.Builder()
                .header("Authorization", "Bearer " + authToken)
                .url(BASE_URL + "/" + username) // Endpoint
                .get() // Use GET
                .build();

        // Send async request
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                // If request fails
                future.complete(null);
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) {
                // If response is success
                if (response.isSuccessful()) {
                    try {
                        // Read response body
                        assert response.body() != null;
                        var body = response.body().string();

                        // Parse JSON
                        var obj = new JSONObject(body);

                        // Check if bmi exists
                        if (obj.has("bmi")) {
                            future.complete(obj.getDouble("bmi"));
                        } else {
                            // BMI not found -> complete with null
                            future.complete(null);
                        }
                    } catch (Exception e) {
                        // complete future with null
                        future.complete(null);
                    }
                } else {
                    // complete future with null
                    future.complete(null);
                }
            }
        });

        // Return future
        return future;
    }

    // =========================================================
    // UPDATE WATER (PATCH /api/users/{username}/water?amount=...)
    // Sends a PATCH request to update the user's water intake
    // =========================================================
    public static CompletableFuture<Boolean> updateWater(String username, int amount) {
        // Future that will hold the result of the network call
        var future = new CompletableFuture<Boolean>();

        // Build the request URL
        var url = BASE_URL + "/" + username + "/water?amount=" + amount;

        // Debug logs before sending request
        Log.d("HTTP", "➡️ Sending PATCH request to " + url);
        Log.d("HTTP", "📤 Request body: (empty)");

        // Build the PATCH request with an empty body
        var request = new Request.Builder()
                .header("Authorization", "Bearer " + authToken)
                .url(url)
                .patch(RequestBody.create(new byte[0], null))
                .build();

        // Execute the request asynchronously
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                // Log error if request fails
                Log.e("HTTP", "❌ updateWater request failed: " + e.getMessage());
                // Complete future with false
                future.complete(false);
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                // Capture status code and body
                var code = response.code();
                // Capture response body as string
                var body = response.body() != null ? response.body().string() : "";

                // Debug logs for response
                Log.d("HTTP", "⬅️ Received response for updateWater, code = " + code);
                Log.d("HTTP", "📥 Response body: " + body);

                // Complete the future depending on success
                if (response.isSuccessful()) {
                    // Log success
                    Log.d("DEBUG", "✅ updateWater worked!");
                    // Complete future with true
                    future.complete(true);
                } else {
                    // Log failure
                    Log.d("DEBUG", "❌ updateWater failed with code " + code);
                    // Complete future with false
                    future.complete(false);
                }
            }
        });

        // Return the future immediately (async result will be set later)
        return future;
    }

    // =========================================================
    // GET WATER (GET /api/users/{username}/water)
    // =========================================================
    public static CompletableFuture<JSONObject> getWater(String username) {
        // Future for result
        var future = new CompletableFuture<JSONObject>();

        // Build GET request
        var request = new Request.Builder()
                .header("Authorization", "Bearer " + authToken)
                .url(BASE_URL + "/" + username + "/water") // Endpoint
                .get() // Use GET
                .build();

        // Send async request
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                // On failure
                // Log error
                Log.e("HTTP", "❌ getWater request failed: " + e.getMessage());
                // Complete future with null
                future.complete(null);
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) {
                // If success
                if (response.isSuccessful()) {
                    try {
                        // Read body
                        assert response.body() != null;

                        var body = response.body().string();

                        // Parse into JSONObject
                        var obj = new JSONObject(body);

                        // Complete with object
                        future.complete(obj);
                    } catch (Exception e) {
                        // complete future with null
                        future.complete(null);
                    }
                } else {
                    // complete future with null
                    future.complete(null);
                }
            }
        });

        // Return future
        return future;
    }

    // =========================================================
    // Helper: completes a Boolean future
    // =========================================================
    private static Callback callbackBoolean(CompletableFuture<Boolean> future) {
        return new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                // Log error
                Log.e("HTTP", "❌ Request failed: " + e.getMessage());
                // On failure -> false
                future.complete(false);
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                var  body = response.body() != null ? response.body().string() : "null";

                // Log response details
                Log.d("HTTP", "⬅️ Response code: " + response.code());
                Log.d("HTTP", "⬅️ Response body: " + body);

                // On success -> true if status is 200-299
                future.complete(response.isSuccessful());
            }
        };
    }

    // =========================================================
    // Helper: completes a JSONObject future
    // =========================================================
    @SuppressWarnings("unused")
    private static Callback callbackJson(CompletableFuture<JSONObject> future) {
        return new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                // On failure -> null
                future.complete(null);
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) {
                // If success
                if (response.isSuccessful()) {
                    try {
                        // Parse JSON
                        assert response.body() != null;

                        var body = response.body().string();

                        // Parse into JSONObject
                        var obj = new JSONObject(body);

                        // complete future with object
                        future.complete(obj);
                    } catch (Exception e) {
                        // complete future with null
                        future.complete(null);
                    }
                } else {
                    // complete future with null
                    future.complete(null);
                }
            }
        };
    }


    // ---------------------------------------------------------------------
    // GET WATER HISTORY MAP
    // Calls: GET /api/users/{username}/waterHistoryMap?days=7
    // Returns: JSONObject {"2025-09-29":1200, "2025-09-28":2000, ...}
    // ---------------------------------------------------------------------
    public static CompletableFuture<JSONObject> getWaterHistoryMap(String username, int days) {
        var future = new CompletableFuture<JSONObject>();

        // Build the URL for water history map
        var url = BASE_URL + "/" + username + "/waterHistoryMap?days=" + days;

        // Build GET request
        var request = new Request.Builder()
                .header("Authorization", "Bearer " + authToken)
                .url(url)
                .get()
                .build();

        // Send request asynchronously
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                // Log error and complete future with null
                Log.e("HTTP", "❌ getWaterHistoryMap failed", e);
                // Complete future with null
                future.complete(null);
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) {
                try (Response r = response) {
                    // If response is not successful, complete with null
                    if (!r.isSuccessful()) {
                        // Log error and complete with null
                        Log.e("HTTP", "❌ getWaterHistoryMap error code=" + r.code());
                        // complete future with null
                        future.complete(null);
                        // Stop processing
                        return;
                    }
                    // Read body and log response
                    var body = r.body() != null ? r.body().string() : "{}";
                    // Log response body
                    Log.d("HTTP", "📥 getWaterHistoryMap response=" + body);

                    // Parse JSON and complete future
                    future.complete(new JSONObject(body));
                } catch (Exception e) {
                    // Handle JSON parse errors
                    Log.e("HTTP", "❌ getWaterHistoryMap parse error", e);
                    // Complete future with null
                    future.complete(null);
                }
            }
        });

        return future;
    }

    // -------------------------------------------------------------
    // getWeeklyAverages
    // Calls: GET {BASE_URL}/{username}/weeklyAverages
    // Returns: CompletableFuture<Map<String, Integer>>
    // -------------------------------------------------------------
    public static CompletableFuture<Map<String, Integer>> getWeeklyAverages(String username) {
        // Future for async result
        var future = new CompletableFuture<Map<String, Integer>>();

        // Build the URL for weakly water history map
        var url = BASE_URL + "/" + username + "/weeklyAverages";

        // Build GET request
        var request = new Request.Builder()
                .header("Authorization", "Bearer " + authToken)
                .url(url)
                .get()
                .build();

        // Send request asynchronously
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                // Log error and complete with exception
                Log.w("HTTP", "weeklyAverages onFailure: " + e.getMessage());
                // Complete future with exception
                future.completeExceptionally(e);
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) {
                try (var body = response.body()) {
                    // Handle non-OK responses
                    if (!response.isSuccessful()) {
                        // parse body into string
                        var resp = (body != null ? body.string() : "");
                        // Log
                        Log.w("HTTP", "⚠️ Non-OK weeklyAverages: " + response.code() + " body=" + resp);
                        // Complete future with empty map
                        future.complete(Collections.emptyMap());

                        // Stop processing
                        return;
                    }

                    // Parse JSON response
                    var json = (body != null ? body.string() : "{}");
                    var obj = new JSONObject(json);

                    // Use LinkedHashMap to preserve order
                    var map = new LinkedHashMap<String, Integer>();
                    var keys = obj.keys();

                    // Iterate over JSON keys and populate map
                    while (keys.hasNext()) {
                        // Get next key
                        var k = keys.next();
                        // Add key-value pair to map
                        map.put(k, obj.optInt(k, 0));
                    }

                    // Complete future with parsed map
                    future.complete(map);
                } catch (Exception e) {
                    // Handle parsing exceptions
                    future.completeExceptionally(e);
                }
            }
        });

        // Return future
        return future;
    }

    // -------------------------------------------------------------
    // getGoal
    // Calls: GET {BASE_URL}/{username}/goal
    // Returns: CompletableFuture<JSONObject>
    public static CompletableFuture<JSONObject> getGoal(String username) {
        // Future for async result (JSONObject response)
        var future = new CompletableFuture<JSONObject>();
        // Build the URL for goal
        var url = BASE_URL + "/" + username + "/goal";

        // Build GET request
        var req = new Request.Builder()
                .header("Authorization", "Bearer " + authToken)
                .url(url)
                .get()
                .build();

        // Send request asynchronously
        client.newCall(req).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                // Complete with exception if request fails
                future.completeExceptionally(e);
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response res) {
                if (!res.isSuccessful()) {
                    // If HTTP response is not OK, complete with exception
                    future.completeExceptionally(new IOException("HTTP " + res.code()));
                    // Stop processing
                    return;
                }
                try {
                    // Parse body into JSONObject {"goalMl": 2600}
                    assert res.body() != null;

                    // Read response body as string
                    var body = res.body().string();

                    // complete future with parsed JSONObject
                    future.complete(new JSONObject(body));
                } catch (Exception ex) {
                    // On parse error, complete exceptionally
                    future.completeExceptionally(ex);
                }
            }
        });

        // Return future
        return future;
    }

    // -------------------------------------------------------------
    // setGoal
    // Calls: PUT {BASE_URL}/{username}/goal?goalMl={goalMl}
    // Returns: CompletableFuture<Boolean>
    public static CompletableFuture<Boolean> setGoal(String username, int goalMl) {
        // Future for async result (true if success, false otherwise)
        var future = new CompletableFuture<Boolean>();
        // Build the URL for goal
        var url = BASE_URL + "/" + username + "/goal?goalMl=" + goalMl;

        // Build PUT request with empty body (query params carry data)
        var req = new Request.Builder()
                .header("Authorization", "Bearer " + authToken)
                .url(url)
                .put(RequestBody.create(new byte[0]))
                .build();

        // Send request asynchronously
        client.newCall(req).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                // Complete with exception if request fails
                future.completeExceptionally(e);
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response res) {
                // Complete with true if response is successful, otherwise false
                future.complete(res.isSuccessful());
            }
        });

        // Return future
        return future;
    }

    // ---------------------------------------------------------------------
    // GET BMI DISTRIBUTION (GLOBAL)
    // Calls: GET {BASE_URL}/stats/bmiDistribution
    // Returns: CompletableFuture<JSONObject> like:
    // {"Underweight":3,"Normal":12,"Overweight":5,"Obese":2}
    // ---------------------------------------------------------------------
    public static CompletableFuture<JSONObject> getBmiDistribution() {
        // Future for async result
        var future = new CompletableFuture<JSONObject>();

        // Build URL for the statistics endpoint
        var url = BASE_URL + "/stats/bmiDistribution";

        // Build GET request
        var request = new Request.Builder()
                .url(url)
                .get()
                .build();

        // Send async request
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                // Log error and complete with null
                //noinspection SpellCheckingInspection
                Log.e("HTTP", "❌ getBmiDistribution failed: " + e.getMessage());
                future.complete(null);
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) {
                try (var body = response.body()) {
                    // If not successful -> return null
                    if (!response.isSuccessful()) {
                        // parse body into string
                        var resp = (body != null ? body.string() : "");
                        // Log
                        //noinspection SpellCheckingInspection
                        Log.w("HTTP", "⚠️ Non-OK getBmiDistribution: " + response.code() + " body=" + resp);
                        // Complete with null
                        future.complete(null);
                        // Stop processing
                        return;
                    }

                    // Read JSON body as string
                    var json = (body != null ? body.string() : "{}");

                    // Log response
                    //noinspection SpellCheckingInspection
                    Log.d("HTTP", "📥 getBmiDistribution response=" + json);

                    // Parse into JSONObject and complete future with it
                    future.complete(new JSONObject(json));
                } catch (Exception ex) {
                    // Log error
                    //noinspection SpellCheckingInspection
                    Log.e("HTTP", "❌ getBmiDistribution parse error", ex);
                    // Complete with null
                    future.complete(null);
                }
            }
        });

        // Return future
        return future;
    }

    // =========================================================
    // GET CALORIES (GET /api/users/{username}/calories)
    // Returns: CompletableFuture<Integer>
    // - On success: the calories value from server (can be 0+)
    // - On failure: null
    // =========================================================
    public static CompletableFuture<Integer> getCalories(String username) {
        // Create future that will hold the Integer result (or null on error)
        var future = new CompletableFuture<Integer>();

        // Build GET request to /{username}/calories
        var request = new Request.Builder()
                .header("Authorization", "Bearer " + authToken)
                // Set target URL for calories endpoint
                .url(BASE_URL + "/" + username + "/calories")
                // Use GET method
                .get()
                // Build the request object
                .build();

        // Execute request asynchronously using OkHttp client
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                // Log error in case of network failure
                Log.e("HTTP", "❌ getCalories request failed: " + e.getMessage());
                // Complete future with null to indicate failure
                future.complete(null);
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) {
                try {
                    // If HTTP response is not in 200-299 range
                    if (!response.isSuccessful()) {
                        // Log warning with HTTP code
                        Log.w("HTTP", "⚠️ getCalories non-OK HTTP: " + response.code());
                        // Complete future with null (error)
                        future.complete(null);
                        // Stop processing
                        return;
                    }

                    // Read response body as string (JSON text)
                    var body = response.body() != null ? response.body().string() : "{}";
                    // Log body for debugging
                    Log.d("HTTP", "📥 getCalories body: " + body);

                    // Parse JSON string into JSONObject
                    var obj = new JSONObject(body);
                    // Extract "calories" field, default = 0 if missing
                    var cals = obj.optInt("calories", 0);

                    // Complete future with parsed calories value
                    future.complete(cals);
                } catch (Exception ex) {
                    // Log parsing error
                    Log.e("HTTP", "❌ getCalories parse error: " + ex.getMessage());
                    // Complete future with null on exception
                    future.complete(null);
                }
            }
        });

        // Return future immediately (result will arrive asynchronously)
        return future;
    }

    // =========================================================
    // SET CALORIES (PUT /api/users/{username}/calories?calories=...)
    // Returns: CompletableFuture<Boolean>
    // - true  → server accepted and updated
    // - false → bad request / user not found / other non-2xx
    // =========================================================
    public static CompletableFuture<Boolean> setCalories(String username, int calories) {
        // Create future that will hold true/false result
        var future = new CompletableFuture<Boolean>();

        // Build URL with query parameter ?calories=...
        var url = BASE_URL + "/" + username + "/calories?calories=" + calories;

        // Build PUT request with empty body (data passes via query param)
        var request = new Request.Builder()
                .header("Authorization", "Bearer " + authToken)
                // Set target URL for update calories endpoint
                .url(url)
                // Use PUT method with empty body
                .put(RequestBody.create(new byte[0]))
                // Build the request object
                .build();

        // Execute request asynchronously
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                // Log error on network failure
                Log.e("HTTP", "❌ setCalories request failed: " + e.getMessage());
                // Complete future with false to indicate failure
                future.complete(false);
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) {
                // Log HTTP status code for debugging
                Log.d("HTTP", "⬅️ setCalories response code: " + response.code());
                // Complete future with true if response is in 200-299 range
                future.complete(response.isSuccessful());
            }
        });

        // Return future immediately (result will be available later)
        return future;
    }
}

