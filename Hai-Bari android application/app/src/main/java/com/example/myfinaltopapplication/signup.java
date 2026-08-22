// Define package name for this Activity
package com.example.myfinaltopapplication;
// Import Intent class for navigation between Activities
import android.content.Intent;
// Import Bundle for saving/restoring Activity state
import android.os.Bundle;
// Import Button widget
import android.widget.Button;
// Import EditText widget for text input fields
import android.widget.EditText;
// Import Toast for short popup notifications
import android.widget.Toast;
// Import EdgeToEdge to enable immersive UI layout
import androidx.activity.EdgeToEdge;
// Import AppCompatActivity base class
import androidx.appcompat.app.AppCompatActivity;
// Import CompletableFuture for handling async responses
import java.util.concurrent.CompletableFuture;

// -----------------------------------------------------------------------------
// signup Activity
// Purpose: allows new users to register in the system
// Uses RestClient.register() to communicate with Spring Boot backend
// -----------------------------------------------------------------------------
public class signup extends AppCompatActivity {

    // Declare UI fields for user inputs
    private EditText usernameInput;   // Field for username
    private EditText passwordInput;   // Field for password
    private EditText fullnameInput;   // Field for full name
    private EditText ageInput;        // Field for age
    @SuppressWarnings("FieldCanBeLocal")
    private Button registerButton;    // Button to trigger registration
    @SuppressWarnings("unused")
    private Button backToLogin;       // (Optional) button to go back to login screen

    // -------------------------------------------------------------------------
    // onCreate - lifecycle method called when Activity is first created
    // -------------------------------------------------------------------------
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Call parent constructor
        super.onCreate(savedInstanceState);
        // Enable edge-to-edge screen layout
        EdgeToEdge.enable(this);
        // Set layout file for this Activity
        setContentView(R.layout.activity_signup);

        // ---------------------------------------------------------------------
        // Bind UI components from layout file to Java fields
        // ---------------------------------------------------------------------
        // Bind input fields and button
        usernameInput = findViewById(R.id.editUsername);
        passwordInput = findViewById(R.id.editPassword);
        fullnameInput = findViewById(R.id.editFullName);
        ageInput = findViewById(R.id.editAge);
        registerButton = findViewById(R.id.btnRegister);

        // ---------------------------------------------------------------------
        // Handle Register button click event
        // ---------------------------------------------------------------------
        registerButton.setOnClickListener(view -> {
            // Read input values from text fields
            // Trim to remove leading/trailing spaces
            String usern = usernameInput.getText().toString().trim();
            String pass = passwordInput.getText().toString().trim();
            String full = fullnameInput.getText().toString().trim();
            String ageStr = ageInput.getText().toString().trim();

            // Validate that none of the fields are empty
            if (usern.isEmpty() || pass.isEmpty() || full.isEmpty() || ageStr.isEmpty()) {
                Toast.makeText(signup.this, getString(R.string.fill_all_fields), Toast.LENGTH_SHORT).show();
                return; // Stop if invalid input
            }

            // Convert age from string to integer
            int age = Integer.parseInt(ageStr);

            // Create new User object with input values
            User newUser = new User(usern, pass, age, full);

            // Call RestClient.register() to send POST /api/users
            CompletableFuture<Boolean> future = RestClient.register(newUser);

            // Handle async server response
            future.thenAccept(success -> runOnUiThread(() -> {
                if (success) {
                    // Registration successful → show toast and go to LoginActivity
                    Toast.makeText(signup.this, R.string.sign_up_succesfully, Toast.LENGTH_SHORT).show();
                    // Create Intent to navigate to LoginActivity
                    Intent intent = new Intent(signup.this, LoginActivity.class);
                    // Start LoginActivity
                    startActivity(intent);
                    finish(); // Close signup so user can’t go back
                } else {
                    // Registration failed (likely username already exists)
                    Toast.makeText(signup.this, R.string.username_allready_exists, Toast.LENGTH_SHORT).show();
                }
            }));
        });
    }
}
