@file:Suppress("ClassName")
// Define package name for this Activity.
package com.example.myfinaltopapplication
// Import Intent class for navigation between Activities.
import android.content.Intent
// Import Bundle for saving/restoring Activity state.
import android.os.Bundle
// Import Button widget.
import android.widget.Button
// Import EditText widget for text input fields.
import android.widget.EditText
// Import Toast for short popup notifications.
import android.widget.Toast
// Import enableEdgeToEdge to enable immersive UI layout.
import androidx.activity.enableEdgeToEdge
// Import AppCompatActivity base class.
import androidx.appcompat.app.AppCompatActivity

// -----------------------------------------------------------------------------
// signup Activity
// Purpose: allows new users to register in the system.
// Uses RestClient.register() to communicate with Spring Boot backend.
// -----------------------------------------------------------------------------
class signup : AppCompatActivity() {
    // Declare UI fields for user inputs.
    private lateinit var usernameInput: EditText   // Field for username.
    private lateinit var passwordInput: EditText   // Field for password.
    private lateinit var filenamesInput: EditText   // Field for full name.
    private lateinit var ageInput: EditText        // Field for age.
    private lateinit var registerButton: Button    // Button to trigger registration.
    @Suppress("unused")
    private lateinit var backToLogin: Button       // (Optional) button to go back to login screen.

    // -------------------------------------------------------------------------
    // onCreate - lifecycle method called when Activity is first created.
    // -------------------------------------------------------------------------
    override fun onCreate(savedInstanceState: Bundle?) {
        // Call parent Activity implementation.
        super.onCreate(savedInstanceState)
        // Enable edge-to-edge screen layout.
        enableEdgeToEdge()
        // Set layout file for this Activity.
        setContentView(R.layout.activity_signup)

        // ---------------------------------------------------------------------
        // Bind UI components from layout file to Kotlin fields.
        // ---------------------------------------------------------------------
        // Bind username input field.
        usernameInput = findViewById(R.id.editUsername)
        // Bind password input field.
        passwordInput = findViewById(R.id.editPassword)
        // Bind full name input field.
        filenamesInput = findViewById(R.id.editFullName)
        // Bind age input field.
        ageInput = findViewById(R.id.editAge)
        // Bind register button.
        registerButton = findViewById(R.id.btnRegister)

        // ---------------------------------------------------------------------
        // Handle Register button click event.
        // ---------------------------------------------------------------------
        registerButton.setOnClickListener {
            // Read username and trim leading/trailing spaces.
            val user = usernameInput.text.toString().trim()
            // Read password and trim leading/trailing spaces.
            val pass = passwordInput.text.toString().trim()
            // Read full name and trim leading/trailing spaces.
            val full = filenamesInput.text.toString().trim()
            // Read age and trim leading/trailing spaces.
            val ageStr = ageInput.text.toString().trim()

            // Validate that none of the fields are empty.
            if (user.isEmpty() || pass.isEmpty() || full.isEmpty() || ageStr.isEmpty()) {
                // Show validation message when a field is empty.
                Toast.makeText(this@signup, getString(R.string.fill_all_fields), Toast.LENGTH_SHORT).show()
            } else {
                // Convert age from String to Int.
                val age = ageStr.toInt()
                // Create new User object with input values.
                val newUser = User(user, pass, age, full)
                // Call RestClient.register() to send the registration request.
                val future = RestClient.register(newUser)

                // Handle async server response.
                future.thenAccept { success ->
                    // Move UI operations back to the Android main thread.
                    runOnUiThread {
                        // Check whether registration succeeded.
                        if (success) {
                            // Registration successful -> show success message.
                            Toast.makeText(this@signup, R.string.sign_up_succesfully, Toast.LENGTH_SHORT).show()
                            // Create Intent to navigate to LoginActivity.
                            val intent = Intent(this@signup, LoginActivity::class.java)
                            // Start LoginActivity.
                            startActivity(intent)
                            // Close signup so the user cannot go back.
                            finish()
                        } else {
                            // Registration failed, likely because the username already exists.
                            Toast.makeText(this@signup, R.string.username_allready_exists, Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }
    }
}