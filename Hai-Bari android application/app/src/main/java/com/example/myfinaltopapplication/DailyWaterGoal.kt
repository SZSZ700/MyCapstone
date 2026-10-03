package com.example.myfinaltopapplication
// Android imports.
import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
// AppCompat.
import androidx.appcompat.app.AppCompatActivity
// JSON.
import org.json.JSONObject
// Java utils.
import java.util.Locale
import java.util.concurrent.CompletableFuture
// MPAndroidChart (PieChart used as donut).
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.ValueFormatter
import com.github.mikephil.charting.utils.ColorTemplate

class DailyWaterGoal : AppCompatActivity() {
    // UI references.
    private lateinit var donutChart: PieChart                 // Donut chart instance.
    @Suppress("unused")
    private lateinit var titleText: TextView                  // Page title.
    private lateinit var goalText: TextView                   // Shows current goal (ml).
    private lateinit var todayText: TextView                  // Shows today consumption (ml).
    private lateinit var goalInput: EditText                  // Input for new goal.
    private lateinit var saveGoalBtn: Button                  // Button to save new goal.
    private lateinit var backButton: ImageButton              // Back navigation.
    private lateinit var loading: ProgressBar                 // Loading spinner.

    // Data.
    private lateinit var username: String                     // Current username.
    private var goalMl: Int = 3000                            // Default goal if server empty.
    private var todayMl: Int = 0                              // Today water amount.

    override fun onCreate(savedInstanceState: Bundle?) {
        // Call parent implementation.
        super.onCreate(savedInstanceState)
        // Inflate layout.
        setContentView(R.layout.activity_daily_water_goal)

        // Bind views.
        donutChart = findViewById(R.id.donutChart)
        titleText = findViewById(R.id.titleText)
        goalText = findViewById(R.id.goalText)
        todayText = findViewById(R.id.todayText)
        goalInput = findViewById(R.id.goalInput)
        saveGoalBtn = findViewById(R.id.saveGoalBtn)
        backButton = findViewById(R.id.backButton)
        loading = findViewById(R.id.loading)

        // Read current user from SharedPreferences (fallback "guest").
        username = getSharedPreferences(getString(R.string.myprefs), MODE_PRIVATE)
            .getString(getString(R.string.currentuser), "guest") ?: "guest"

        // Configure chart visual once (static styling).
        setupChartAppearance()

        // Back click -> return to HomePage.
        backButton.setOnClickListener {
            // Create intent to navigate to HomePage.
            val intent = Intent(this@DailyWaterGoal, HomePage::class.java)
            // Start HomePage.
            startActivity(intent)
        }

        // Save goal handler.
        saveGoalBtn.setOnClickListener {
            // Handle Save Goal button click.
            onSaveGoalClicked()
        }

        // Initial data load.
        fetchAndRender()
    }

    override fun onResume() {
        // Call parent implementation.
        super.onResume()
        // Refresh whenever page is visible again (in case goal changed elsewhere).
        fetchAndRender()
    }

    // Handles click on "Save goal" button.
    private fun onSaveGoalClicked() {
        // Get the text and trim leading/trailing spaces.
        val txt = goalInput.text.toString().trim()

        // Validate that input is not empty.
        if (TextUtils.isEmpty(txt)) {
            // Show validation message.
            Toast.makeText(this, "Enter a daily goal in ml", Toast.LENGTH_SHORT).show()
            // Stop execution.
            return
        }

        // Declare variable for parsed goal.
        val newGoal: Int

        try {
            // Parse goal to integer.
            newGoal = txt.toInt()
        } catch (_: NumberFormatException) {
            // Show error if goal is not a valid number.
            Toast.makeText(this, "Goal must be a number", Toast.LENGTH_SHORT).show()
            // Stop execution.
            return
        }

        // Basic sanity range.
        if (newGoal !in 500..10000) {
            // Show error if goal is outside the allowed range.
            Toast.makeText(this, "Goal should be between 500 and 10000 ml", Toast.LENGTH_SHORT).show()
            // Stop execution.
            return
        }

        // Show loading.
        setLoading(true)

        // Call REST to save goal.
        RestClient.setGoal(username, newGoal).thenAccept { ok ->
            // Move UI operations back to the Android main thread.
            runOnUiThread {
                // Hide loading.
                setLoading(false)

                // Check whether update succeeded.
                if (ok) {
                    // Update local state.
                    goalMl = newGoal
                    // Show success toast.
                    Toast.makeText(this@DailyWaterGoal, "Goal updated", Toast.LENGTH_SHORT).show()
                    // Update labels.
                    updateHeaderLabels()
                    // Update chart.
                    renderChart()
                } else {
                    // Log failed update.
                    Log.e("DAILY_WATER", "Failed to update goal")
                    // Show error toast if failed.
                    Toast.makeText(this@DailyWaterGoal, "Failed to update goal", Toast.LENGTH_SHORT).show()
                }
            }
        }.exceptionally { ex ->
            // Handle error on the Android main thread.
            runOnUiThread {
                // Hide loading on error.
                setLoading(false)
                // Show error toast with exception message.
                Toast.makeText(this@DailyWaterGoal, "Error: ${ex.message}", Toast.LENGTH_SHORT).show()
            }

            // Return null for CompletableFuture exceptionally().
            null
        }
    }

    // Loads goal + today from server, then renders the donut.
    private fun fetchAndRender() {
        // Show spinner.
        setLoading(true)

        // Request goal (GET /{username}/goal).
        val fGoal: CompletableFuture<JSONObject> = RestClient.getGoal(username)

        // Request today water (GET /{username}/water).
        val fWater: CompletableFuture<JSONObject?> = RestClient.getWater(username)

        // When both are done, update UI.
        CompletableFuture.allOf(fGoal, fWater).thenAccept {
            // Move UI operations back to the Android main thread.
            runOnUiThread {
                try {
                    // Parse goal JSON {"goalMl": 2600}.
                    val jGoal = fGoal.getNow(null)

                    // Check whether goal exists.
                    if (jGoal != null && jGoal.has("goalMl")) {
                        // Read goal from JSON.
                        goalMl = jGoal.optInt("goalMl", goalMl)
                    }

                    // Parse water JSON {"todayWater": 1800, "yesterdayWater": ...}.
                    val jWater = fWater.getNow(null)

                    // Check whether today's water exists.
                    if (jWater != null && jWater.has("todayWater")) {
                        // Read today's water amount.
                        todayMl = jWater.optInt("todayWater", 0)
                    }

                    // Update labels.
                    updateHeaderLabels()
                    // Draw chart.
                    renderChart()
                } catch (e: Exception) {
                    // Log parsing error.
                    Log.e("DONUT", "Parsing error", e)
                    // Show error toast.
                    Toast.makeText(this@DailyWaterGoal, "Failed to parse server data", Toast.LENGTH_SHORT).show()
                } finally {
                    // Hide spinner.
                    setLoading(false)
                }
            }
        }.exceptionally { ex ->
            // Handle any loading error on the Android main thread.
            runOnUiThread {
                // Log loading error.
                Log.e("DONUT", "Error loading data", ex)
                // Hide loading spinner.
                setLoading(false)
                // Show error toast with exception message.
                Toast.makeText(this@DailyWaterGoal, "Load error: ${ex.message}", Toast.LENGTH_SHORT).show()
            }

            // Return null for CompletableFuture exceptionally().
            null
        }
    }

    // Updates top text labels (goal + today).
    private fun updateHeaderLabels() {
        // Update goal label.
        goalText.text = String.format(Locale.getDefault(), "Goal: %,d ml", goalMl)
        // Update today's water label.
        todayText.text = String.format(Locale.getDefault(), "Today: %,d ml", todayMl)
    }

    // Applies static appearance to the donut chart.
    private fun setupChartAppearance() {
        // Use percent values to show % on slices.
        donutChart.setUsePercentValues(true)
        // Disable chart description text.
        donutChart.description.isEnabled = false
        // Enable inner hole to create donut effect.
        donutChart.isDrawHoleEnabled = true
        // Set inner hole size (percent of radius).
        donutChart.holeRadius = 68f
        // Set outer transparent ring size.
        donutChart.transparentCircleRadius = 72f
        // Enable center text.
        // Enable center text.
        donutChart.setDrawCenterText(true)
        // Make center text bold.
        donutChart.setCenterTextTypeface(Typeface.DEFAULT_BOLD)

        // Get chart legend.
        val legend: Legend = donutChart.legend
        // Enable legend.
        legend.isEnabled = true
        // Place legend at the bottom.
        legend.verticalAlignment = Legend.LegendVerticalAlignment.BOTTOM
        // Center legend horizontally.
        legend.horizontalAlignment = Legend.LegendHorizontalAlignment.CENTER
        // Use horizontal legend orientation.
        legend.orientation = Legend.LegendOrientation.HORIZONTAL
        // Allow legend text wrapping.
        legend.isWordWrapEnabled = true

        // Disable labels drawn directly on chart entries.
        donutChart.setDrawEntryLabels(false)
    }

    // Renders the donut with current todayMl and goalMl.
    private fun renderChart() {
        // Compute "remaining" but never negative.
        val remaining = 0.coerceAtLeast(goalMl - todayMl)

        // Build entries: consumed vs remaining.
        val entries = ArrayList<PieEntry>()
        // Add consumed water.
        entries.add(PieEntry(todayMl.toFloat(), "Consumed"))
        // Add remaining water.
        entries.add(PieEntry(remaining.toFloat(), "Remaining"))

        // Create DataSet with material colors.
        val set = PieDataSet(entries, "")
        // Apply material colors.
        set.setColors(*ColorTemplate.MATERIAL_COLORS)
        // Set spacing between slices.
        set.sliceSpace = 2f
        // Set value text size.
        set.valueTextSize = 12f
        // Show values on slices.
        set.setDrawValues(true)

        // Formatter shows percentages with 0 decimals.
        set.valueFormatter = object : ValueFormatter() {
            // Format each slice percentage.
            override fun getPieLabel(value: Float, pieEntry: PieEntry?): String {
                // Return percentage with no decimal places.
                return String.format(Locale.getDefault(), "%.0f%%", value)
            }
        }

        // Attach dataset to chart data.
        val data = PieData(set)
        // Assign data to chart.
        donutChart.data = data

        // Center text shows absolute numbers (e.g., 1800 / 2600).
        donutChart.centerText = String.format(Locale.getDefault(), "%,d / %,d ml", todayMl, goalMl)
        // Set center text size.
        donutChart.setCenterTextSize(16f)

        // Animate chart.
        donutChart.animateY(600)
        // Refresh chart.
        donutChart.invalidate()
    }

    // Shows/hides loading spinner and disables inputs while loading.
    private fun setLoading(show: Boolean) {
        // Show or hide loading spinner.
        if (show) {
            loading.visibility = View.VISIBLE
        } else {
            loading.visibility = View.GONE
        }

        // Disable Save button while loading.
        saveGoalBtn.isEnabled = !show
        // Disable goal input while loading.
        goalInput.isEnabled = !show
    }
}