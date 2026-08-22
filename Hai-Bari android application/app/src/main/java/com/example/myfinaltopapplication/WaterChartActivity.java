// Define package for the application
package com.example.myfinaltopapplication;

// Import Android base classes for UI and logging
import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageButton;
import android.widget.TextView;
// Import AppCompatActivity as base class for Activity
import androidx.appcompat.app.AppCompatActivity;
// Import Java utilities for formatting, collections, and concurrency
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Locale;
// Import MPAndroidChart library for chart rendering
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.ValueFormatter;

// ------------------------------------------------------
// WaterChartActivity
// Purpose: Show user’s water consumption in 2 charts:
// 1. Last 7 days (daily amounts)
// 2. Weekly averages (4 weeks)
// ------------------------------------------------------
public class WaterChartActivity extends AppCompatActivity {

    // Bar chart for last 7 days
    private BarChart barChart7days;

    // Bar chart for weekly averages
    private BarChart barChartWeekly;

    // Title above charts
    private TextView chartTitle;

    // Back button for navigation
    @SuppressWarnings("FieldCanBeLocal")
    private ImageButton backButton;

    // ----------------------------------------------------------------
    // onCreate – entry point when activity is created
    // ----------------------------------------------------------------
    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Call parent constructor
        super.onCreate(savedInstanceState);

        // Inflate the layout XML file for this Activity
        setContentView(R.layout.activity_water_chart);

        // Link UI components to Java objects
        barChart7days = findViewById(R.id.barChart7days);   // Chart for 7 days
        barChartWeekly = findViewById(R.id.barChartWeekly); // Chart for weekly averages
        chartTitle = findViewById(R.id.chartTitle);         // Title text
        backButton = findViewById(R.id.imageButton);        // Back button

        // Retrieve logged-in username from SharedPreferences (fallback = "guest")
        var username = getSharedPreferences(getString(R.string.myprefs), MODE_PRIVATE)
                .getString(getString(R.string.currentuser), "guest");

        // Number of days for daily chart (last 7 days)
        var days = 7;

        // --------------------------------------------------
        // 1. Fetch last 7 days data from REST client
        // --------------------------------------------------
        var future = RestClient.getWaterHistoryMap(username, days);

        // Handle asynchronous response
        future.thenAccept(obj -> runOnUiThread(() -> {
            try {
                // If no data returned → show message
                if (obj == null) {
                    chartTitle.setText("No data available");
                    return;
                }

                // Log received JSON for debugging
                Log.d("CHART_DATA", "Got JSON: " + obj);

                // Prepare chart entries (bars) and labels (X-axis)
                var entries = new ArrayList<BarEntry>();
                var labels = new ArrayList<String>();

                // Collect JSON keys (dates)
                var keys = obj.keys();
                // Convert to ArrayList for sorting
                var sortedKeys = new ArrayList<String>();
                // Add all keys to list
                while (keys.hasNext()) sortedKeys.add(keys.next());

                // Sort the dates in ascending order
                //noinspection Java8ListSort
                Collections.sort(sortedKeys, Comparator.naturalOrder());

                // Convert each JSON value into a BarEntry
                var index = 0;
                for (var date : sortedKeys) {
                    // Get water amount for this date
                    var amount = obj.optInt(date, 0);

                    // Add entry (X=index, Y=amount)
                    entries.add(new BarEntry(index, amount));

                    // Format date to short (MM-dd) for labels
                    try {
                        @SuppressWarnings("DataFlowIssue") var shortLabel = new SimpleDateFormat("MM-dd", Locale.getDefault())
                                .format(new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(date));
                        // Add label to list
                        labels.add(shortLabel);
                    } catch (Exception e) {
                        // Fallback to original date if parsing fails
                        labels.add(date);
                    }
                    // Increment index
                    index++;
                }

                // Create dataset for 7-day chart
                var dataSet = new BarDataSet(entries, "");
                dataSet.setColor(getResources().getColor(android.R.color.holo_blue_light));

                // Configure dataset appearance
                var data = new BarData(dataSet);
                data.setBarWidth(0.7f);       // Bar width
                data.setValueTextSize(10f);   // Value text size above bar

                // Assign dataset to chart
                barChart7days.setData(data);

                // Configure X-axis for 7-day chart
                var xAxis = barChart7days.getXAxis();
                xAxis.setGranularity(1f);                     // Step size = 1
                xAxis.setPosition(XAxis.XAxisPosition.BOTTOM); // Labels at bottom
                xAxis.setDrawGridLines(false);                // Remove grid lines
                xAxis.setLabelRotationAngle(-45);             // Rotate labels
                xAxis.setTextSize(10f);                       // Label size

                // Custom formatter to show correct labels
                xAxis.setValueFormatter(new ValueFormatter() {
                    @Override
                    public String getFormattedValue(float value) {
                        var i = (int) value;
                        if (i >= 0 && i < labels.size()) return labels.get(i);
                        return "";
                    }
                });

                // Remove legend and description from chart
                barChart7days.getLegend().setEnabled(false);
                barChart7days.getDescription().setEnabled(false);

                // Refresh chart display
                barChart7days.invalidate();

                // Update chart title
                chartTitle.setText("Water History - Last " + days + " days");

            } catch (Exception e) {
                // Handle errors gracefully
                Log.e("CHART", "Error displaying 7-day chart", e);
                chartTitle.setText("Error loading chart");
            }
        }));

        // --------------------------------------------------
        // 2. Fetch weekly averages data from REST client
        // --------------------------------------------------
        var futureWeekly = RestClient.getWeeklyAverages(username);

        // Handle asynchronous response
        futureWeekly.thenAccept(weeklyMap -> runOnUiThread(() -> {
            try {
                // If weekly map is empty or null
                if (weeklyMap == null || weeklyMap.isEmpty()) {
                    Log.w("CHART_WEEKLY", "No weekly averages found");
                    return;
                }

                // 🐈 LOG 🐈 //
                for (var entry : weeklyMap.entrySet()) {
                    Log.d("WEEKLY_MAP", entry.getKey() + " -> " + entry.getValue());
                }
                // 🐈 LOG 🐈 //

                // Prepare entries for weekly chart
                var weeklyEntries = new ArrayList<BarEntry>();
                var weekLabels = new ArrayList<String>();

                // Convert weekly averages into chart entries
                var idx = 0;
                // Iterate over map entries
                for (var entry : weeklyMap.entrySet()) {
                    weeklyEntries.add(new BarEntry(idx, entry.getValue()));
                    // Add week label to list
                    weekLabels.add(entry.getKey());
                    // Increment index
                    idx++;
                }

                // Create dataset for weekly chart
                var weeklySet = new BarDataSet(weeklyEntries, "");
                weeklySet.setColor(getResources().getColor(android.R.color.holo_green_light));

                // Configure dataset styling
                var weeklyData = new BarData(weeklySet);
                weeklyData.setBarWidth(0.5f);       // Bar width
                weeklyData.setValueTextSize(10f);   // Value text size

                // Assign dataset to weekly chart
                barChartWeekly.setData(weeklyData);

                // Configure X-axis for weekly chart
                var xAxisW = barChartWeekly.getXAxis();
                xAxisW.setGranularity(1f);                     // Step size = 1
                xAxisW.setPosition(XAxis.XAxisPosition.BOTTOM); // Labels at bottom
                xAxisW.setDrawGridLines(false);                // Remove grid lines
                xAxisW.setTextSize(12f);                       // Label size

                // Custom formatter for week labels
                xAxisW.setValueFormatter(new ValueFormatter() {
                    @Override
                    public String getFormattedValue(float value) {
                        var i = (int) value;
                        if (i >= 0 && i < weekLabels.size()) return weekLabels.get(i);
                        return "";
                    }
                });

                // Remove legend and description
                barChartWeekly.getLegend().setEnabled(false);
                barChartWeekly.getDescription().setEnabled(false);

                // Refresh chart display
                barChartWeekly.invalidate();

            } catch (Exception e) {
                // Handle error in weekly chart
                Log.e("CHART_WEEKLY", "Error displaying weekly chart", e);
            }
        }));

        backButton.setOnClickListener(view -> {
            // Navigate to HomePage
            // create intent to navigate to HomePage
            Intent intent = new Intent(this, HomePage.class);
            // start HomePage
            startActivity(intent);
        });
    }
}
