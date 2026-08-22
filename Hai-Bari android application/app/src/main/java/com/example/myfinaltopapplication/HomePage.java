package com.example.myfinaltopapplication;           // Package declaration
import android.content.Intent;                       // For switching between activities
import android.os.Bundle;                            // For saving/restoring activity state
import android.widget.Button;                        // UI component for buttons
import androidx.activity.EdgeToEdge;                 // Enables edge-to-edge UI layout
import androidx.appcompat.app.AppCompatActivity;     // Base class for Android activities

// -----------------------------------------------------------------------------
// HomePage Activity
// This activity acts as the "dashboard" or main menu after login.
// From here the user can navigate to BMI calculator or Water tracking pages.
// -----------------------------------------------------------------------------
public class HomePage extends AppCompatActivity {

    // -------------------------------------------------------------------------
    // Class fields (UI components)
    @SuppressWarnings("FieldCanBeLocal")
    private Button bmiPage;    // Button for navigating to BMI calculator activity
    @SuppressWarnings("FieldCanBeLocal")
    private Button waterPage;  // Button for navigating to Water tracking activity
    @SuppressWarnings("FieldCanBeLocal")
    private Button graphPage;  // Button for navigating to Graphs/Statistics activity
    @SuppressWarnings("FieldCanBeLocal")
    private Button dailyGoal; // Button for navigating to Daily Water Goal activity
    // -------------------------------------------------------------------------

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState); // Call parent constructor
        EdgeToEdge.enable(this); // Enable full screen UI (edge-to-edge)

        // ---------------------------------------------------------------------
        // Link this Activity to its layout file (activity_home_page.xml)
        setContentView(R.layout.activity_home_page);
        // ---------------------------------------------------------------------

        // ---------------------------------------------------------------------
        // Connect UI components (find buttons by their IDs in XML layout)
        bmiPage = findViewById(R.id.button3);        // BMI button
        waterPage = findViewById(R.id.button4);      // Water button
        graphPage = findViewById(R.id.button5);      // Graphs/Statistics button
        dailyGoal = findViewById(R.id.button6);      // Daily Water Goal button
        // ---------------------------------------------------------------------

        // ---------------------------------------------------------------------
        // Set click listener for BMI button
        bmiPage.setOnClickListener(view -> {
            // Create intent to open BMIActivity
            Intent bmi = new Intent(HomePage.this, BMIActivity.class);
            startActivity(bmi); // Start BMIActivity
        });
        // ---------------------------------------------------------------------

        // ---------------------------------------------------------------------
        // Set click listener for Water button
        waterPage.setOnClickListener(view -> {
            // Create intent to open WaterActivity
            Intent wte = new Intent(HomePage.this, WaterActivity.class);
            startActivity(wte); // Start WaterActivity
        });
        // ---------------------------------------------------------------------

        // ---------------------------------------------------------------------
        // Future extension:
        // You can add navigation to a Graphs/Statistics page here for final project
        // ---------------------------------------------------------------------
        graphPage.setOnClickListener(view -> {
            // Create intent to open GraphsActivity
            Intent graph = new Intent(HomePage.this, WaterChartActivity.class);
            startActivity(graph); // Start GraphsActivity
        });
        // ---------------------------------------------------------------------

        // ---------------------------------------------------------------------
        // Set click listener for Daily Water Goal button
        dailyGoal.setOnClickListener(view -> {
            // Create intent to open DailyWaterGoalActivity
            Intent daily = new Intent(HomePage.this, DailyWaterGoal.class);
            startActivity(daily); // Start DailyWaterGoalActivity
        });
    }
}
