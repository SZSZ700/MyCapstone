package com.example.myfinaltopapplication;
import android.content.Intent; // For navigating between activities
import android.content.SharedPreferences;
import android.os.Bundle; // For saving/restoring activity state
import android.widget.ImageButton; // For using ImageButton UI component
import androidx.activity.EdgeToEdge; // For enabling edge-to-edge layout
import androidx.appcompat.app.AppCompatActivity; // Base class for Android activities

// -----------------------------------------------------------------------------
// MainActivity: This is the entry point of the Android app
// It only shows a button (water drops image) that leads to LoginActivity
// -----------------------------------------------------------------------------
public class MainActivity extends AppCompatActivity {

    // UI field: the image button that represents "Sign In / Sign Up"
    @SuppressWarnings("FieldCanBeLocal")
    private ImageButton signupPage;

    // -------------------------------------------------------------------------
    // onCreate() - lifecycle method, called when activity is created
    // -------------------------------------------------------------------------
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);   // Call parent constructor
        EdgeToEdge.enable(this); // Enable edge-to-edge layout
        setContentView(R.layout.activity_main); // Inflate the UI layout (activity_main.xml)

        // Read the saved JWT from SharedPreferences.
        SharedPreferences prefs = getSharedPreferences(getString(R.string.myprefs), MODE_PRIVATE);
        String savedToken = prefs.getString("jwtToken", null);

        // Restore the JWT inside RestClient if one was previously saved.
        if (savedToken != null && !savedToken.isBlank()) {
            RestClient.setAuthToken(savedToken);
        }

        // Initialize the image button (find it by its ID from XML)
        signupPage = findViewById(R.id.imageButton2);

        // Set an onClickListener on the button
        signupPage.setOnClickListener(view -> {
            // When clicked, create an Intent to open LoginActivity
            Intent intent = new Intent(MainActivity.this, LoginActivity.class);

            // Actually start the new activity (screen transition)
            startActivity(intent);
        });
    }
}
