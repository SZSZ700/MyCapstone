// Define the package this Activity belongs to
package com.example.myfinaltopapplication;
// Import Android Intent class for navigation between screens
import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
// Import SharedPreferences for saving and loading user session data locally
import android.content.SharedPreferences;
// Import Bundle for saving/restoring Activity state
import android.icu.text.SimpleDateFormat;
import android.os.Bundle;
// Import all UI widgets like TextView, Button, Spinner, Toast
import android.os.SystemClock;
import android.widget.*;
// Import AppCompatActivity as the base class for Activities
import androidx.appcompat.app.AppCompatActivity;
// Import CompletableFuture for handling async REST API calls
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
// Import Log for debugging
import android.util.Log;

import org.json.JSONObject;

// -------------------------------------------------------------
// WaterActivity - Activity to track and update water consumption
// -------------------------------------------------------------
public class WaterActivity extends AppCompatActivity {
    // TextView to display how much water was consumed today
    private TextView totalWaterText;

    // TextView to display how much water was consumed yesterday
    @SuppressWarnings("FieldCanBeLocal")
    private TextView yesterdayText;

    // Button to log 150 ml of water
    @SuppressWarnings("FieldCanBeLocal")
    private Button drink150;

    // Button to log 200 ml of water
    @SuppressWarnings("FieldCanBeLocal")
    private Button drink200;

    // Button to log 1000 ml of water
    @SuppressWarnings("FieldCanBeLocal")
    private Button drink1000;

    // ImageButton to navigate back to Home page
    @SuppressWarnings("FieldCanBeLocal")
    private ImageButton bhome;

    // Holds the currently logged-in username (loaded from SharedPreferences)
    private String currentUser;
    // Holds the total water consumed today
    private int totalDrank = 0;

    // TextView to show goal consistency title
    private TextView goalSummaryTitle;
    // TextView to show "X days out of 7 ..."
    private TextView goalSummaryText;
    // ProgressBar to visualize percent of days reaching the goal
    private ProgressBar goalProgressBar;
    // TextView for best drinking day
    private TextView bestDayText;
    // TextView for lowest (non-zero) drinking day
    private TextView lowestDayText;

    // -- FOR NOTIFICATIONS -- //
    @SuppressLint("UseSwitchCompatOrMaterialCode")
    @SuppressWarnings("FieldCanBeLocal")
    private Switch switchWaterReminder;
    private static final String KEY_WATER_REMINDER_ENABLED = "waterReminderEnabled";

    private static final int REMINDER_REQ_CODE = 2001;
    // -- FOR NOTIFICATIONS -- //


    // -- FOR CUSTOM WATER -- //
    @SuppressWarnings("FieldCanBeLocal")
    private EditText customWaterText;

    @SuppressWarnings("FieldCanBeLocal")
    private Button customWaterButton;
    // -- FOR CUSTOM WATER -- //

    // -------------------------------------------------------------------------
    // onCreate - called when the Activity is first created
    // -------------------------------------------------------------------------
    @SuppressLint({"SetTextI18n", "ApplySharedPref"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Call parent implementation to initialize Activity
        super.onCreate(savedInstanceState);
        // Load the UI layout from XML (activity_water.xml)
        setContentView(R.layout.activity_water);

        // Load user session preferences (local storage)
        SharedPreferences prefs = getSharedPreferences(getString(R.string.myprefs), MODE_PRIVATE);
        // Retrieve current logged-in user
        currentUser = prefs.getString(getString(R.string.currentuser), null);

        // If no user is logged in → redirect to LoginActivity
        if (currentUser == null) {
            // Show error toast
            Toast.makeText(this, "You must log in first", Toast.LENGTH_SHORT).show();
            // Navigate to LoginActivity
            Intent login = new Intent(WaterActivity.this, LoginActivity.class);
            // Start LoginActivity
            startActivity(login);
            // Close WaterActivity so the user cannot go back
            finish();
            // Stop further execution
            return;
        }

        // Link Java fields with actual UI components in the layout
        totalWaterText = findViewById(R.id.totalWaterText);
        yesterdayText = findViewById(R.id.yesterdayText);
        drink150 = findViewById(R.id.drink150);
        drink200 = findViewById(R.id.drink200);
        drink1000 = findViewById(R.id.drink1000);
        bhome = findViewById(R.id.imageButton4);
        switchWaterReminder = findViewById(R.id.switchWaterReminder);
        // views for goal statistics and best/worst day
        goalSummaryTitle = findViewById(R.id.goalSummaryTitle);
        goalSummaryText = findViewById(R.id.goalSummaryText);
        goalProgressBar = findViewById(R.id.goalProgressBar);
        bestDayText = findViewById(R.id.bestDayText);
        lowestDayText = findViewById(R.id.lowestDayText);
        // -- FOR CUSTOM WATER -- //
        customWaterText = findViewById(R.id.customWater);
        customWaterButton = findViewById(R.id.button7);
        // -- FOR CUSTOM WATER -- //


        // Load locally saved data for today and yesterday from SharedPreferences
        totalDrank = prefs.getInt("todayWater", 0);
        // Load yesterday's amount from SharedPreferences
        var yesterdayAmountLocal = prefs.getInt("yesterdayWater", 0);
        // Update the UI with local values
        totalWaterText.setText(getString(R.string.so_far_today) + totalDrank + getString(R.string.ml1));
        yesterdayText.setText("Yesterday: " + yesterdayAmountLocal + " ml");

        // Fetch latest water log from the backend (Spring Boot → Firebase)
        RestClient.getWater(currentUser).thenAccept(obj -> runOnUiThread(() -> {
            // If server returned a valid JSON object
            if (obj != null) {
                // Extract today's water amount from JSON (default = 0)
                var today = obj.optInt("todayWater", 0);
                // Extract yesterday's water amount from JSON (default = 0)
                var yesterday = obj.optInt("yesterdayWater", 0);

                // Update local variable for today's consumption
                totalDrank = today;
                // Update the UI with new values from server
                totalWaterText.setText("So far today: " + today + " ml");
                yesterdayText.setText("Yesterday: " + yesterday + " ml");

                // Save updated values back to SharedPreferences
                SharedPreferences.Editor editor = prefs.edit();
                editor.putInt("todayWater", today);
                editor.putInt("yesterdayWater", yesterday);
                // save changes to SharedPreferences immediately
                editor.commit();

                // Log values to Logcat
                Log.d("WATER_PREFS", "Saved from server: todayWater=" + today + ", yesterdayWater=" + yesterday);
            }
        }));

        // Set click listener for 150 ml button → calls updateWater(150)
        drink150.setOnClickListener(v -> updateWater(150));
        // Set click listener for 200 ml button → calls updateWater(200)
        drink200.setOnClickListener(v -> updateWater(200));
        // Set click listener for 1000 ml button → calls updateWater(1000)
        drink1000.setOnClickListener(v -> updateWater(1000));

        // -- FOR NOTIFICATIONS -- //
        // Load switch state from SharedPreferences
        boolean enabled = prefs.getBoolean(KEY_WATER_REMINDER_ENABLED, false);
        switchWaterReminder.setChecked(enabled);
        if (enabled) startWaterReminderEvery2Hours();

        // Set switch listener for water reminder
        switchWaterReminder.setOnCheckedChangeListener((buttonView, isChecked) -> {

            // Save switch state to SharedPreferences
            prefs.edit().putBoolean(KEY_WATER_REMINDER_ENABLED, isChecked).apply();

            // on/off water reminder
            if (isChecked) {
                startWaterReminderEvery2Hours();
            } else {
                stopWaterReminder();
            }
        });
        // -- FOR NOTIFICATIONS -- //

        // Set click listener for home button → navigates to HomePage
        bhome.setOnClickListener(v -> {
            // create intent to navigate to HomePage
            Intent bh = new Intent(WaterActivity.this, HomePage.class);
            // start HomePage
            startActivity(bh);
        });

        // --------------------------------------------------
        // Extra statistics: goal consistency + best/lowest day
        // --------------------------------------------------

        // Number of days to check for statistics (same as history graph: 7 last days)
        var daysForStats = 7;

        // Call backend to get daily totals for last N days
        CompletableFuture<JSONObject> historyFuture =
                RestClient.getWaterHistoryMap(currentUser, daysForStats);

        // Handle async response for history
        historyFuture.thenAccept(obj -> runOnUiThread(() -> {
            try {
                // If no data at all from server
                if (obj == null || obj.length() == 0) {
                    // change UI to show no data available
                    goalSummaryTitle.setText("Goal consistency (last " + daysForStats + " days)");
                    goalSummaryText.setText("No history data available");
                    goalProgressBar.setProgress(0);
                    bestDayText.setText("Best day: no data");
                    lowestDayText.setText("Lowest day: no data");
                    return;
                }

                // Collect all date keys from JSON
                Iterator<String> keys = obj.keys();
                // Sort keys in ascending order (oldest -> newest)
                var sortedKeys = new ArrayList<String>();
                // Add all keys to the list
                while (keys.hasNext()) {
                    sortedKeys.add(keys.next());
                }

                // Sort dates ascending (oldest -> newest) so indexes are stable
                //noinspection Java8ListSort
                Collections.sort(sortedKeys, Comparator.naturalOrder());

                // Number of days that actually exist in the JSON
                var totalDays = sortedKeys.size();

                // Array to store the total amount per day aligned with sortedKeys
                var dailyAmounts = new int[totalDays];

                // Fill the dailyAmounts array
                for (int i = 0; i < totalDays; i++) {
                    // Extract date from the list
                    var date = sortedKeys.get(i);
                    // Extract amount for the current date
                    var amount = obj.optInt(date, 0);
                    // Store the amount in the array
                    dailyAmounts[i] = amount;
                }

                // -------------------------------
                // Find best day and lowest day
                // -------------------------------

                // Best day initialized as "no data"
                var bestAmount = -1;
                // Best date initialized as null
                String bestDate = null;

                // Lowest non-zero day initialized as max int
                var lowestAmount = Integer.MAX_VALUE;
                // Lowest date initialized as null
                String lowestDate = null;

                // Loop through all days and detect best / lowest
                for (var i = 0; i < totalDays; i++) {
                    // Extract amount and date for the current day
                    var amount = dailyAmounts[i];
                    // Extract date for the current day
                    var date = sortedKeys.get(i);

                    // Update the best day (max amount)
                    if (amount > bestAmount) {
                        bestAmount = amount;
                        bestDate = date;
                    }

                    // Update the lowest non-zero day (min amount > 0)
                    if (amount > 0 && amount < lowestAmount) {
                        lowestAmount = amount;
                        lowestDate = date;
                    }
                }

                // Prepare labels for display
                var bestLabel = (bestDate != null) ? bestDate : "no data";
                var lowestLabel = (lowestDate != null) ? lowestDate : "no data";

                // Try to format dates from yyyy-MM-dd to MM-dd
                try {
                    var from = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                    var to = new SimpleDateFormat("MM-dd", Locale.getDefault());

                    if (bestDate != null) { bestLabel = to.format(from.parse(bestDate)); }

                    if (lowestDate != null) { lowestLabel = to.format(from.parse(lowestDate)); }
                } catch (Exception ignore) {
                    // If parsing fails, keep original format
                }

                // Update UI for best day
                if (bestDate != null) {
                    // update UI to show best day
                    bestDayText.setText("Best day: " + bestLabel + " (" + bestAmount + " ml)");
                } else {
                    // If no best day is available → show "no data"
                    bestDayText.setText("Best day: no data");
                }

                // Update UI for lowest day (non-zero)
                if (lowestDate != null) {
                    lowestDayText.setText("Lowest day: " + lowestLabel + " (" + lowestAmount + " ml)");
                } else {
                    lowestDayText.setText("Lowest day: no data");
                }

                // -----------------------------------------
                // Now compute "days on target" vs goalMl
                // -----------------------------------------

                // Call backend to get current daily goal
                RestClient.getGoal(currentUser).thenAccept(goalObj -> runOnUiThread(() -> {
                    try {
                        // If no goal is defined on server
                        if (goalObj == null) {
                            goalSummaryTitle.setText("Goal consistency (last " + totalDays + " days)");
                            goalSummaryText.setText("Goal not available");
                            goalProgressBar.setProgress(0);
                            return;
                        }

                        // Extract goalMl from JSON (fallback = 3000ml)
                        var goalMl = goalObj.optInt("goalMl", 3000);

                        // Count days where daily total >= goalMl
                        var daysReached = 0;
                        // Loop through all days and count days that reached the goal
                        for (var i = 0; i < totalDays; i++) {
                            // Check if current day's total >= goalMl
                            if (dailyAmounts[i] >= goalMl) {
                                // Increase count of days reached
                                daysReached++;
                            }
                        }

                        // Compute percentage of days that reached the goal
                        var percent = (int) Math.round((daysReached * 100.0) / totalDays);

                        // Update UI with summary and progress bar
                        goalSummaryTitle.setText("Goal consistency (last " + totalDays + " days)");
                        goalSummaryText.setText("Days on target: " + daysReached + " / " + totalDays + " (" + percent + "%)");
                        goalProgressBar.setMax(100);
                        goalProgressBar.setProgress(percent);

                    } catch (Exception e2) {
                        // If any error occurs while computing goal stats
                        goalSummaryText.setText("Error computing goal stats");
                        goalProgressBar.setProgress(0);
                    }
                }));

            } catch (Exception e) {
                // Any unexpected error in history processing
                Log.e("WATER_STATS", "Error computing water statistics", e);
                goalSummaryTitle.setText("Goal consistency");
                goalSummaryText.setText("Error loading history");
                goalProgressBar.setProgress(0);
                bestDayText.setText("Best day: error");
                lowestDayText.setText("Lowest day: error");
            }
        }));

        customWaterButton.setOnClickListener(v -> {
            // Read user inputs (custom water amount) as strings
            // Trim to remove leading/trailing spaces
            var txt = customWaterText.getText().toString().trim();

            // Validate that the field is not empty
            if (txt.isEmpty()) {
                // Show error toast if empty input is provided
                Toast.makeText(this, getString(R.string.fill_all_fields), Toast.LENGTH_SHORT).show();
                return; // Stop if invalid input
            }

            try{
                // Convert amount of water to number
                var amountOfWater = Integer.parseInt(txt);

                // Validate that the input is a positive number
                // and that it is not greater than 10000
                if (amountOfWater <= 0 || amountOfWater >= 10000){
                    Toast.makeText(this, "Invalid input", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Update water consumption
                // call updateWater(amountOfWater) method
                this.updateWater(amountOfWater);
            }catch (NumberFormatException e){
                // If the input is not a valid number → show error toast
                Toast.makeText(this, "Invalid input", Toast.LENGTH_SHORT).show();
            }
        });

    }

    // -------------------------------------------------------------------------
    // updateWater - called when user logs new water consumption
    // amount = amount of water consumed (ml)
    // -------------------------------------------------------------------------
    @SuppressLint({"SetTextI18n", "ApplySharedPref"})
    private void updateWater(int amount) {
        // Increase today's total amount
        totalDrank += amount;
        // Update the UI immediately
        totalWaterText.setText("So far today: " + totalDrank + " ml");

        // Call backend API to update water log in Firebase
        RestClient.updateWater(currentUser, amount).thenAccept(success -> runOnUiThread(() -> {
            // If update succeeded
            if (success) {
                // Save updated total to SharedPreferences
                var prefs = getSharedPreferences(getString(R.string.myprefs), MODE_PRIVATE);
                // create editor to save locally
                SharedPreferences.Editor editor = prefs.edit();
                // save today's total
                editor.putInt("todayWater", totalDrank);
                // save changes to SharedPreferences immediately
                editor.commit();

                // Log value to Logcat
                Log.d("WATER_PREFS", "Updated locally: todayWater=" + totalDrank);

                // Show confirmation toast
                Toast.makeText(this, "+" + amount + " ml saved!", Toast.LENGTH_SHORT).show();
            } else {
                // If server update failed, show error toast
                Toast.makeText(this, "Failed to update water", Toast.LENGTH_SHORT).show();
            }
        }));
    }

    // -------------------------------------------------------------------------
    // startWaterReminderEvery2Hours - called when user enables water reminder
    // -------------------------------------------------------------------------
    private void startWaterReminderEvery2Hours() {

        // Get AlarmManager instance
        AlarmManager am = (AlarmManager) getSystemService(Context.ALARM_SERVICE);

        // Create Intent to start WaterReminderReceiver
        Intent i = new Intent(this, WaterReminderReceiver.class);

        // Create PendingIntent to handle the Intent
        PendingIntent pi = PendingIntent.getBroadcast(
                // Context
                this,
                // Request code for the PendingIntent
                REMINDER_REQ_CODE,
                // Intent to start WaterReminderReceiver
                i,
                // Flags for the PendingIntent
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // Set repeating alarm every 2 hours
        long intervalMillis = 2L * 60L * 60L * 1000L; // 2 hours
        // Set first trigger time
        long firstTrigger = SystemClock.elapsedRealtime() + intervalMillis;

        // Schedule the alarm
        am.setInexactRepeating(
                // Alarm type
                AlarmManager.ELAPSED_REALTIME_WAKEUP,
                // First trigger time
                firstTrigger,
                // Interval between triggers
                intervalMillis,
                pi
        );
    }

    // -------------------------------------------------------------------------
    // stopWaterReminder - called when user disable water reminder
    // -------------------------------------------------------------------------
    private void stopWaterReminder() {

        // Get AlarmManager instance
        AlarmManager am = (AlarmManager) getSystemService(Context.ALARM_SERVICE);

        // Create Intent to stop WaterReminderReceiver
        Intent i = new Intent(this, WaterReminderReceiver.class);

        // Create PendingIntent to handle the Intent
        PendingIntent pi = PendingIntent.getBroadcast(
                // Context
                this,
                // Request code for the PendingIntent
                REMINDER_REQ_CODE,
                // Intent to stop the reminder
                i,
                // Flags for the PendingIntent
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // Cancel the alarm
        am.cancel(pi);
        // Cancel the PendingIntent
        pi.cancel();
    }
}

