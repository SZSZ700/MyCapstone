package com.example.myfinaltopapplication
// Android imports used in the Activity.
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
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
// Robolectric imports.
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowToast
// Mockito.
import org.mockito.Mockito
// Mockito Kotlin matchers.
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
// Java concurrency.
import java.util.concurrent.CompletableFuture
// MPAndroidChart imports to inspect donut chart state.
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.PieDataSet

// -----------------------------------------------------------------------------
// DailyWaterGoalActivityTest
// Purpose: Deep tests for DailyWaterGoal using Robolectric + Mockito.
// I test:
//   1) Validation errors when saving a goal (empty / non-numeric / out-of-range)
//   2) Successful goal update flow (calls RestClient, updates labels + chart)
//   3) Initial fetchAndRender: goal + today from JSON -> labels + chart
//   4) Back button navigation to HomePage
// -----------------------------------------------------------------------------
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class DailyWaterGoalActivityTest {

    // -------------------------------------------------------------------------
    // Helper: store a username in SharedPreferences before creating the Activity.
    // -------------------------------------------------------------------------
    private fun putUserInPrefs(app: Application) {
        // Get SharedPreferences by name defined in strings.xml.
        val prefs = app.getSharedPreferences(app.getString(R.string.myprefs), Application.MODE_PRIVATE)

        // Edit SharedPreferences to store current user.
        val editor = prefs.edit()
        // Store username.
        editor.putString(app.getString(R.string.currentuser), "john")
        // Save changes immediately.
        editor.commit()
    }

    // -------------------------------------------------------------------------
    // TEST 1: Empty goal input -> validation Toast, no RestClient.setGoal call.
    // -------------------------------------------------------------------------
    @Test
    fun saveGoal_emptyInput_showsValidationToast_andDoesNotCallSetGoal() {
        // Mock static methods of RestClient.
        val restClientMock = Mockito.mockStatic(RestClient::class.java)

        restClientMock.use { restClientMock ->
            // Get application instance.
            val app = RuntimeEnvironment.getApplication()

            // Put logged-in user "john" into SharedPreferences.
            putUserInPrefs(app)

            // Prepare minimal futures for initial fetchAndRender to avoid real calls.
            val goalFuture: CompletableFuture<JSONObject?> = CompletableFuture.completedFuture(JSONObject())
            val waterFuture: CompletableFuture<JSONObject?> = CompletableFuture.completedFuture(JSONObject())

            // Stub getGoal for initial load.
            restClientMock.`when`<CompletableFuture<JSONObject?>> {
                RestClient.getGoal("john")
            }.thenReturn(goalFuture)

            // Stub getWater for initial load.
            restClientMock.`when`<CompletableFuture<JSONObject?>> {
                RestClient.getWater("john")
            }.thenReturn(waterFuture)

            // Build and start DailyWaterGoal Activity.
            val activity = Robolectric.buildActivity(DailyWaterGoal::class.java).setup().get()

            // Run all pending UI tasks.
            Shadows.shadowOf(Looper.getMainLooper()).idle()

            // Find goal input.
            val goalInput: EditText = activity.findViewById(R.id.goalInput)
            // Find save goal button.
            val saveGoalBtn: Button = activity.findViewById(R.id.saveGoalBtn)

            // Leave input empty.
            goalInput.setText("")

            // Click "Save goal".
            saveGoalBtn.performClick()

            // Run pending UI tasks for Toast.
            Shadows.shadowOf(Looper.getMainLooper()).idle()

            // Capture latest Toast text.
            val toastText: CharSequence? = ShadowToast.getTextOfLatestToast()

            // Assert Toast is shown.
            assertNotNull(toastText)
            // Assert message is the validation message.
            assertEquals("Enter a daily goal in ml", toastText.toString())

            // Verify that setGoal was never called.
            restClientMock.verify(
                { RestClient.setGoal(any<String>(), any<Int>()) },
                Mockito.never()
            )
        }
    }

    // -------------------------------------------------------------------------
    // TEST 2: Non-numeric input -> "Goal must be a number" Toast, no setGoal call.
    // -------------------------------------------------------------------------
    @Test
    fun saveGoal_nonNumericInput_showsNumberError_andDoesNotCallSetGoal() {
        // Mock RestClient statics.
        val restClientMock = Mockito.mockStatic(RestClient::class.java)

        restClientMock.use { restClientMock ->
            // Get application.
            val app = RuntimeEnvironment.getApplication()

            // Put user "john".
            putUserInPrefs(app)

            // Minimal futures for fetchAndRender.
            val goalFuture: CompletableFuture<JSONObject?> = CompletableFuture.completedFuture(JSONObject())
            val waterFuture: CompletableFuture<JSONObject?> = CompletableFuture.completedFuture(JSONObject())

            // Stub getGoal for "john".
            restClientMock.`when`<CompletableFuture<JSONObject?>> {
                RestClient.getGoal("john")
            }.thenReturn(goalFuture)

            // Stub getWater for "john".
            restClientMock.`when`<CompletableFuture<JSONObject?>> {
                RestClient.getWater("john")
            }.thenReturn(waterFuture)

            // Build Activity.
            val activity = Robolectric.buildActivity(DailyWaterGoal::class.java).setup().get()

            // Run pending tasks.
            Shadows.shadowOf(Looper.getMainLooper()).idle()

            // Find goal input.
            val goalInput: EditText = activity.findViewById(R.id.goalInput)
            // Find save button.
            val saveGoalBtn: Button = activity.findViewById(R.id.saveGoalBtn)

            // Enter non-numeric value.
            goalInput.setText("abc")

            // Click save.
            saveGoalBtn.performClick()

            // Run pending UI tasks.
            Shadows.shadowOf(Looper.getMainLooper()).idle()

            // Get latest Toast text.
            val toastText: CharSequence? = ShadowToast.getTextOfLatestToast()

            // Assert Toast is shown.
            assertNotNull(toastText)
            // Assert Toast message.
            assertEquals("Goal must be a number", toastText.toString())

            // Verify setGoal not called.
            restClientMock.verify(
                { RestClient.setGoal(any<String>(), any<Int>()) },
                Mockito.never()
            )
        }
    }

    // -------------------------------------------------------------------------
    // TEST 3: Out-of-range input (<500 or >10000) -> range Toast, no setGoal call.
    // Here we test upper bound; lower bound is symmetric.
    // -------------------------------------------------------------------------
    @Test
    fun saveGoal_outOfRange_showsRangeToast_andDoesNotCallSetGoal() {
        // Mock RestClient statics.
        val restClientMock = Mockito.mockStatic(RestClient::class.java)

        restClientMock.use { restClientMock ->
            // Get application instance.
            val app = RuntimeEnvironment.getApplication()

            // Put user.
            putUserInPrefs(app)

            // Minimal futures for initial load.
            val goalFuture: CompletableFuture<JSONObject?> = CompletableFuture.completedFuture(JSONObject())
            val waterFuture: CompletableFuture<JSONObject?> = CompletableFuture.completedFuture(JSONObject())

            // Stub getGoal for "john".
            restClientMock.`when`<CompletableFuture<JSONObject?>> {
                RestClient.getGoal("john")
            }.thenReturn(goalFuture)

            // Stub getWater for "john".
            restClientMock.`when`<CompletableFuture<JSONObject?>> {
                RestClient.getWater("john")
            }.thenReturn(waterFuture)

            // Build Activity.
            val activity = Robolectric.buildActivity(DailyWaterGoal::class.java).setup().get()

            // Run pending UI tasks.
            Shadows.shadowOf(Looper.getMainLooper()).idle()

            // Find goal input.
            val goalInput: EditText = activity.findViewById(R.id.goalInput)
            // Find save button.
            val saveGoalBtn: Button = activity.findViewById(R.id.saveGoalBtn)

            // Put an out-of-range value (e.g. 20000).
            goalInput.setText("20000")

            // Click save.
            saveGoalBtn.performClick()

            // Run pending tasks.
            Shadows.shadowOf(Looper.getMainLooper()).idle()

            // Get latest Toast.
            val toastText: CharSequence? = ShadowToast.getTextOfLatestToast()

            // Assert Toast is shown.
            assertNotNull(toastText)
            // Assert Toast message.
            assertEquals("Goal should be between 500 and 10000 ml", toastText.toString())

            // Verify setGoal was not invoked.
            restClientMock.verify(
                { RestClient.setGoal(any<String>(), any<Int>()) },
                Mockito.never()
            )
        }
    }

    // -------------------------------------------------------------------------
    // TEST 4:
    // Successful goal update:
    //  - Initial fetch returns goal=3000 and today=1000
    //  - User changes goal to 2600
    //  - setGoal is called, labels update, donut center text shows "1000 / 2600 ml"
    // -------------------------------------------------------------------------
    @Test
    fun saveGoal_success_updatesGoal_andRendersDonutCorrectly() {
        // Mock RestClient statics.
        val restClientMock = Mockito.mockStatic(RestClient::class.java)

        restClientMock.use { restClientMock ->
            // Get application.
            val app = RuntimeEnvironment.getApplication()

            // Put user "john".
            putUserInPrefs(app)

            // Build fake JSON for initial goal.
            val goalJson = JSONObject()
            // Set initial goal to 3000 ml.
            goalJson.put("goalMl", 3000)

            // Build fake JSON for water: today=1000.
            val waterJson = JSONObject()
            // Set today's water amount.
            waterJson.put("todayWater", 1000)
            // Set yesterday's water amount.
            waterJson.put("yesterdayWater", 0)

            // Futures for initial fetchAndRender.
            val goalFuture: CompletableFuture<JSONObject?> = CompletableFuture.completedFuture(goalJson)
            val waterFuture: CompletableFuture<JSONObject?> = CompletableFuture.completedFuture(waterJson)

            // Stub getGoal for "john".
            restClientMock.`when`<CompletableFuture<JSONObject?>> {
                RestClient.getGoal("john")
            }.thenReturn(goalFuture)

            // Stub getWater for "john".
            restClientMock.`when`<CompletableFuture<JSONObject?>> {
                RestClient.getWater("john")
            }.thenReturn(waterFuture)

            // Stub setGoal("john", 2600) to succeed.
            val setGoalFuture: CompletableFuture<Boolean> = CompletableFuture.completedFuture(true)

            // Stub setGoal using Kotlin matchers.
            restClientMock.`when`<CompletableFuture<Boolean>> {
                RestClient.setGoal(eq("john"), eq(2600))
            }.thenReturn(setGoalFuture)

            // Build Activity.
            val activity = Robolectric.buildActivity(DailyWaterGoal::class.java).setup().get()

            // Run pending tasks (to apply initial fetchAndRender).
            Shadows.shadowOf(Looper.getMainLooper()).idle()

            // Find goal input.
            val goalInput: EditText = activity.findViewById(R.id.goalInput)
            // Find save goal button.
            val saveGoalBtn: Button = activity.findViewById(R.id.saveGoalBtn)
            // Find goal label.
            val goalText: TextView = activity.findViewById(R.id.goalText)
            // Find today's water label.
            val todayText: TextView = activity.findViewById(R.id.todayText)
            // Find donut chart.
            val donutChart: PieChart = activity.findViewById(R.id.donutChart)

            // Assert initial goal label.
            assertEquals("Goal: 3,000 ml", goalText.text.toString())
            // Assert initial today label.
            assertEquals("Today: 1,000 ml", todayText.text.toString())

            // Now change goal to 2600.
            goalInput.setText("2600")

            // Click save goal to trigger onSaveGoalClicked.
            saveGoalBtn.performClick()

            // Run pending UI tasks for thenAccept.
            Shadows.shadowOf(Looper.getMainLooper()).idle()

            // Verify setGoal was called exactly once with "john", 2600.
            restClientMock.verify(
                { RestClient.setGoal(eq("john"), eq(2600)) },
                Mockito.times(1)
            )

            // Labels should now show updated goal.
            assertEquals("Goal: 2,600 ml", goalText.text.toString())
            // Today remains 1000.
            assertEquals("Today: 1,000 ml", todayText.text.toString())

            // Read donut center text.
            val centerText = donutChart.centerText.toString()
            // Check donut center text "1000 / 2600 ml".
            assertEquals("1,000 / 2,600 ml", centerText)

            // Get chart data.
            val data = donutChart.data
            // Assert data is not null.
            assertNotNull(data)
            // Assert data has 1 set.
            assertEquals(1, data.getDataSetCount())

            // Get first dataset.
            val set = data.getDataSetByIndex(0) as PieDataSet
            // Assert set has 2 entries.
            assertEquals(2, set.entryCount)

            // Get consumed entry.
            val consumed = set.getEntryForIndex(0)
            // Get remaining entry.
            val remaining = set.getEntryForIndex(1)

            // Assert consumed value.
            assertEquals(1000f, consumed.y, 0.001f)
            // Assert remaining value.
            assertEquals(1600f, remaining.y, 0.001f)
        }
    }

    // -------------------------------------------------------------------------
    // TEST 5:
    // Initial fetchAndRender with no data (null futures) -> keeps defaults:
    //  goal 3000, today 0, chart still renders with 0 / 3000.
    // -------------------------------------------------------------------------
    @Test
    fun fetchAndRender_noData_keepsDefaultsAndRendersChart() {
        // Mock RestClient statics.
        val restClientMock = Mockito.mockStatic(RestClient::class.java)

        restClientMock.use { restClientMock ->
            // Get application instance.
            val app = RuntimeEnvironment.getApplication()

            // Put user.
            putUserInPrefs(app)

            // Future for goal that returns null.
            val goalFuture: CompletableFuture<JSONObject?> = CompletableFuture.completedFuture(null)
            // Future for water that returns null.
            val waterFuture: CompletableFuture<JSONObject?> = CompletableFuture.completedFuture(null)

            // Stub getGoal for "john".
            restClientMock.`when`<CompletableFuture<JSONObject?>> {
                RestClient.getGoal("john")
            }.thenReturn(goalFuture)

            // Stub getWater for "john".
            restClientMock.`when`<CompletableFuture<JSONObject?>> {
                RestClient.getWater("john")
            }.thenReturn(waterFuture)

            // Build Activity.
            val activity = Robolectric.buildActivity(DailyWaterGoal::class.java).setup().get()

            // Run pending UI tasks.
            Shadows.shadowOf(Looper.getMainLooper()).idle()

            // Find goal label.
            val goalText: TextView = activity.findViewById(R.id.goalText)
            // Find today's water label.
            val todayText: TextView = activity.findViewById(R.id.todayText)
            // Find donut chart.
            val donutChart: PieChart = activity.findViewById(R.id.donutChart)

            // Default goal should be 3000.
            assertEquals("Goal: 3,000 ml", goalText.text.toString())
            // Default today should be 0.
            assertEquals("Today: 0 ml", todayText.text.toString())

            // Read donut center text.
            val centerText = donutChart.centerText.toString()
            // Assert center text reflects 0 / 3000.
            assertEquals("0 / 3,000 ml", centerText)
        }
    }

    // -------------------------------------------------------------------------
    // TEST 6:
    // Back button click -> navigate to HomePage Activity.
    // -------------------------------------------------------------------------
    @Test
    fun backButton_click_navigatesToHomePage() {
        // Mock RestClient to avoid real calls.
        val restClientMock = Mockito.mockStatic(RestClient::class.java)

        restClientMock.use { restClientMock ->
            // Get application.
            val app = RuntimeEnvironment.getApplication()

            // Put user in prefs.
            putUserInPrefs(app)

            // Minimal future for getGoal.
            val goalFuture: CompletableFuture<JSONObject?> = CompletableFuture.completedFuture(JSONObject())
            // Minimal future for getWater.
            val waterFuture: CompletableFuture<JSONObject?> = CompletableFuture.completedFuture(JSONObject())

            // Stub getGoal for "john".
            restClientMock.`when`<CompletableFuture<JSONObject?>> {
                RestClient.getGoal("john")
            }.thenReturn(goalFuture)

            // Stub getWater for "john".
            restClientMock.`when`<CompletableFuture<JSONObject?>> {
                RestClient.getWater("john")
            }.thenReturn(waterFuture)

            // Build Activity.
            val activity = Robolectric.buildActivity(DailyWaterGoal::class.java).setup().get()

            // Run pending tasks.
            Shadows.shadowOf(Looper.getMainLooper()).idle()

            // Find back button.
            val backButton: ImageButton = activity.findViewById(R.id.backButton)

            // Click back button.
            backButton.performClick()

            // Inspect next started Activity via ShadowActivity.
            val shadowActivity = Shadows.shadowOf(activity)
            // Get started Activity.
            val started = shadowActivity.nextStartedActivity

            // Assert navigation happened.
            assertNotNull(started)
            // Assert started Activity is HomePage.
            assertEquals(HomePage::class.java.name, started.component!!.className)
        }
    }
}