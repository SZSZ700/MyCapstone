package com.example.myfinaltopapplication
import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Intent
import android.content.SharedPreferences
import android.icu.text.SimpleDateFormat
import android.os.Bundle
import android.os.SystemClock
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit
import org.json.JSONObject
import java.util.Collections
import java.util.Locale
import java.util.concurrent.CompletableFuture
import kotlin.math.roundToInt

// -- FOR NOTIFICATIONS -- //
private const val KEY_WATER_REMINDER_ENABLED = "waterReminderEnabled"
private const val REMINDER_REQ_CODE = 2001
// -- FOR NOTIFICATIONS -- //

// -------------------------------------------------------------
// WaterActivity - Activity to track and update water consumption.
// -------------------------------------------------------------
class WaterActivity : AppCompatActivity() {
    // TextView to display how much water was consumed today.
    private lateinit var totalWaterText: TextView

    // TextView to display how much water was consumed yesterday.
    private lateinit var yesterdayText: TextView

    // Button to log 150 ml of water.
    private lateinit var drink150: Button

    // Button to log 200 ml of water.
    private lateinit var drink200: Button

    // Button to log 1000 ml of water.
    private lateinit var drink1000: Button

    // ImageButton to navigate back to Home page.
    private lateinit var bhome: ImageButton

    // Holds the currently logged-in username (loaded from SharedPreferences).
    private lateinit var currentUser: String
    // Holds the total water consumed today.
    private var totalDrank: Int = 0

    // TextView to show goal consistency title.
    private lateinit var goalSummaryTitle: TextView
    // TextView to show "X days out of 7 ...".
    private lateinit var goalSummaryText: TextView
    // ProgressBar to visualize percent of days reaching the goal.
    private lateinit var goalProgressBar: ProgressBar
    // TextView for best drinking day.
    private lateinit var bestDayText: TextView
    // TextView for lowest (non-zero) drinking day.
    private lateinit var lowestDayText: TextView

    // -- FOR NOTIFICATIONS -- //
    @SuppressLint("UseSwitchCompatOrMaterialCode")
    private lateinit var switchWaterReminder: Switch
    // -- FOR NOTIFICATIONS -- //

    // -- FOR CUSTOM WATER -- //
    private lateinit var customWaterText: EditText
    private lateinit var customWaterButton: Button
    // -- FOR CUSTOM WATER -- //

    // -------------------------------------------------------------------------
    // onCreate - called when the Activity is first created.
    // -------------------------------------------------------------------------
    @SuppressLint("SetTextI18n", "ApplySharedPref")
    override fun onCreate(savedInstanceState: Bundle?) {
        // Call parent implementation to initialize Activity.
        super.onCreate(savedInstanceState)
        // Load the UI layout from XML (activity_water.xml).
        setContentView(R.layout.activity_water)

        // Load user session preferences (local storage).
        val prefs: SharedPreferences = getSharedPreferences(getString(R.string.myprefs), MODE_PRIVATE)
        // Retrieve current logged-in user.
        val savedUser = prefs.getString(getString(R.string.currentuser), null)

        // If no user is logged in -> redirect to LoginActivity.
        if (savedUser == null) {
            // Show error toast.
            Toast.makeText(this, "You must log in first", Toast.LENGTH_SHORT).show()
            // Navigate to LoginActivity.
            val login = Intent(this@WaterActivity, LoginActivity::class.java)
            // Start LoginActivity.
            startActivity(login)
            // Close WaterActivity so the user cannot go back.
            finish()
            // Stop further execution.
            return
        }

        // Save the logged-in username.
        currentUser = savedUser

        // Link Kotlin fields with actual UI components in the layout.
        totalWaterText = findViewById(R.id.totalWaterText)
        yesterdayText = findViewById(R.id.yesterdayText)
        drink150 = findViewById(R.id.drink150)
        drink200 = findViewById(R.id.drink200)
        drink1000 = findViewById(R.id.drink1000)
        bhome = findViewById(R.id.imageButton4)
        switchWaterReminder = findViewById(R.id.switchWaterReminder)
        // Views for goal statistics and best/worst day.
        goalSummaryTitle = findViewById(R.id.goalSummaryTitle)
        goalSummaryText = findViewById(R.id.goalSummaryText)
        goalProgressBar = findViewById(R.id.goalProgressBar)
        bestDayText = findViewById(R.id.bestDayText)
        lowestDayText = findViewById(R.id.lowestDayText)
        // -- FOR CUSTOM WATER -- //
        customWaterText = findViewById(R.id.customWater)
        customWaterButton = findViewById(R.id.button7)
        // -- FOR CUSTOM WATER -- //

        // Load locally saved data for today and yesterday from SharedPreferences.
        totalDrank = prefs.getInt("todayWater", 0)
        // Load yesterday's amount from SharedPreferences.
        val yesterdayAmountLocal = prefs.getInt("yesterdayWater", 0)
        // Update the UI with local values.
        totalWaterText.text = getString(R.string.so_far_today) + totalDrank + getString(R.string.ml1)
        yesterdayText.text = "Yesterday: $yesterdayAmountLocal ml"

        // Fetch latest water log from the backend (Spring Boot -> Firebase).
        RestClient.getWater(currentUser).thenAccept { obj ->
            // Move UI operations back to the Android main thread.
            runOnUiThread {
                // If server returned a valid JSON object.
                if (obj != null) {
                    // Extract today's water amount from JSON (default = 0).
                    val today = obj.optInt("todayWater", 0)
                    // Extract yesterday's water amount from JSON (default = 0).
                    val yesterday = obj.optInt("yesterdayWater", 0)

                    // Update local variable for today's consumption.
                    totalDrank = today
                    // Update the UI with new values from server.
                    totalWaterText.text = "So far today: $today ml"
                    yesterdayText.text = "Yesterday: $yesterday ml"

                    // Save updated values back to SharedPreferences.
                    prefs.edit(commit = true) {
                        // Save today's water.
                        putInt("todayWater", today)
                        // Save yesterday's water.
                        putInt("yesterdayWater", yesterday)
                        // Save changes to SharedPreferences immediately.
                    }

                    // Log values to Logcat.
                    Log.d("WATER_PREFS", "Saved from server: todayWater=$today, yesterdayWater=$yesterday")
                }
            }
        }

        // Set click listener for 150 ml button -> calls updateWater(150).
        drink150.setOnClickListener {
            updateWater(150)
        }
        // Set click listener for 200 ml button -> calls updateWater(200).
        drink200.setOnClickListener {
            updateWater(200)
        }
        // Set click listener for 1000 ml button -> calls updateWater(1000).
        drink1000.setOnClickListener {
            updateWater(1000)
        }

        // -- FOR NOTIFICATIONS -- //
        // Load switch state from SharedPreferences.
        val enabled = prefs.getBoolean(KEY_WATER_REMINDER_ENABLED, false)
        // Restore switch state.
        switchWaterReminder.isChecked = enabled

        // Start reminder if it was previously enabled.
        if (enabled) {
            startWaterReminderEvery2Hours()
        }

        // Set switch listener for water reminder.
        switchWaterReminder.setOnCheckedChangeListener { _, isChecked ->
            // Save switch state to SharedPreferences.
            prefs.edit { putBoolean(KEY_WATER_REMINDER_ENABLED, isChecked) }

            // On/off water reminder.
            if (isChecked) {
                startWaterReminderEvery2Hours()
            } else {
                stopWaterReminder()
            }
        }
        // -- FOR NOTIFICATIONS -- //

        // Set click listener for home button -> navigates to HomePage.
        bhome.setOnClickListener {
            // Create intent to navigate to HomePage.
            val bh = Intent(this@WaterActivity, HomePage::class.java)
            // Start HomePage.
            startActivity(bh)
        }

        // --------------------------------------------------
        // Extra statistics: goal consistency + best/lowest day.
        // --------------------------------------------------

        // Number of days to check for statistics (same as history graph: 7 last days).
        val daysForStats = 7

        // Call backend to get daily totals for last N days.
        val historyFuture: CompletableFuture<JSONObject?> = RestClient.getWaterHistoryMap(currentUser, daysForStats)

        // Handle async response for history.
        historyFuture.thenAccept { obj ->
            // Move UI operations back to the Android main thread.
            runOnUiThread {
                try {
                    // If no data at all from server.
                    if (obj == null || obj.length() == 0) {
                        // Change UI to show no data available.
                        goalSummaryTitle.text = "Goal consistency (last $daysForStats days)"
                        goalSummaryText.text = "No history data available"
                        goalProgressBar.progress = 0
                        bestDayText.text = "Best day: no data"
                        lowestDayText.text = "Lowest day: no data"
                    } else {
                        // Collect all date keys from JSON.
                        val keys: Iterator<String> = obj.keys()
                        // Sort keys in ascending order (oldest -> newest).
                        val sortedKeys = ArrayList<String>()
                        // Add all keys to the list.
                        while (keys.hasNext()) {
                            sortedKeys.add(keys.next())
                        }

                        // Sort dates ascending (oldest -> newest) so indexes are stable.
                        Collections.sort(sortedKeys, Comparator.naturalOrder())

                        // Number of days that actually exist in the JSON.
                        val totalDays = sortedKeys.size

                        // Array to store the total amount per day aligned with sortedKeys.
                        val dailyAmounts = IntArray(totalDays)

                        // Fill the dailyAmounts array.
                        for (i in 0 until totalDays) {
                            // Extract date from the list.
                            val date = sortedKeys[i]
                            // Extract amount for the current date.
                            val amount = obj.optInt(date, 0)
                            // Store the amount in the array.
                            dailyAmounts[i] = amount
                        }

                        // -------------------------------
                        // Find best day and lowest day.
                        // -------------------------------

                        // Best day initialized as "no data".
                        var bestAmount = -1
                        // Best date initialized as null.
                        var bestDate: String? = null

                        // Lowest non-zero day initialized as max int.
                        var lowestAmount = Int.MAX_VALUE
                        // Lowest date initialized as null.
                        var lowestDate: String? = null

                        // Loop through all days and detect best / lowest.
                        for (i in 0 until totalDays) {
                            // Extract amount for the current day.
                            val amount = dailyAmounts[i]
                            // Extract date for the current day.
                            val date = sortedKeys[i]

                            // Update the best day (max amount).
                            if (amount > bestAmount) {
                                bestAmount = amount
                                bestDate = date
                            }

                            // Update the lowest non-zero day (min amount > 0).
                            if (amount in 1..<lowestAmount) {
                                lowestAmount = amount
                                lowestDate = date
                            }
                        }

                        // Prepare labels for display.
                        var bestLabel = bestDate ?: "no data"
                        var lowestLabel = lowestDate ?: "no data"

                        // Try to format dates from yyyy-MM-dd to MM-dd.
                        try {
                            val from = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                            val to = SimpleDateFormat("MM-dd", Locale.getDefault())

                            // Format best date.
                            if (bestDate != null) {
                                bestLabel = to.format(from.parse(bestDate))
                            }

                            // Format lowest date.
                            if (lowestDate != null) {
                                lowestLabel = to.format(from.parse(lowestDate))
                            }
                        } catch (_: Exception) {
                            // If parsing fails, keep original format.
                        }

                        // Update UI for best day.
                        if (bestDate != null) {
                            // Update UI to show best day.
                            bestDayText.text = "Best day: $bestLabel ($bestAmount ml)"
                        } else {
                            // If no best day is available -> show "no data".
                            bestDayText.text = "Best day: no data"
                        }

                        // Update UI for lowest day (non-zero).
                        if (lowestDate != null) {
                            // Update UI to show lowest day.
                            lowestDayText.text = "Lowest day: $lowestLabel ($lowestAmount ml)"
                        } else {
                            // Show no data if there is no non-zero day.
                            lowestDayText.text = "Lowest day: no data"
                        }

                        // -----------------------------------------
                        // Now compute "days on target" vs goalMl.
                        // -----------------------------------------

                        // Call backend to get current daily goal.
                        RestClient.getGoal(currentUser).thenAccept { goalObj ->
                            // Move UI operations back to the Android main thread.
                            runOnUiThread {
                                try {
                                    // If no goal is defined on server.
                                    if (goalObj == null) {
                                        goalSummaryTitle.text = "Goal consistency (last $totalDays days)"
                                        goalSummaryText.text = "Goal not available"
                                        goalProgressBar.progress = 0
                                    } else {
                                        // Extract goalMl from JSON (fallback = 3000ml).
                                        val goalMl = goalObj.optInt("goalMl", 3000)

                                        // Count days where daily total >= goalMl.
                                        var daysReached = 0
                                        // Loop through all days and count days that reached the goal.
                                        for (i in 0 until totalDays) {
                                            // Check if current day's total >= goalMl.
                                            if (dailyAmounts[i] >= goalMl) {
                                                // Increase count of days reached.
                                                daysReached++
                                            }
                                        }

                                        // Compute percentage of days that reached the goal.
                                        val percent = ((daysReached * 100.0) / totalDays).roundToInt()

                                        // Update UI with summary and progress bar.
                                        goalSummaryTitle.text = "Goal consistency (last $totalDays days)"
                                        goalSummaryText.text = "Days on target: $daysReached / $totalDays ($percent%)"
                                        goalProgressBar.max = 100
                                        goalProgressBar.progress = percent
                                    }
                                } catch (_: Exception) {
                                    // If any error occurs while computing goal stats.
                                    goalSummaryText.text = "Error computing goal stats"
                                    goalProgressBar.progress = 0
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    // Any unexpected error in history processing.
                    Log.e("WATER_STATS", "Error computing water statistics", e)
                    goalSummaryTitle.text = "Goal consistency"
                    goalSummaryText.text = "Error loading history"
                    goalProgressBar.progress = 0
                    bestDayText.text = "Best day: error"
                    lowestDayText.text = "Lowest day: error"
                }
            }
        }

        customWaterButton.setOnClickListener {
            // Read user inputs (custom water amount) as strings.
            // Trim to remove leading/trailing spaces.
            val txt = customWaterText.text.toString().trim()

            // Validate that the field is not empty.
            if (txt.isEmpty()) {
                // Show error toast if empty input is provided.
                Toast.makeText(this, getString(R.string.fill_all_fields), Toast.LENGTH_SHORT).show()
            } else {
                try {
                    // Convert amount of water to number.
                    val amountOfWater = txt.toInt()

                    // Validate that the input is a positive number.
                    // And that it is not greater than 10000.
                    if (amountOfWater !in 1..<10000) {
                        // Show invalid input message.
                        Toast.makeText(this, "Invalid input", Toast.LENGTH_SHORT).show()
                    } else {
                        // Update water consumption.
                        // Call updateWater(amountOfWater) method.
                        updateWater(amountOfWater)
                    }
                } catch (_: NumberFormatException) {
                    // If the input is not a valid number -> show error toast.
                    Toast.makeText(this, "Invalid input", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // -------------------------------------------------------------------------
    // updateWater - called when user logs new water consumption.
    // amount = amount of water consumed (ml).
    // -------------------------------------------------------------------------
    @SuppressLint("SetTextI18n", "ApplySharedPref")
    private fun updateWater(amount: Int) {
        // Increase today's total amount.
        totalDrank += amount
        // Update the UI immediately.
        totalWaterText.text = "So far today: $totalDrank ml"

        // Call backend API to update water log in Firebase.
        RestClient.updateWater(currentUser, amount).thenAccept { success ->
            // Move UI operations back to the Android main thread.
            runOnUiThread {
                // If update succeeded.
                if (success) {
                    // Save updated total to SharedPreferences.
                    val prefs = getSharedPreferences(getString(R.string.myprefs), MODE_PRIVATE)
                    // Create editor to save locally.
                    prefs.edit(commit = true) {
                        // Save today's total.
                        putInt("todayWater", totalDrank)
                        // Save changes to SharedPreferences immediately.
                    }

                    // Log value to Logcat.
                    Log.d("WATER_PREFS", "Updated locally: todayWater=$totalDrank")

                    // Show confirmation toast.
                    Toast.makeText(this@WaterActivity, "+$amount ml saved!", Toast.LENGTH_SHORT).show()
                } else {
                    // If server update failed, show error toast.
                    Toast.makeText(this@WaterActivity, "Failed to update water", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // -------------------------------------------------------------------------
    // startWaterReminderEvery2Hours - called when user enables water reminder.
    // -------------------------------------------------------------------------
    private fun startWaterReminderEvery2Hours() {
        // Get AlarmManager instance.
        val am = getSystemService(ALARM_SERVICE) as AlarmManager

        // Create Intent to start WaterReminderReceiver.
        val i = Intent(this, WaterReminderReceiver::class.java)

        // Create PendingIntent to handle the Intent.
        val pi = PendingIntent.getBroadcast(
            // Context.
            this,
            // Request code for the PendingIntent.
            REMINDER_REQ_CODE,
            // Intent to start WaterReminderReceiver.
            i,
            // Flags for the PendingIntent.
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Set repeating alarm every 2 hours.
        val intervalMillis = 2L * 60L * 60L * 1000L // 2 hours.
        // Set first trigger time.
        val firstTrigger = SystemClock.elapsedRealtime() + intervalMillis

        // Schedule the alarm.
        am.setInexactRepeating(
            // Alarm type.
            AlarmManager.ELAPSED_REALTIME_WAKEUP,
            // First trigger time.
            firstTrigger,
            // Interval between triggers.
            intervalMillis,
            // PendingIntent to trigger.
            pi
        )
    }

    // -------------------------------------------------------------------------
    // stopWaterReminder - called when user disables water reminder.
    // -------------------------------------------------------------------------
    private fun stopWaterReminder() {
        // Get AlarmManager instance.
        val am = getSystemService(ALARM_SERVICE) as AlarmManager

        // Create Intent to stop WaterReminderReceiver.
        val i = Intent(this, WaterReminderReceiver::class.java)

        // Create PendingIntent to handle the Intent.
        val pi = PendingIntent.getBroadcast(
            // Context.
            this,
            // Request code for the PendingIntent.
            REMINDER_REQ_CODE,
            // Intent to stop the reminder.
            i,
            // Flags for the PendingIntent.
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Cancel the alarm.
        am.cancel(pi)
        // Cancel the PendingIntent.
        pi.cancel()
    }
}