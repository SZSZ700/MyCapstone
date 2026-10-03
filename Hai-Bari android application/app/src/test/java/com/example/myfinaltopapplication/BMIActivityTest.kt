package com.example.myfinaltopapplication
// Android imports.
import android.app.Activity
import android.app.Application
import android.os.Looper
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
// JSON for fake server responses.
import org.json.JSONObject
// JUnit imports.
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
// Robolectric imports.
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows
import org.robolectric.android.controller.ActivityController
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowToast
// Mockito.
import org.mockito.Mockito
// Mockito Kotlin matchers.
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
// Java concurrency.
import java.util.concurrent.CompletableFuture
// MPAndroidChart imports for checking PieChart state.
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet

// -----------------------------------------------------------------------------
// BMIActivityTest
// Purpose: Deep tests for BMIActivity using Robolectric + Mockito.
// I test:
//   1) Redirect to LoginActivity if no user in SharedPreferences
//   2) Initial load of BMI + calories from server into UI
//   3) Successful BMI calculation and update (updateBmi + SharedPreferences)
//   4) Calories add / subtract / reset flows (including setCalories calls)
//   5) BMI distribution PieChart data rendering
//   6) Back button navigation to HomePage
// -----------------------------------------------------------------------------
@Suppress("UnusedVariable")
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class BMIActivityTest {

    // Hold every ActivityController created by this test class so it can be closed after each test.
    private val activityControllers = mutableListOf<ActivityController<*>>()

    // -------------------------------------------------------------------------
    // Helper: build an Activity and keep its controller for deterministic cleanup.
    // -------------------------------------------------------------------------
    private fun <T : Activity> buildActivity(activityClass: Class<T>): T {
        // Build and fully start the Activity.
        val controller = Robolectric.buildActivity(activityClass).setup()

        // Save the controller so @After can close it even if the test exits early.
        activityControllers.add(controller)

        // Return the Activity instance used by the test.
        return controller.get()
    }

    // -------------------------------------------------------------------------
    // Cleanup: close every ActivityController created during the current test.
    // -------------------------------------------------------------------------
    @After
    fun tearDownActivities() {
        // Close in reverse order in case one Activity started another Activity.
        activityControllers.asReversed().forEach { controller ->
            // Move the Activity through its final lifecycle cleanup.
            controller.close()
        }

        // Remove references so the next test starts with an empty controller list.
        activityControllers.clear()
    }

    // -------------------------------------------------------------------------
    // Helper: store a username in SharedPreferences before creating the Activity.
    // -------------------------------------------------------------------------
    private fun putUserInPrefs(app: Application) {
        // Get SharedPreferences by name as defined in strings.xml.
        val prefs = app.getSharedPreferences(app.getString(R.string.myprefs), Application.MODE_PRIVATE)

        // Create SharedPreferences editor.
        val editor = prefs.edit()
        // Store the current user name.
        editor.putString(app.getString(R.string.currentuser), "john")
        // Save changes immediately.
        editor.commit()
    }

    // -------------------------------------------------------------------------
    // TEST 1:
    // If there is NO current user in SharedPreferences:
    //   - Activity should show Toast "You must log in first"
    //   - Activity should start LoginActivity
    //   - Activity should finish itself.
    // -------------------------------------------------------------------------
    @Test
    fun onCreate_noUser_redirectsToLoginAndFinishes() {
        // Mock static RestClient to avoid real calls.
        val restClientMock = Mockito.mockStatic(RestClient::class.java)

        restClientMock.use { _ ->
            // Get application instance.
            @Suppress("unused")
            val app = RuntimeEnvironment.getApplication()

            // Build BMIActivity.
            val activity = buildActivity(BMIActivity::class.java)

            // Run pending UI tasks.
            Shadows.shadowOf(Looper.getMainLooper()).idle()

            // Check latest Toast message.
            val toastText: CharSequence? = ShadowToast.getTextOfLatestToast()
            // Assert that Toast was shown.
            assertNotNull(toastText)
            // Assert that Toast message equals "You must log in first".
            assertEquals("You must log in first", toastText.toString())

            // Check that LoginActivity was started.
            val shadowActivity = Shadows.shadowOf(activity)
            // Get started Activity.
            val started = shadowActivity.nextStartedActivity

            // Assert that LoginActivity was started.
            assertNotNull(started)
            // Assert that LoginActivity is the target.
            assertEquals(LoginActivity::class.java.name, started.component!!.className)

            // Activity should be finishing.
            assertTrue(activity.isFinishing)
        }
    }

    // -------------------------------------------------------------------------
    // TEST 2:
    // Initial load with server data:
    //   - getBmi returns 24.5
    //   - getCalories returns 1500
    //   - UI should show "Saved BMI: 24.50"
    //   - caloriesStatusText should reflect 1500
    //   - lastBmi and lastCalories should be stored in SharedPreferences.
    // -------------------------------------------------------------------------
    @Test
    fun onCreate_withServerData_loadsBmiAndCaloriesIntoUi() {
        // Mock RestClient.
        val restClientMock = Mockito.mockStatic(RestClient::class.java)

        restClientMock.use { restClientMock ->
            // Get application.
            val app = RuntimeEnvironment.getApplication()

            // Put logged-in user.
            putUserInPrefs(app)

            // Stub getBmi("john") -> 24.5.
            val bmiFuture: CompletableFuture<Double?> = CompletableFuture.completedFuture(24.5)
            // Return fake BMI future.
            restClientMock.`when`<CompletableFuture<Double?>> {
                RestClient.getBmi("john")
            }.thenReturn(bmiFuture)

            // Stub getCalories("john") -> 1500.
            val caloriesFuture: CompletableFuture<Int?> = CompletableFuture.completedFuture(1500)
            // Return fake calories future.
            restClientMock.`when`<CompletableFuture<Int?>> {
                RestClient.getCalories("john")
            }.thenReturn(caloriesFuture)

            // Stub getBmiDistribution() with empty JSON to simplify.
            val distFuture: CompletableFuture<JSONObject?> =
                CompletableFuture.completedFuture(JSONObject())
            // Return fake BMI distribution.
            restClientMock.`when`<CompletableFuture<JSONObject?>> {
                RestClient.getBmiDistribution()
            }.thenReturn(distFuture)

            // Build BMIActivity.
            val activity = buildActivity(BMIActivity::class.java)

            // Run pending UI tasks.
            Shadows.shadowOf(Looper.getMainLooper()).idle()

            // Find BMI result TextView.
            val resultText: TextView = activity.findViewById(R.id.resultText)
            // Find calories status TextView.
            val caloriesStatusText: TextView = activity.findViewById(R.id.caloriesStatusText)

            // Check BMI text.
            assertEquals("Saved BMI: 24.50", resultText.text.toString())

            // Read calories status text.
            val statusText = caloriesStatusText.text.toString()
            // Assert that statusText contains "Today calories: 1500 kcal".
            assertTrue(statusText.contains("Today calories: 1500 kcal"))

            // Check SharedPreferences stored values.
            val prefs = app.getSharedPreferences(
                app.getString(R.string.myprefs),
                Application.MODE_PRIVATE
            )

            // Get stored BMI.
            val lastBmi = prefs.getFloat("lastBmi", -1f)
            // Get stored calories.
            val lastCalories = prefs.getInt("lastCalories", -1)

            // Assert stored BMI.
            assertEquals(24.5f, lastBmi, 0.001f)
            // Assert stored calories.
            assertEquals(1500, lastCalories)
        }
    }

    // -------------------------------------------------------------------------
    // TEST 3 (simple, robust):
    // BMI calculation success:
    //   - weight = 70, height = 170
    //   - Text shows "Your BMI is ..."
    //   - updateBmi is called once for "john" with any double
    //   - lastBmi is stored in SharedPreferences (not -1)
    //   - Toast "BMI saved successfully" is shown.
    // -------------------------------------------------------------------------
    @Test
    fun calcButton_validInput_calculatesAndSavesBmi() {
        // Mock RestClient.
        val restClientMock = Mockito.mockStatic(RestClient::class.java)

        restClientMock.use { restClientMock ->
            // Get application instance.
            val app = RuntimeEnvironment.getApplication()

            // Put logged-in user "john" into SharedPreferences.
            putUserInPrefs(app)

            // Stub getBmi("john") -> null (no previous BMI).
            val bmiFuture: CompletableFuture<Double?> = CompletableFuture.completedFuture(null)
            // Return fake BMI future.
            restClientMock.`when`<CompletableFuture<Double?>> {
                RestClient.getBmi("john")
            }.thenReturn(bmiFuture)

            // Stub getCalories("john") -> 0.
            val caloriesFuture: CompletableFuture<Int?> = CompletableFuture.completedFuture(0)
            // Return fake calories future.
            restClientMock.`when`<CompletableFuture<Int?>> {
                RestClient.getCalories("john")
            }.thenReturn(caloriesFuture)

            // Stub getBmiDistribution() -> empty JSON.
            val distFuture: CompletableFuture<JSONObject?> =
                CompletableFuture.completedFuture(JSONObject())
            // Return fake distribution.
            restClientMock.`when`<CompletableFuture<JSONObject?>> {
                RestClient.getBmiDistribution()
            }.thenReturn(distFuture)

            // Stub updateBmi("john", any double) -> success.
            restClientMock.`when`<CompletableFuture<Boolean>> {
                RestClient.updateBmi(eq("john"), any<Double>())
            }.thenReturn(CompletableFuture.completedFuture(true))

            // Build and start BMIActivity.
            val activity = buildActivity(BMIActivity::class.java)

            // Let initial async calls finish.
            Shadows.shadowOf(Looper.getMainLooper()).idle()

            // Find weight input.
            val weightEdit: EditText = activity.findViewById(R.id.weightEdit)
            // Find height input.
            val heightEdit: EditText = activity.findViewById(R.id.heightEdit)
            // Find calculate button.
            val calcButton: Button = activity.findViewById(R.id.calcButton)
            // Find result TextView.
            val resultText: TextView = activity.findViewById(R.id.resultText)

            // Enter sample weight.
            weightEdit.setText("70")
            // Enter sample height.
            heightEdit.setText("170")

            // Click calculate.
            calcButton.performClick()

            // Let async updateBmi thenAccept run.
            Shadows.shadowOf(Looper.getMainLooper()).idle()

            // Read BMI result text.
            val res = resultText.text.toString()
            // Check that resultText starts with "Your BMI is ".
            assertTrue(res.startsWith("Your BMI is "))

            // Verify updateBmi was called once for "john" with any double.
            restClientMock.verify(
                { RestClient.updateBmi(eq("john"), any<Double>()) },
                Mockito.times(1)
            )

            // Get SharedPreferences.
            val prefs = app.getSharedPreferences(
                app.getString(R.string.myprefs),
                Application.MODE_PRIVATE
            )

            // Get stored BMI value.
            val storedBmi = prefs.getFloat("lastBmi", -1f)
            // Assert that lastBmi is not default value (-1).
            assertNotEquals(-1f, storedBmi, 0.0001f)

            // Get latest Toast message.
            val toastText: CharSequence? = ShadowToast.getTextOfLatestToast()
            // Assert that Toast was shown.
            assertNotNull(toastText)
            // Assert that Toast message equals "BMI saved successfully".
            assertEquals("BMI saved successfully", toastText.toString())
        }
    }

    // -------------------------------------------------------------------------
    // TEST 4:
    // Calories add flow:
    //   - Initial server calories = 1000
    //   - User enters "500" and clicks add
    //   - currentCalories should become 1500
    //   - caloriesStatusText updated
    //   - setCalories called once with 1500
    //   - "Calories saved" Toast is shown.
    // -------------------------------------------------------------------------
    @Test
    fun addCalories_validInput_updatesCaloriesAndSaves() {
        // Mock RestClient.
        val restClientMock = Mockito.mockStatic(RestClient::class.java)

        restClientMock.use { restClientMock ->
            // Get application.
            val app = RuntimeEnvironment.getApplication()

            // Put user.
            putUserInPrefs(app)

            // Stub getBmi -> null.
            val bmiFuture: CompletableFuture<Double?> = CompletableFuture.completedFuture(null)
            // Return fake BMI future.
            restClientMock.`when`<CompletableFuture<Double?>> {
                RestClient.getBmi("john")
            }.thenReturn(bmiFuture)

            // Stub getCalories -> 1000.
            val caloriesFuture: CompletableFuture<Int?> = CompletableFuture.completedFuture(1000)
            // Return fake calories future.
            restClientMock.`when`<CompletableFuture<Int?>> {
                RestClient.getCalories("john")
            }.thenReturn(caloriesFuture)

            // Stub getBmiDistribution -> empty JSON.
            val distFuture: CompletableFuture<JSONObject?> =
                CompletableFuture.completedFuture(JSONObject())
            // Return fake BMI distribution.
            restClientMock.`when`<CompletableFuture<JSONObject?>> {
                RestClient.getBmiDistribution()
            }.thenReturn(distFuture)

            // Stub setCalories("john", any int) -> true.
            restClientMock.`when`<CompletableFuture<Boolean>> {
                RestClient.setCalories(eq("john"), any<Int>())
            }.thenReturn(CompletableFuture.completedFuture(true))

            // Build activity.
            val activity = buildActivity(BMIActivity::class.java)

            // Run pending tasks (initial futures).
            Shadows.shadowOf(Looper.getMainLooper()).idle()

            // Find calories input.
            val caloriesInput: EditText = activity.findViewById(R.id.caloriesInput)
            // Find add button.
            val addButton: Button = activity.findViewById(R.id.addCaloriesButton)
            // Find calories status TextView.
            val caloriesStatusText: TextView = activity.findViewById(R.id.caloriesStatusText)

            // Enter "500".
            caloriesInput.setText("500")

            // Click add.
            addButton.performClick()

            // Run pending tasks (setCalories thenAccept).
            Shadows.shadowOf(Looper.getMainLooper()).idle()

            // Read status text.
            val statusText = caloriesStatusText.text.toString()
            // Assert that statusText contains "Today calories: 1500 kcal".
            assertTrue(statusText.contains("Today calories: 1500 kcal"))

            // Check that setCalories was called with 1500.
            restClientMock.verify(
                { RestClient.setCalories(eq("john"), eq(1500)) },
                Mockito.times(1)
            )

            // Get latest Toast message.
            val toastText: CharSequence? = ShadowToast.getTextOfLatestToast()
            // Assert that Toast was shown.
            assertNotNull(toastText)
            // Assert that Toast message equals "Calories saved".
            assertEquals("Calories saved", toastText.toString())
        }
    }

    // -------------------------------------------------------------------------
    // TEST 5:
    // Calories reset flow:
    //   - Initial server calories = 1200
    //   - User clicks reset button
    //   - currentCalories becomes 0
    //   - status text shows "Today calories: 0 kcal ..."
    //   - setCalories called with 0.
    // -------------------------------------------------------------------------
    @Test
    fun resetCalories_setsToZero_andSaves() {
        // Mock RestClient.
        val restClientMock = Mockito.mockStatic(RestClient::class.java)

        restClientMock.use { restClientMock ->
            // Get application.
            val app = RuntimeEnvironment.getApplication()

            // Put user.
            putUserInPrefs(app)

            // Stub getBmi -> null.
            val bmiFuture: CompletableFuture<Double?> = CompletableFuture.completedFuture(null)
            // Return fake BMI future.
            restClientMock.`when`<CompletableFuture<Double?>> {
                RestClient.getBmi("john")
            }.thenReturn(bmiFuture)

            // Stub getCalories -> 1200.
            val caloriesFuture: CompletableFuture<Int?> = CompletableFuture.completedFuture(1200)
            // Return fake calories future.
            restClientMock.`when`<CompletableFuture<Int?>> {
                RestClient.getCalories("john")
            }.thenReturn(caloriesFuture)

            // Stub getBmiDistribution -> empty JSON.
            val distFuture: CompletableFuture<JSONObject?> =
                CompletableFuture.completedFuture(JSONObject())
            // Return fake BMI distribution.
            restClientMock.`when`<CompletableFuture<JSONObject?>> {
                RestClient.getBmiDistribution()
            }.thenReturn(distFuture)

            // Stub setCalories("john", any int) -> true.
            restClientMock.`when`<CompletableFuture<Boolean>> {
                RestClient.setCalories(eq("john"), any<Int>())
            }.thenReturn(CompletableFuture.completedFuture(true))

            // Build activity.
            val activity = buildActivity(BMIActivity::class.java)

            // Run pending tasks.
            Shadows.shadowOf(Looper.getMainLooper()).idle()

            // Find reset button.
            val resetButton: Button = activity.findViewById(R.id.resetCaloriesButton)
            // Find calories status TextView.
            val caloriesStatusText: TextView = activity.findViewById(R.id.caloriesStatusText)

            // Click reset.
            resetButton.performClick()

            // Run pending tasks.
            Shadows.shadowOf(Looper.getMainLooper()).idle()

            // Read status text.
            val statusText = caloriesStatusText.text.toString()
            // Assert that status text shows 0 calories.
            assertTrue(statusText.contains("Today calories: 0 kcal"))

            // Verify setCalories was called with 0.
            restClientMock.verify(
                { RestClient.setCalories(eq("john"), eq(0)) },
                Mockito.times(1)
            )
        }
    }

    // -------------------------------------------------------------------------
    // TEST 6:
    // BMI distribution PieChart:
    //   - getBmiDistribution returns JSON with some counts
    //   - PieChart should have entries for non-zero categories
    //   - DataSet entry count should match number of non-zero categories.
    // -------------------------------------------------------------------------
    @Test
    fun loadBmiDistributionChart_withData_populatesPieChart() {
        // Mock RestClient.
        val restClientMock = Mockito.mockStatic(RestClient::class.java)

        restClientMock.use { restClientMock ->
            // Get application.
            val app = RuntimeEnvironment.getApplication()

            // Put user.
            putUserInPrefs(app)

            // Stub getBmi -> null.
            val bmiFuture: CompletableFuture<Double?> = CompletableFuture.completedFuture(null)
            // Return fake BMI future.
            restClientMock.`when`<CompletableFuture<Double?>> {
                RestClient.getBmi("john")
            }.thenReturn(bmiFuture)

            // Stub getCalories -> 0.
            val caloriesFuture: CompletableFuture<Int?> = CompletableFuture.completedFuture(0)
            // Return fake calories future.
            restClientMock.`when`<CompletableFuture<Int?>> {
                RestClient.getCalories("john")
            }.thenReturn(caloriesFuture)

            // Build JSON distribution.
            val distJson = JSONObject()
            // Underweight = 2.
            distJson.put("Underweight", 2)
            // Normal = 5.
            distJson.put("Normal", 5)
            // Overweight = 3.
            distJson.put("Overweight", 3)
            // Obese = 0.
            distJson.put("Obese", 0)

            // Stub getBmiDistribution -> distJson.
            val distFuture: CompletableFuture<JSONObject?> =
                CompletableFuture.completedFuture(distJson)
            // Return fake distribution.
            restClientMock.`when`<CompletableFuture<JSONObject?>> {
                RestClient.getBmiDistribution()
            }.thenReturn(distFuture)

            // Build activity.
            val activity = buildActivity(BMIActivity::class.java)

            // Run pending tasks (including loadBmiDistributionChart).
            Shadows.shadowOf(Looper.getMainLooper()).idle()

            // Find PieChart.
            val chart: PieChart = activity.findViewById(R.id.bmiPieChart)
            // Assert that chart exists.
            assertNotNull(chart)

            // Get data from chart.
            val data: PieData = chart.data
            // Assert that data is not null.
            assertNotNull(data)

            // There should be one dataset.
            assertEquals(1, data.dataSetCount)

            // Get first dataset.
            val dataSet = data.getDataSetByIndex(0) as PieDataSet

            // Non-zero categories are: Underweight, Normal, Overweight -> 3 entries.
            assertEquals(3, dataSet.entryCount)

            // Get first entry.
            val e0 = dataSet.getEntryForIndex(0)
            // Get second entry.
            val e1 = dataSet.getEntryForIndex(1)
            // Get third entry.
            val e2 = dataSet.getEntryForIndex(2)

            // Check Underweight Y-value.
            assertEquals(2f, e0.y, 0.001f)
            // Check Underweight label.
            assertEquals("Underweight", e0.label)

            // Check Normal Y-value.
            assertEquals(5f, e1.y, 0.001f)
            // Check Normal label.
            assertEquals("Normal", e1.label)

            // Check Overweight Y-value.
            assertEquals(3f, e2.y, 0.001f)
            // Check Overweight label.
            assertEquals("Overweight", e2.label)
        }
    }

    // -------------------------------------------------------------------------
    // TEST 7:
    // Back button:
    //   - Clicking backHome should start HomePage Activity.
    // -------------------------------------------------------------------------
    @Test
    fun backHomeButton_click_navigatesToHomePage() {
        // Mock RestClient.
        val restClientMock = Mockito.mockStatic(RestClient::class.java)

        restClientMock.use { restClientMock ->
            // Get application.
            val app = RuntimeEnvironment.getApplication()

            // Put user.
            putUserInPrefs(app)

            // Stub getBmi -> null.
            val bmiFuture: CompletableFuture<Double?> = CompletableFuture.completedFuture(null)
            // Return fake BMI future.
            restClientMock.`when`<CompletableFuture<Double?>> {
                RestClient.getBmi("john")
            }.thenReturn(bmiFuture)

            // Stub getCalories -> 0.
            val caloriesFuture: CompletableFuture<Int?> = CompletableFuture.completedFuture(0)
            // Return fake calories future.
            restClientMock.`when`<CompletableFuture<Int?>> {
                RestClient.getCalories("john")
            }.thenReturn(caloriesFuture)

            // Stub getBmiDistribution -> empty JSON.
            val distFuture: CompletableFuture<JSONObject?> =
                CompletableFuture.completedFuture(JSONObject())
            // Return fake distribution.
            restClientMock.`when`<CompletableFuture<JSONObject?>> {
                RestClient.getBmiDistribution()
            }.thenReturn(distFuture)

            // Build activity.
            val activity = buildActivity(BMIActivity::class.java)

            // Run pending tasks.
            Shadows.shadowOf(Looper.getMainLooper()).idle()

            // Find back button.
            val backHome: ImageButton = activity.findViewById(R.id.imageButton3)

            // Click back button.
            backHome.performClick()

            // Inspect next started activity.
            val shadowActivity = Shadows.shadowOf(activity)
            // Get started Activity.
            val started = shadowActivity.nextStartedActivity

            // Assert that an Activity was started.
            assertNotNull(started)
            // Assert that the target Activity is HomePage.
            assertEquals(HomePage::class.java.name, started.component!!.className)
        }
    }
}