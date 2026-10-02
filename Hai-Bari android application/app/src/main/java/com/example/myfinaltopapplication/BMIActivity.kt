package com.example.myfinaltopapplication
// Import Android Intent class for navigation between screens.
import android.annotation.SuppressLint
import android.content.Intent
// Import SharedPreferences for local key-value data storage.
// Import Color for PieChart category colors.
// Import Bundle to restore/save Activity state.
import android.os.Bundle
// Import logging for debug messages.
import android.util.Log
// Import Button widget.
import android.widget.Button
// Import EditText for user input fields.
import android.widget.EditText
// Import ImageButton for back navigation.
import android.widget.ImageButton
// Import TextView to display results.
import android.widget.TextView
// Import Toast for short popup messages.
import android.widget.Toast
// Import base class for Android activities.
import androidx.appcompat.app.AppCompatActivity
// Import MPAndroidChart classes for BMI PieChart.
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import androidx.core.content.edit
import androidx.core.graphics.toColorInt

// -------------------------------------------------------------
// BMIActivity - calculates Body Mass Index and syncs with server.
// Also shows global BMI distribution in a PieChart.
// And manages a simple daily calories log.
// -------------------------------------------------------------
class BMIActivity : AppCompatActivity() {
    // Input field for weight (kg).
    private lateinit var weightEdit: EditText
    // Input field for height (cm).
    private lateinit var heightEdit: EditText
    // Button to trigger BMI calculation.
    private lateinit var calcButton: Button
    // TextView to show calculated or saved BMI.
    private lateinit var resultText: TextView
    // Back button to go home.
    private lateinit var backHome: ImageButton
    // PieChart to show global BMI distribution.
    private lateinit var bmiPieChart: PieChart

    // -------- Calories UI --------
    // EditText where user types calories amount to add/remove.
    private lateinit var caloriesInput: EditText
    // Button to add calories.
    private lateinit var addCaloriesButton: Button
    // Button to subtract calories.
    private lateinit var subtractCaloriesButton: Button
    // Button to reset calories to 0.
    private lateinit var resetCaloriesButton: Button
    // TextView that shows today's calories and status vs target.
    private lateinit var caloriesStatusText: TextView

    // Currently logged-in user (loaded from SharedPreferences).
    private lateinit var currentUser: String

    // Current daily calories value.
    private var currentCalories: Int = 0

    // -------------------------------------------------------------------------
    // onCreate - lifecycle method called when Activity is created.
    // -------------------------------------------------------------------------
    @SuppressLint("SetTextI18n", "DefaultLocale")
    override fun onCreate(savedInstanceState: Bundle?) {
        // Call parent implementation.
        super.onCreate(savedInstanceState)
        // Load layout XML for BMIActivity.
        setContentView(R.layout.activity_bmiactivity)

        // ---------------------------------------------------------------------
        // Load current logged-in user from SharedPreferences.
        // ---------------------------------------------------------------------
        // Load user session preferences (local storage).
        val prefs = getSharedPreferences(getString(R.string.myprefs), MODE_PRIVATE)
        // Read the currently logged-in username.
        val savedUser = prefs.getString(getString(R.string.currentuser), null)

        // If user is not logged in -> force redirect to log in.
        if (savedUser == null) {
            // Show error toast.
            Toast.makeText(this, "You must log in first", Toast.LENGTH_SHORT).show()
            // Create intent to navigate to LoginActivity.
            val login = Intent(this@BMIActivity, LoginActivity::class.java)
            // Start LoginActivity.
            startActivity(login)
            // Close BMIActivity so the user cannot go back.
            finish()
            // Stop execution here.
            return
        }

        // Save the logged-in username in the class field.
        currentUser = savedUser

        // ---------------------------------------------------------------------
        // Bind UI components from XML to Kotlin fields.
        // ---------------------------------------------------------------------
        // Bind weight input.
        weightEdit = findViewById(R.id.weightEdit)
        // Bind height input.
        heightEdit = findViewById(R.id.heightEdit)
        // Bind calculate button.
        calcButton = findViewById(R.id.calcButton)
        // Bind BMI result text.
        resultText = findViewById(R.id.resultText)
        // Bind back button.
        backHome = findViewById(R.id.imageButton3)
        // Bind PieChart for global BMI distribution.
        bmiPieChart = findViewById(R.id.bmiPieChart)
        // Bind calories input.
        caloriesInput = findViewById(R.id.caloriesInput)
        // Bind add calories button.
        addCaloriesButton = findViewById(R.id.addCaloriesButton)
        // Bind subtract calories button.
        subtractCaloriesButton = findViewById(R.id.subtractCaloriesButton)
        // Bind reset calories button.
        resetCaloriesButton = findViewById(R.id.resetCaloriesButton)
        // Bind calories status text.
        caloriesStatusText = findViewById(R.id.caloriesStatusText)

        // ---------------------------------------------------------------------
        // Try to load saved BMI from server.
        // ---------------------------------------------------------------------
        // Load BMI from server.
        RestClient.getBmi(currentUser).thenAccept { bmi ->
            // Move UI operations back to the Android main thread.
            runOnUiThread {
                // If BMI exists in server -> display it.
                if (bmi != null) {
                    // Display saved BMI.
                    resultText.text = "Saved BMI: " + String.format("%.2f", bmi)
                    // Create SharedPreferences editor.
                    prefs.edit {
                        // Save BMI locally in SharedPreferences as backup.
                        putFloat("lastBmi", bmi.toFloat())
                        // Save changes.
                    }
                } else {
                    // If no BMI in server -> try to load local backup.
                    val savedBmi = prefs.getFloat("lastBmi", -1f)
                    // Check whether a local BMI backup exists.
                    if (savedBmi != -1f) {
                        // Display locally saved BMI.
                        resultText.text = "Saved BMI: " + String.format("%.2f", savedBmi)
                    }
                }
            }
        }

        // ---------------------------------------------------------------------
        // Load daily calories from server (simple single field "calories").
        // ---------------------------------------------------------------------
        RestClient.getCalories(currentUser).thenAccept { calories ->
            // Move UI operations back to the Android main thread.
            runOnUiThread {
                // Check whether the server returned calories.
                if (calories != null) {
                    // If server returned value -> use it.
                    currentCalories = calories
                    // Create SharedPreferences editor.
                    prefs.edit {
                        // Save calories locally as backup.
                        putInt("lastCalories", calories)
                        // Save changes.
                    }
                } else {
                    // Fallback to local stored value (if exists).
                    currentCalories = prefs.getInt("lastCalories", 0)
                }

                // Update status text to reflect current calories.
                updateCaloriesStatusText()
            }
        }

        // ---------------------------------------------------------------------
        // Calculate button - compute BMI and update server.
        // ---------------------------------------------------------------------
        calcButton.setOnClickListener {
            // Read weight input as string and trim spaces.
            val weightStr = weightEdit.text.toString().trim()
            // Read height input as string and trim spaces.
            val heightStr = heightEdit.text.toString().trim()

            // Validate that both fields are not empty.
            if (weightStr.isEmpty() || heightStr.isEmpty()) {
                // Show validation message.
                Toast.makeText(this, getString(R.string.fill_all_fields), Toast.LENGTH_SHORT).show()
            } else {
                try {
                    // Convert weight to Double.
                    val weight = weightStr.toDouble()
                    // Convert height from centimeters to meters.
                    val heightM = heightStr.toDouble() / 100.0
                    // Calculate BMI = weight / (height^2).
                    val bmi = weight / (heightM * heightM)

                    // Display calculated BMI immediately.
                    resultText.text = "Your BMI is " + String.format("%.2f", bmi)

                    // Save BMI to server using RestClient.
                    // Use currentUser to identify the user for authentication.
                    RestClient.updateBmi(currentUser, bmi).thenAccept { success ->
                        // Move UI operations back to the Android main thread.
                        runOnUiThread {
                            // Check whether server update succeeded.
                            if (success) {
                                // Create SharedPreferences editor.
                                prefs.edit {
                                    // If server update succeeded -> save locally.
                                    putFloat("lastBmi", bmi.toFloat())
                                    // Save changes.
                                }

                                // Show confirmation toast.
                                Toast.makeText(this@BMIActivity, "BMI saved successfully", Toast.LENGTH_SHORT).show()

                                // Optionally reload global chart after save.
                                loadBmiDistributionChart()
                            } else {
                                // If failed -> show error toast.
                                Toast.makeText(this@BMIActivity, "Failed to save BMI", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                } catch (_: NumberFormatException) {
                    // If input is not a valid number.
                    Toast.makeText(this, "Invalid input", Toast.LENGTH_SHORT).show()
                }
            }
        }

        // ---------------------------------------------------------------------
        // Calories buttons - add / subtract / reset.
        // ---------------------------------------------------------------------
        addCaloriesButton.setOnClickListener {
            // Read user input as string and trim.
            val txt = caloriesInput.text.toString().trim()

            // Check whether the input is empty.
            if (txt.isEmpty()) {
                // Show toast if input is empty.
                Toast.makeText(this@BMIActivity, "Please enter calories amount", Toast.LENGTH_SHORT).show()
            } else {
                try {
                    // Convert the entered calories amount to Int.
                    val delta = txt.toInt()

                    // Check that the amount is positive.
                    if (delta <= 0) {
                        // Show toast if input is not positive.
                        Toast.makeText(this@BMIActivity, "Amount must be positive", Toast.LENGTH_SHORT).show()
                    } else {
                        // Increase today's calories.
                        currentCalories += delta
                        // Update status text.
                        updateCaloriesStatusText()
                        // Save to server.
                        saveCaloriesToServer()
                    }
                } catch (_: NumberFormatException) {
                    // Show error when calories input is not a valid number.
                    Toast.makeText(this@BMIActivity, "Invalid calories number", Toast.LENGTH_SHORT).show()
                }
            }
        }

        subtractCaloriesButton.setOnClickListener {
            // Read user input as string and trim.
            val txt = caloriesInput.text.toString().trim()

            // Check whether the input is empty.
            if (txt.isEmpty()) {
                // Show toast if input is empty.
                Toast.makeText(this@BMIActivity, "Please enter calories amount", Toast.LENGTH_SHORT).show()
            } else {
                try {
                    // Convert the entered calories amount to Int.
                    val delta = txt.toInt()

                    // Check that the amount is positive.
                    if (delta <= 0) {
                        // Show toast if input is not positive.
                        Toast.makeText(this@BMIActivity, "Amount must be positive", Toast.LENGTH_SHORT).show()
                    } else {
                        // Decrease today's calories.
                        currentCalories -= delta

                        // Prevent calories from becoming negative.
                        if (currentCalories < 0) {
                            // Reset calories to zero.
                            currentCalories = 0
                        }

                        // Update status text.
                        updateCaloriesStatusText()
                        // Save to server.
                        saveCaloriesToServer()
                    }
                } catch (_: NumberFormatException) {
                    // Show error when calories input is not a valid number.
                    Toast.makeText(this@BMIActivity, "Invalid calories number", Toast.LENGTH_SHORT).show()
                }
            }
        }

        resetCaloriesButton.setOnClickListener {
            // Reset calories to zero.
            currentCalories = 0
            // Update status text.
            updateCaloriesStatusText()
            // Save to server.
            saveCaloriesToServer()
        }

        // ---------------------------------------------------------------------
        // Back button - return to HomePage.
        // ---------------------------------------------------------------------
        backHome.setOnClickListener {
            // Create intent to navigate to HomePage.
            val bhome = Intent(this@BMIActivity, HomePage::class.java)
            // Start HomePage.
            startActivity(bhome)
        }

        // ---------------------------------------------------------------------
        // Load global BMI distribution for PieChart (does not affect old logic).
        // ---------------------------------------------------------------------
        loadBmiDistributionChart()
    }

    // -------------------------------------------------------------------------
    // Helper: updateCaloriesStatusText
    // Builds a message like:
    // "Today calories: 1500 kcal"
    // -------------------------------------------------------------------------
    private fun updateCaloriesStatusText() {
        // Build calories and BMI category information message.
        val msg = "Today calories: " + currentCalories + " kcal.\n\n" +
                "[BMI < 18.5] → Underweight\n" +
                "[18.5 <= BMI < 25] → Normal\n" +
                "[25 <= BMI < 30] → Overweight\n" +
                "[BMI >= 30] → Obese"

        // Display the message.
        caloriesStatusText.text = msg
    }

    // -------------------------------------------------------------------------
    // Helper: saveCaloriesToServer
    // Uses RestClient.setCalories and also updates SharedPreferences backup.
    // -------------------------------------------------------------------------
    private fun saveCaloriesToServer() {
        // Send the current calories value to the server.
        RestClient.setCalories(currentUser, currentCalories).thenAccept { success ->
            // Move UI operations back to the Android main thread.
            runOnUiThread {
                // Check whether the server update succeeded.
                if (success) {
                    // Load SharedPreferences.
                    val prefs = getSharedPreferences(getString(R.string.myprefs), MODE_PRIVATE)
                    // Create editor to save locally.
                    prefs.edit {
                        // Save today's calories.
                        putInt("lastCalories", currentCalories)
                        // Save changes to SharedPreferences.
                    }
                    // Show confirmation toast.
                    Toast.makeText(this@BMIActivity, "Calories saved", Toast.LENGTH_SHORT).show()
                } else {
                    // If failed -> show error toast.
                    Toast.makeText(this@BMIActivity, "Failed to save calories", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // -------------------------------------------------------------------------
    // loadBmiDistributionChart - fetches global BMI distribution and renders it
    // in a PieChart using RestClient.getBmiDistribution().
    // -------------------------------------------------------------------------
    @Suppress("SpellCheckingInspection")
    private fun loadBmiDistributionChart() {
        // Show default text if there is no data yet.
        bmiPieChart.setNoDataText("No BMI statistics available")

        // Call REST API to get global BMI distribution.
        val future = RestClient.getBmiDistribution()

        // Handle async response.
        future.thenAccept { obj ->
            // Move chart operations back to the Android main thread.
            runOnUiThread {
                try {
                    // Check whether the server returned data.
                    if (obj == null) {
                        // Log when no BMI distribution was returned.
                        Log.w("BMI_CHART", "getBmiDistribution returned null")
                    } else {
                        // Prepare entries for each BMI category.
                        val entries = ArrayList<PieEntry>()
                        // Prepare colors for each BMI category.
                        val colors = ArrayList<Int>()

                        // Read Underweight count from JSON (0 if missing).
                        val under = obj.optInt("Underweight", 0)
                        // Read Normal count from JSON (0 if missing).
                        val normal = obj.optInt("Normal", 0)
                        // Read Overweight count from JSON (0 if missing).
                        val over = obj.optInt("Overweight", 0)
                        // Read Obese count from JSON (0 if missing).
                        val obese = obj.optInt("Obese", 0)

                        // Add Underweight category only if at least one user exists.
                        if (under > 0) {
                            // Add Underweight entry.
                            entries.add(PieEntry(under.toFloat(), "Underweight"))
                            // Add soft blue color.
                            colors.add("#64B5F6".toColorInt())
                        }

                        // Add Normal category only if at least one user exists.
                        if (normal > 0) {
                            // Add Normal entry.
                            entries.add(PieEntry(normal.toFloat(), "Normal"))
                            // Add soft green color.
                            colors.add("#81C784".toColorInt())
                        }

                        // Add Overweight category only if at least one user exists.
                        if (over > 0) {
                            // Add Overweight entry.
                            entries.add(PieEntry(over.toFloat(), "Overweight"))
                            // Add soft yellow color.
                            colors.add("#FFD54F".toColorInt())
                        }

                        // Add Obese category only if at least one user exists.
                        if (obese > 0) {
                            // Add Obese entry.
                            entries.add(PieEntry(obese.toFloat(), "Obese"))
                            // Add soft red color.
                            colors.add("#E57373".toColorInt())
                        }

                        // Check whether there are any BMI values to display.
                        if (entries.isEmpty()) {
                            // Clear previous chart data.
                            bmiPieChart.clear()
                            // Show message when no BMI records exist.
                            bmiPieChart.setNoDataText("No BMI statistics yet")
                        } else {
                            // Create data set for PieChart.
                            val dataSet = PieDataSet(entries, "")
                            // Set colors for the BMI categories.
                            dataSet.colors = colors
                            // Set text size for values on slices.
                            dataSet.valueTextSize = 12f

                            // Create PieData object from dataset.
                            val data = PieData(dataSet)

                            // Assign data to chart.
                            bmiPieChart.data = data

                            // Disable chart description.
                            bmiPieChart.description.isEnabled = false
                            // Disable percentage mode.
                            bmiPieChart.setUsePercentValues(false)
                            // Set category label text size.
                            bmiPieChart.setEntryLabelTextSize(10f)

                            // Enable legend to show categories.
                            bmiPieChart.legend.isEnabled = true

                            // Refresh chart.
                            bmiPieChart.invalidate()
                        }
                    }
                } catch (e: Exception) {
                    // Log any unexpected error.
                    Log.e("BMI_CHART", "Error rendering BMI PieChart", e)
                }
            }
        }
    }
}