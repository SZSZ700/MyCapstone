@file:Suppress("PackageName")
package com.example.myfinaltopapplication
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.ImageButton
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity

// -----------------------------------------------------------------------------
// MainActivity: This is the entry point of the Android app.
//
// It shows a button represented by a water-drops image.
// Pressing the button opens LoginActivity.
//
// The activity also restores a previously saved JWT token from
// SharedPreferences and gives it back to RestClient.
// -----------------------------------------------------------------------------
class MainActivity : AppCompatActivity() {
    // UI field: the image button that represents "Sign In / Sign Up".
    private lateinit var signupPage: ImageButton

    // -------------------------------------------------------------------------
    // onCreate() - lifecycle method called when the activity is created.
    // -------------------------------------------------------------------------
    override fun onCreate(savedInstanceState: Bundle?) {
        // Call the parent Activity implementation.
        super.onCreate(savedInstanceState)
        // Enable edge-to-edge layout.
        enableEdgeToEdge()
        // Inflate activity_main.xml and display it on the screen.
        setContentView(R.layout.activity_main)

        // Open this application's private SharedPreferences file.
        val prefs: SharedPreferences = getSharedPreferences(
                getString(R.string.myprefs), MODE_PRIVATE)

        // Read the previously saved JWT.
        //
        // The result is nullable because no token may have been saved yet.
        val savedToken: String? = prefs.getString("jwtToken", null)

        // Check whether a valid-looking saved token exists.
        if (!savedToken.isNullOrBlank()) {
            // Restore the JWT inside RestClient.
            RestClient.setAuthToken(savedToken)
        }

        // Find the image button from activity_main.xml.
        signupPage = findViewById(R.id.imageButton2)

        // Register the click listener for the image button.
        signupPage.setOnClickListener {
            // Create an Intent that points from MainActivity to LoginActivity.
            val intent = Intent(this@MainActivity, LoginActivity::class.java)
            // Open LoginActivity.
            startActivity(intent)
        }
    }
}