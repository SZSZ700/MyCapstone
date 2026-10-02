@file:Suppress("SpellCheckingInspection")

package com.example.myfinaltopapplication
// Import SuppressLint for suppressing SharedPreferences commit warning.
import android.annotation.SuppressLint
// Import Intent for switching between activities.
import android.content.Intent
// Import SharedPreferences for saving user session locally.
import android.content.SharedPreferences
// Import Bundle for saving/restoring Activity state.
import android.os.Bundle
// Import Button widget for standard clickable buttons.
import android.widget.Button
// Import EditText widget for text input (username, password).
import android.widget.EditText
// Import ImageButton for optional back/home navigation.
import android.widget.ImageButton
// Import Toast for displaying short popup messages.
import android.widget.Toast
// Import enableEdgeToEdge for full immersive screen support.
import androidx.activity.enableEdgeToEdge
// Import AppCompatActivity base class.
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit

// -----------------------------------------------------------------------------
// LoginActivity
// Purpose: allows a user to log into the application by checking credentials
// against backend REST API (Spring Boot + MongoDB).
// -----------------------------------------------------------------------------
class LoginActivity : AppCompatActivity() {

    // Declare UI components (input fields + buttons).
    private lateinit var username: EditText        // Input field for username.
    private lateinit var pass: EditText            // Input field for password.
    private lateinit var loginBtn: Button          // Button that triggers login process.
    private lateinit var signUpBtn: Button         // Button to navigate to signup page.
    @Suppress("unused")
    private lateinit var backTohome: ImageButton   // Optional button for returning to home.

    // -------------------------------------------------------------------------
    // onCreate - lifecycle method called when Activity is first created.
    // -------------------------------------------------------------------------
    @SuppressLint("ApplySharedPref")
    override fun onCreate(savedInstanceState: Bundle?) {
        // Call parent Activity implementation.
        super.onCreate(savedInstanceState)
        // Enable edge-to-edge immersive layout.
        enableEdgeToEdge()
        // Load the layout XML file.
        setContentView(R.layout.activity_login)

        // ---------------------------------------------------------------------
        // Link Kotlin fields with XML components from layout.
        // ---------------------------------------------------------------------
        // Bind username input.
        username = findViewById(R.id.editTextText)
        // Bind password input.
        pass = findViewById(R.id.editTextTextPassword)
        // Bind login button.
        loginBtn = findViewById(R.id.button)
        // Bind signup button.
        signUpBtn = findViewById(R.id.button2)

        // ---------------------------------------------------------------------
        // Login button click logic.
        // ---------------------------------------------------------------------
        loginBtn.setOnClickListener {
            // Read username from the input field.
            val user = username.text.toString().trim()
            // Read password from the input field.
            val pas = pass.text.toString().trim()

            // Validate that no field is empty.
            if (user.isEmpty() || pas.isEmpty()) {
                // Show validation error to the user.
                Toast.makeText(this@LoginActivity, "Please fill all fields", Toast.LENGTH_SHORT).show()
            } else {
                // Call RestClient.login() -> sends POST /api/users/login to server.
                val loginFuture = RestClient.login(user, pas)

                // Handle asynchronous server response.
                loginFuture.thenAccept { user ->
                    // Move UI operations back to the Android main thread.
                    runOnUiThread {
                        // Check whether login succeeded.
                        if (user != null) {
                            // If login successful -> open the application SharedPreferences.
                            val prefs: SharedPreferences = getSharedPreferences(getString(R.string.myprefs), MODE_PRIVATE)
                            // Create an editor for changing SharedPreferences values.
                            prefs.edit {
                                // Save username.
                                putString(getString(R.string.currentuser), user.userName)
                                // Save age.
                                putInt(getString(R.string.age), user.age)
                                // Save full name.
                                putString("fullName", user.fullName)
                                // Read the JWT that RestClient received from the login response.
                                val token = RestClient.getAuthToken()
                                // Save the JWT locally so it can be restored later.
                                putString("jwtToken", token)

                                // ---------------------------------------------------------------------
                                // Save water data (today + yesterday) from server response.
                                // ---------------------------------------------------------------------
                                try {
                                    // RestClient.getWater -> sends GET /api/users/{username}/water.
                                    RestClient.getWater(user.userName).thenAccept { obj ->
                                        // Move SharedPreferences and UI-related work to the main thread.
                                        runOnUiThread {
                                            // Check whether water data was returned.
                                            if (obj != null) {
                                                // Read today's water amount or use 0 when missing.
                                                val today = obj.optInt("todayWater", 0)
                                                // Read yesterday's water amount or use 0 when missing.
                                                val yesterday = obj.optInt("yesterdayWater", 0)

                                                // Save today's water amount.
                                                putInt("todayWater", today)
                                                // Save yesterday's water amount.
                                                putInt("yesterdayWater", yesterday)
                                                // Save changes to SharedPreferences using editor.
                                                commit()

                                                // Log saved water values for debugging.
                                                android.util.Log.d(
                                                    "LOGIN_PREFS",
                                                    "Saved water at login: today=$today, yesterday=$yesterday"
                                                )
                                            } else {
                                                // If water data was not found, set today to 0.
                                                putInt("todayWater", 0)
                                                // Set yesterday to 0 as well.
                                                putInt("yesterdayWater", 0)
                                                // Save changes to SharedPreferences.
                                                commit()
                                            }
                                        }
                                    }
                                } catch (_: Exception) {
                                    // If water data could not be loaded, set today to 0.
                                    putInt("todayWater", 0)
                                    // Set yesterday to 0 as well.
                                    putInt("yesterdayWater", 0)
                                    // Save changes to SharedPreferences.
                                    commit()
                                }

                                // Save the basic user session values to SharedPreferences.
                            }

                            // Show success toast with user's full name.
                            Toast.makeText(this@LoginActivity, "Welcome ${user.fullName}", Toast.LENGTH_SHORT).show()

                            // Create Intent for HomePage Activity.
                            val intent = Intent(this@LoginActivity, HomePage::class.java)
                            // Start HomePage Activity.
                            startActivity(intent)
                            // Close LoginActivity so the user cannot go back to it.
                            finish()
                        } else {
                            // If login failed, show invalid credentials message.
                            Toast.makeText(this@LoginActivity, "Invalid username or password", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }

        // ---------------------------------------------------------------------
        // Signup button click logic.
        // ---------------------------------------------------------------------
        signUpBtn.setOnClickListener {
            // Create Intent to open signup Activity.
            val intent = Intent(this@LoginActivity, signup::class.java)
            // Start the signup Activity.
            startActivity(intent)
        }
    }
}