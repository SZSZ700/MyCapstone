// Define package of the test.
package com.example.myfinaltopapplication
// Import Android Intent for inspecting navigation between Activities.
import android.content.Intent
// Import Android Looper for running pending main-thread tasks.
import android.os.Looper
// Import Android widget Button to access buttons from layout.
import android.widget.Button
// Import JUnit annotations.
import org.junit.Test
import org.junit.runner.RunWith
// Import assertions.
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
// Import Robolectric test runner.
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
// Import Robolectric configuration for SDK level.
import org.robolectric.annotation.Config
// Import Shadows helpers to inspect started Activities.
import org.robolectric.Shadows
import org.robolectric.shadows.ShadowActivity

// -----------------------------------------------------------------------------
// HomePageTest
// Purpose: deep tests for HomePage navigation using Robolectric.
// I test that each button starts the correct Activity:
//  - BMI button -> BMIActivity
//  - Water button -> WaterActivity
//  - Graph button -> WaterChartActivity
//  - DailyGoal button -> DailyWaterGoal
// -----------------------------------------------------------------------------
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class HomePageTest {

    // -------------------------------------------------------------------------
    // Helper method: build a fully created HomePage activity.
    // -------------------------------------------------------------------------
    private fun buildActivity(): HomePage {
        // Use Robolectric to build HomePage and call onCreate().
        // Return created instance.
        return Robolectric.buildActivity(HomePage::class.java)
            .setup()
            .get()
    }

    // -------------------------------------------------------------------------
    // TEST 1: Verify that all buttons are present in the layout and not null.
    // -------------------------------------------------------------------------
    @Test
    fun homePage_onCreate_buttonsAreNotNull() {
        // Build HomePage activity.
        val activity = buildActivity()

        // Find BMI button by id.
        val bmiBtn: Button = activity.findViewById(R.id.button3)
        // Find Water button by id.
        val waterBtn: Button = activity.findViewById(R.id.button4)
        // Find Graph button by id.
        val graphBtn: Button = activity.findViewById(R.id.button5)
        // Find Daily Goal button by id.
        val dailyGoalBtn: Button = activity.findViewById(R.id.button6)

        // Assert that BMI button exists.
        assertNotNull(bmiBtn)
        // Assert that Water button exists.
        assertNotNull(waterBtn)
        // Assert that Graph button exists.
        assertNotNull(graphBtn)
        // Assert that Daily Goal button exists.
        assertNotNull(dailyGoalBtn)
    }

    // -------------------------------------------------------------------------
    // TEST 2: Clicking BMI button should start BMIActivity.
    // -------------------------------------------------------------------------
    @Test
    fun clickingBmiButton_startsBMIActivity() {
        // Build HomePage activity.
        val activity = buildActivity()

        // Find BMI button.
        val bmiBtn: Button = activity.findViewById(R.id.button3)
        // Perform click on BMI button.
        bmiBtn.performClick()

        // Run pending tasks in main looper (defensive).
        Shadows.shadowOf(Looper.getMainLooper()).idle()

        // Get ShadowActivity for navigation inspection.
        val shadowActivity: ShadowActivity = Shadows.shadowOf(activity)
        // Get next started activity Intent.
        val startedIntent: Intent = shadowActivity.nextStartedActivity

        // Assert that an Activity was started.
        assertNotNull(startedIntent)
        // Assert that the Activity class is BMIActivity.
        assertEquals(BMIActivity::class.java.name, startedIntent.component!!.className)
    }

    // -------------------------------------------------------------------------
    // TEST 3: Clicking Water button should start WaterActivity.
    // -------------------------------------------------------------------------
    @Test
    fun clickingWaterButton_startsWaterActivity() {
        // Build HomePage activity.
        val activity = buildActivity()

        // Find Water button.
        val waterBtn: Button = activity.findViewById(R.id.button4)
        // Perform click on Water button.
        waterBtn.performClick()

        // Run pending tasks in main looper.
        Shadows.shadowOf(Looper.getMainLooper()).idle()

        // Get ShadowActivity.
        val shadowActivity: ShadowActivity = Shadows.shadowOf(activity)
        // Get next started activity Intent.
        val startedIntent: Intent = shadowActivity.nextStartedActivity

        // Assert that we navigated somewhere.
        assertNotNull(startedIntent)
        // Assert that target is WaterActivity.
        assertEquals(WaterActivity::class.java.name, startedIntent.component!!.className)
    }

    // -------------------------------------------------------------------------
    // TEST 4: Clicking Graph button should start WaterChartActivity.
    // -------------------------------------------------------------------------
    @Test
    fun clickingGraphButton_startsWaterChartActivity() {
        // Build HomePage activity.
        val activity = buildActivity()

        // Find Graph button.
        val graphBtn: Button = activity.findViewById(R.id.button5)
        // Perform click on Graph button.
        graphBtn.performClick()

        // Run pending tasks in main looper.
        Shadows.shadowOf(Looper.getMainLooper()).idle()

        // Get ShadowActivity.
        val shadowActivity: ShadowActivity = Shadows.shadowOf(activity)
        // Get next started activity Intent.
        val startedIntent: Intent = shadowActivity.nextStartedActivity

        // Assert that an Activity was started.
        assertNotNull(startedIntent)
        // Assert that the Activity is WaterChartActivity.
        assertEquals(WaterChartActivity::class.java.name, startedIntent.component!!.className)
    }

    // -------------------------------------------------------------------------
    // TEST 5: Clicking Daily Goal button should start DailyWaterGoal activity.
    // -------------------------------------------------------------------------
    @Test
    fun clickingDailyGoalButton_startsDailyWaterGoalActivity() {
        // Build HomePage activity.
        val activity = buildActivity()

        // Find Daily Goal button.
        val dailyGoalBtn: Button = activity.findViewById(R.id.button6)
        // Perform click on Daily Goal button.
        dailyGoalBtn.performClick()

        // Run pending tasks in main looper.
        Shadows.shadowOf(Looper.getMainLooper()).idle()

        // Get ShadowActivity.
        val shadowActivity: ShadowActivity = Shadows.shadowOf(activity)
        // Get next started activity Intent.
        val startedIntent: Intent = shadowActivity.nextStartedActivity

        // Assert that an Activity was started.
        assertNotNull(startedIntent)
        // Assert that destination is DailyWaterGoal.
        assertEquals(DailyWaterGoal::class.java.name, startedIntent.component!!.className)
    }
}