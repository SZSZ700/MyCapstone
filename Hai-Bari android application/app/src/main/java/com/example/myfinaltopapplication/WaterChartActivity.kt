@file:Suppress("UseWithIndex")

package com.example.myfinaltopapplication
// Import Android base classes for UI and logging.
import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.ImageButton
import android.widget.TextView
// Import AppCompatActivity as base class for Activity.
import androidx.appcompat.app.AppCompatActivity
// Import Java utilities for formatting and collections.
import java.text.SimpleDateFormat
import java.util.ArrayList
import java.util.Collections
import java.util.Comparator
import java.util.Locale
// Import MPAndroidChart library for chart rendering.
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.ValueFormatter

// ------------------------------------------------------
// WaterChartActivity
// Purpose: Show user's water consumption in 2 charts:
// 1. Last 7 days (daily amounts)
// 2. Weekly averages (4 weeks)
// ------------------------------------------------------
@Suppress("DEPRECATION")
class WaterChartActivity : AppCompatActivity() {
    // Bar chart for last 7 days.
    private lateinit var barChart7days: BarChart

    // Bar chart for weekly averages.
    private lateinit var barChartWeekly: BarChart

    // Title above charts.
    private lateinit var chartTitle: TextView

    // Back button for navigation.
    private lateinit var backButton: ImageButton

    // ----------------------------------------------------------------
    // onCreate - entry point when activity is created.
    // ----------------------------------------------------------------
    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        // Call parent implementation.
        super.onCreate(savedInstanceState)

        // Inflate the layout XML file for this Activity.
        setContentView(R.layout.activity_water_chart)

        // Link UI components to Kotlin objects.
        barChart7days = findViewById(R.id.barChart7days)   // Chart for 7 days.
        barChartWeekly = findViewById(R.id.barChartWeekly) // Chart for weekly averages.
        chartTitle = findViewById(R.id.chartTitle)         // Title text.
        backButton = findViewById(R.id.imageButton)        // Back button.

        // Retrieve logged-in username from SharedPreferences (fallback = "guest").
        val username = getSharedPreferences(getString(R.string.myprefs), MODE_PRIVATE)
            .getString(getString(R.string.currentuser), "guest") ?: "guest"

        // Number of days for daily chart (last 7 days).
        val days = 7

        // --------------------------------------------------
        // 1. Fetch last 7 days data from REST client.
        // --------------------------------------------------
        val future = RestClient.getWaterHistoryMap(username, days)

        // Handle asynchronous response.
        future.thenAccept { obj ->
            // Move UI operations back to the Android main thread.
            runOnUiThread {
                try {
                    // If no data returned -> show message.
                    if (obj == null) {
                        chartTitle.text = "No data available"
                    } else {
                        // Log received JSON for debugging.
                        Log.d("CHART_DATA", "Got JSON: $obj")

                        // Prepare chart entries (bars) and labels (X-axis).
                        val entries = ArrayList<BarEntry>()
                        val labels = ArrayList<String>()

                        // Collect JSON keys (dates).
                        val keys = obj.keys()
                        // Convert to ArrayList for sorting.
                        val sortedKeys = ArrayList<String>()
                        // Add all keys to list.
                        while (keys.hasNext()) {
                            sortedKeys.add(keys.next())
                        }

                        // Sort the dates in ascending order.
                        Collections.sort(sortedKeys, Comparator.naturalOrder())

                        // Index used for X position in the chart.
                        var index = 0

                        // Convert each JSON value into a BarEntry.
                        for (date in sortedKeys) {
                            // Get water amount for this date.
                            val amount = obj.optInt(date, 0)

                            // Add entry (X=index, Y=amount).
                            entries.add(BarEntry(index.toFloat(), amount.toFloat()))

                            // Format date to short (MM-dd) for labels.
                            try {
                                // Parse the original yyyy-MM-dd date.
                                val parsedDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(date)!!
                                // Convert the date to MM-dd format.
                                val shortLabel = SimpleDateFormat("MM-dd", Locale.getDefault()).format(parsedDate)
                                // Add label to list.
                                labels.add(shortLabel)
                            } catch (_: Exception) {
                                // Fallback to original date if parsing fails.
                                labels.add(date)
                            }

                            // Increment index.
                            index++
                        }

                        // Create dataset for 7-day chart.
                        val dataSet = BarDataSet(entries, "")
                        // Set chart bar color.
                        dataSet.setColor(resources.getColor(android.R.color.holo_blue_light))

                        // Configure dataset appearance.
                        val data = BarData(dataSet)
                        // Set bar width.
                        data.barWidth = 0.7f
                        // Set value text size above bar.
                        data.setValueTextSize(10f)

                        // Assign dataset to chart.
                        barChart7days.setData(data)

                        // Configure X-axis for 7-day chart.
                        val xAxis = barChart7days.xAxis
                        // Step size = 1.
                        xAxis.setGranularity(1f)
                        // Labels at bottom.
                        xAxis.position = XAxis.XAxisPosition.BOTTOM
                        // Remove grid lines.
                        xAxis.setDrawGridLines(false)
                        // Rotate labels.
                        xAxis.labelRotationAngle = -45f
                        // Set label size.
                        xAxis.setTextSize(10f)

                        // Custom formatter to show correct labels.
                        xAxis.valueFormatter = object : ValueFormatter() {
                            // Convert X position to the matching date label.
                            override fun getFormattedValue(value: Float): String {
                                // Convert chart position to integer index.
                                val i = value.toInt()
                                // Return matching label when index is valid.
                                if (i >= 0 && i < labels.size) {
                                    return labels[i]
                                }
                                // Return empty text when index is invalid.
                                return ""
                            }
                        }

                        // Remove legend from chart.
                        barChart7days.legend.isEnabled = false
                        // Remove description from chart.
                        barChart7days.description.isEnabled = false

                        // Refresh chart display.
                        barChart7days.invalidate()

                        // Update chart title.
                        chartTitle.text = "Water History - Last $days days"
                    }
                } catch (e: Exception) {
                    // Handle errors gracefully.
                    Log.e("CHART", "Error displaying 7-day chart", e)
                    // Show error message.
                    chartTitle.text = "Error loading chart"
                }
            }
        }

        // --------------------------------------------------
        // 2. Fetch weekly averages data from REST client.
        // --------------------------------------------------
        val futureWeekly = RestClient.getWeeklyAverages(username)

        // Handle asynchronous response.
        futureWeekly.thenAccept { weeklyMap ->
            // Move UI operations back to the Android main thread.
            runOnUiThread {
                try {
                    // If weekly map is empty or null.
                    if (weeklyMap == null || weeklyMap.isEmpty()) {
                        // Log when no weekly data exists.
                        Log.w("CHART_WEEKLY", "No weekly averages found")
                    } else {
                        // 🐈 LOG 🐈 //
                        for (entry in weeklyMap.entries) {
                            // Log each week and its average.
                            Log.d("WEEKLY_MAP", "${entry.key} -> ${entry.value}")
                        }
                        // 🐈 LOG 🐈 //

                        // Prepare entries for weekly chart.
                        val weeklyEntries = ArrayList<BarEntry>()
                        val weekLabels = ArrayList<String>()

                        // Index used for X position in weekly chart.
                        var idx = 0

                        // Convert weekly averages into chart entries.
                        for (entry in weeklyMap.entries) {
                            // Add weekly average to chart.
                            weeklyEntries.add(BarEntry(idx.toFloat(), entry.value.toFloat()))
                            // Add week label to list.
                            weekLabels.add(entry.key)
                            // Increment index.
                            idx++
                        }

                        // Create dataset for weekly chart.
                        val weeklySet = BarDataSet(weeklyEntries, "")
                        // Set chart bar color.
                        weeklySet.setColor(resources.getColor(android.R.color.holo_green_light))

                        // Configure dataset styling.
                        val weeklyData = BarData(weeklySet)
                        // Set bar width.
                        weeklyData.barWidth = 0.5f
                        // Set value text size.
                        weeklyData.setValueTextSize(10f)

                        // Assign dataset to weekly chart.
                        barChartWeekly.setData(weeklyData)

                        // Configure X-axis for weekly chart.
                        val xAxisW = barChartWeekly.xAxis
                        // Step size = 1.
                        xAxisW.setGranularity(1f)
                        // Labels at bottom.
                        xAxisW.position = XAxis.XAxisPosition.BOTTOM
                        // Remove grid lines.
                        xAxisW.setDrawGridLines(false)
                        // Set label size.
                        xAxisW.setTextSize(12f)

                        // Custom formatter for week labels.
                        xAxisW.valueFormatter = object : ValueFormatter() {
                            // Convert X position to the matching week label.
                            override fun getFormattedValue(value: Float): String {
                                // Convert chart position to integer index.
                                val i = value.toInt()
                                // Return matching label when index is valid.
                                if (i >= 0 && i < weekLabels.size) {
                                    return weekLabels[i]
                                }
                                // Return empty text when index is invalid.
                                return ""
                            }
                        }

                        // Remove legend from weekly chart.
                        barChartWeekly.legend.isEnabled = false
                        // Remove description from weekly chart.
                        barChartWeekly.description.isEnabled = false

                        // Refresh chart display.
                        barChartWeekly.invalidate()
                    }
                } catch (e: Exception) {
                    // Handle error in weekly chart.
                    Log.e("CHART_WEEKLY", "Error displaying weekly chart", e)
                }
            }
        }

        // Set click listener for back button.
        backButton.setOnClickListener {
            // Create intent to navigate to HomePage.
            val intent = Intent(this@WaterChartActivity, HomePage::class.java)
            // Start HomePage.
            startActivity(intent)
        }
    }
}