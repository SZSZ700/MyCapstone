package com.example.myfinaltopapplication
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity

// -----------------------------------------------------------------------------
// HomePage Activity.
//
// This activity acts as the dashboard or main menu after login.
//
// From here the user can navigate to:
// - BMI calculator
// - Water tracking
// - Water statistics
// - Daily water goal
// -----------------------------------------------------------------------------
class HomePage : AppCompatActivity() {
    // -------------------------------------------------------------------------
    // Class fields - UI components.
    // -------------------------------------------------------------------------
    // Button for navigating to the BMI calculator activity.
    private lateinit var bmiPage: Button
    // Button for navigating to the water tracking activity.
    private lateinit var waterPage: Button
    // Button for navigating to the graphs/statistics activity.
    private lateinit var graphPage: Button
    // Button for navigating to the daily water goal activity.
    private lateinit var dailyGoal: Button

    // -------------------------------------------------------------------------
    // Called when the activity is created.
    // -------------------------------------------------------------------------
    override fun onCreate(savedInstanceState: Bundle?) {
        // Call the parent Activity implementation.
        super.onCreate(savedInstanceState)
        // Enable edge-to-edge layout.
        enableEdgeToEdge()
        // ---------------------------------------------------------------------
        // Link this Activity to activity_home_page.xml.
        // ---------------------------------------------------------------------
        // Inflate the HomePage layout.
        setContentView(R.layout.activity_home_page)

        // ---------------------------------------------------------------------
        // Connect the UI buttons to their XML IDs.
        // ---------------------------------------------------------------------
        // Find the BMI button.
        bmiPage = findViewById(R.id.button3)
        // Find the Water button.
        waterPage = findViewById(R.id.button4)
        // Find the Graphs/Statistics button.
        graphPage = findViewById(R.id.button5)
        // Find the Daily Water Goal button.
        dailyGoal = findViewById(R.id.button6)

        // ---------------------------------------------------------------------
        // BMI BUTTON
        // ---------------------------------------------------------------------
        // Set the click listener for the BMI button.
        bmiPage.setOnClickListener {
            // Create an Intent that opens BMIActivity.
            val bmi = Intent(this@HomePage, BMIActivity::class.java)
            // Start BMIActivity.
            startActivity(bmi)
        }

        // ---------------------------------------------------------------------
        // WATER BUTTON
        // ---------------------------------------------------------------------
        // Set the click listener for the Water button.
        waterPage.setOnClickListener {
            // Create an Intent that opens WaterActivity.
            val water = Intent(this@HomePage, WaterActivity::class.java)
            // Start WaterActivity.
            startActivity(water)
        }

        // ---------------------------------------------------------------------
        // GRAPHS / STATISTICS BUTTON
        // ---------------------------------------------------------------------
        // Set the click listener for the Graphs/Statistics button.
        graphPage.setOnClickListener {
            // Create an Intent that opens WaterChartActivity.
            val graph = Intent(this@HomePage, WaterChartActivity::class.java)
            // Start WaterChartActivity.
            startActivity(graph)
        }

        // ---------------------------------------------------------------------
        // DAILY WATER GOAL BUTTON
        // ---------------------------------------------------------------------

        // Set the click listener for the Daily Water Goal button.
        dailyGoal.setOnClickListener {
            // Create an Intent that opens DailyWaterGoal.
            val daily = Intent(this@HomePage, DailyWaterGoal::class.java)
            // Start DailyWaterGoal.
            startActivity(daily)
        }
    }
}