// Define package
package com.example.myfinaltopapplication;
// Android imports
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
// AppCompat
import androidx.appcompat.app.AppCompatActivity;
// JSON
import org.json.JSONObject;
// Java utils
import java.util.ArrayList;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;

// MPAndroidChart (PieChart used as donut)
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.github.mikephil.charting.utils.ColorTemplate;

public class DailyWaterGoal extends AppCompatActivity {

    // UI references
    private PieChart donutChart;                 // Donut chart instance
    @SuppressWarnings({"FieldCanBeLocal", "unused"})
    private TextView titleText;                  // Page title
    private TextView goalText;                   // Shows current goal (ml)
    private TextView todayText;                  // Shows today consumption (ml)
    private EditText goalInput;                  // Input for new goal
    private Button saveGoalBtn;                  // Button to save new goal
    @SuppressWarnings("FieldCanBeLocal")
    private ImageButton backButton;              // Back navigation
    private ProgressBar loading;                 // Loading spinner

    // Data
    private String username;                     // Current username
    private int goalMl = 3000;                   // Default goal if server empty
    private int todayMl = 0;                     // Today water amount

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Inflate layout
        setContentView(R.layout.activity_daily_water_goal);

        // Bind views
        donutChart  = findViewById(R.id.donutChart);
        titleText   = findViewById(R.id.titleText);
        goalText    = findViewById(R.id.goalText);
        todayText   = findViewById(R.id.todayText);
        goalInput   = findViewById(R.id.goalInput);
        saveGoalBtn = findViewById(R.id.saveGoalBtn);
        backButton  = findViewById(R.id.backButton);
        loading     = findViewById(R.id.loading);

        // Read current user from SharedPreferences (fallback "guest")
        username = getSharedPreferences(getString(R.string.myprefs), MODE_PRIVATE)
                .getString(getString(R.string.currentuser), "guest");

        // Configure chart visual once (static styling)
        setupChartAppearance();

        // Back click → finish
        backButton.setOnClickListener(v -> {
            // create intent to navigate to HomePage
            Intent intent = new Intent(this, HomePage.class);
            // start HomePage
            startActivity(intent);
        });

        // Save goal handler
        saveGoalBtn.setOnClickListener(v -> onSaveGoalClicked());

        // Initial data load
        fetchAndRender();
    }

    @Override
    protected void onResume() {
        // Call parent implementation
        super.onResume();
        // Refresh whenever page is visible again (in case goal changed elsewhere)
        fetchAndRender();
    }

    // Handles click on "Save goal" button
    private void onSaveGoalClicked() {
        // Get the text, validate not empty
        var txt = goalInput.getText().toString().trim();
        if (TextUtils.isEmpty(txt)) {
            Toast.makeText(this, "Enter a daily goal in ml", Toast.LENGTH_SHORT).show();
            return;
        }

        // Parse to integer
        int newGoal;
        try {
            newGoal = Integer.parseInt(txt);
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Goal must be a number", Toast.LENGTH_SHORT).show();
            return;
        }

        // Basic sanity range
        if (newGoal < 500 || newGoal > 10000) {
            Toast.makeText(this, "Goal should be between 500 and 10000 ml", Toast.LENGTH_SHORT).show();
            return;
        }

        // Show loading
        setLoading(true);

        // Call REST to save goal
        RestClient.setGoal(username, newGoal)
                .thenAccept(ok -> runOnUiThread(() -> {
                    // Hide loading
                    setLoading(false);
                    if (ok) {
                        // Update local state and UI
                        goalMl = newGoal;
                        // Show toast and update labels
                        Toast.makeText(this, "Goal updated", Toast.LENGTH_SHORT).show();
                        // Update labels + chart
                        updateHeaderLabels();
                        renderChart();
                    } else {
                        // Show error toast if failed
                        Log.e("DAILY_WATER", "Failed to update goal");
                        Toast.makeText(this, "Failed to update goal", Toast.LENGTH_SHORT).show();
                    }
                }))
                .exceptionally(ex -> {
                    // Hide loading on error
                    runOnUiThread(() -> {
                        setLoading(false);
                        // Show error toast with exception message
                        Toast.makeText(this, "Error: " + ex.getMessage(), Toast.LENGTH_SHORT).show();
                    });
                    // Return null
                    return null;
                });
    }

    // Loads goal + today from server, then renders the donut
    private void fetchAndRender() {
        // Show spinner
        setLoading(true);

        // Request goal (GET /{username}/goal)
        CompletableFuture<JSONObject> fGoal = RestClient.getGoal(username);

        // Request today water (GET /{username}/water)
        CompletableFuture<JSONObject> fWater = RestClient.getWater(username);

        // When both are done, update UI
        CompletableFuture.allOf(fGoal, fWater)
                .thenAccept(v -> runOnUiThread(() -> {
                    try {
                        // Parse goal JSON {"goalMl": 2600}
                        var jGoal = fGoal.getNow(null);

                        if (jGoal != null && jGoal.has("goalMl")) {
                            goalMl = jGoal.optInt("goalMl", goalMl);
                        }

                        // Parse water JSON {"todayWater": 1800, "yesterdayWater": ...}
                        var jWater = fWater.getNow(null);

                        if (jWater != null && jWater.has("todayWater")) {
                            todayMl = jWater.optInt("todayWater", 0);
                        }

                        // Update labels then draw chart
                        updateHeaderLabels();
                        renderChart();

                    } catch (Exception e) {
                        // Log parsing error
                        Log.e("DONUT", "Parsing error", e);
                        // Show toast
                        Toast.makeText(this, "Failed to parse server data", Toast.LENGTH_SHORT).show();
                    } finally {
                        // Hide spinner
                        setLoading(false);
                    }
                }))
                .exceptionally(ex -> {
                    // On any error, hide loader and show toast
                    runOnUiThread(() -> {
                        Log.e("DONUT", "Error loading data", ex);
                        setLoading(false);
                        // Show error toast with exception message
                        Toast.makeText(this, "Load error: " + ex.getMessage(), Toast.LENGTH_SHORT).show();
                    });
                    // Return null
                    return null;
                });
    }

    // Updates top text labels (goal + today)
    private void updateHeaderLabels() {
        goalText.setText(String.format(Locale.getDefault(), "Goal: %,d ml", goalMl));
        todayText.setText(String.format(Locale.getDefault(), "Today: %,d ml", todayMl));
    }

    // Applies static appearance to the donut chart
    private void setupChartAppearance() {
        // Use percent values to show % on slices
        donutChart.setUsePercentValues(true);
        // No inner white description text
        donutChart.getDescription().setEnabled(false);
        // Smooth hole to create donut effect
        donutChart.setDrawHoleEnabled(true);
        donutChart.setHoleRadius(68f);       // Inner hole size (percent of radius)
        donutChart.setTransparentCircleRadius(72f); // Outer transparent ring
        // Disable center text initially
        donutChart.setDrawCenterText(true);
        donutChart.setCenterTextTypeface(Typeface.DEFAULT_BOLD);
        // Tweak legend position/shape
        Legend legend = donutChart.getLegend();
        legend.setEnabled(true);
        legend.setVerticalAlignment(Legend.LegendVerticalAlignment.BOTTOM);
        legend.setHorizontalAlignment(Legend.LegendHorizontalAlignment.CENTER);
        legend.setOrientation(Legend.LegendOrientation.HORIZONTAL);
        legend.setWordWrapEnabled(true);
        // Disable entry labels
        donutChart.setDrawEntryLabels(false);
    }

    // Renders the donut with current todayMl and goalMl
    private void renderChart() {
        // Compute "remaining" but never negative
        var remaining = Math.max(0, goalMl - todayMl);

        // Build entries: consumed vs remaining
        var entries = new ArrayList<PieEntry>();
        entries.add(new PieEntry(todayMl, "Consumed"));
        entries.add(new PieEntry(remaining, "Remaining"));

        // DataSet with nice material colors
        PieDataSet set = new PieDataSet(entries, "");
        set.setColors(ColorTemplate.MATERIAL_COLORS);
        set.setSliceSpace(2f);
        set.setValueTextSize(12f);
        set.setDrawValues(true);

        // Formatter shows percentages with 0 decimals
        set.setValueFormatter(new ValueFormatter() {
            @Override
            public String getPieLabel(float value, PieEntry pieEntry) {
                return String.format(Locale.getDefault(), "%.0f%%", value);
            }
        });

        // Attach dataset to chart data
        var data = new PieData(set);
        donutChart.setData(data);

        // Center text shows absolute numbers (e.g., 1800 / 2600)
        donutChart.setCenterText(String.format(Locale.getDefault(), "%,d / %,d ml", todayMl, goalMl));
        donutChart.setCenterTextSize(16f);

        // Animate and invalidate
        donutChart.animateY(600);
        donutChart.invalidate();
    }

    // Shows/hides loading spinner and disables inputs while loading
    private void setLoading(boolean show) {
        loading.setVisibility(show ? View.VISIBLE : View.GONE);
        saveGoalBtn.setEnabled(!show);
        goalInput.setEnabled(!show);
    }
}
