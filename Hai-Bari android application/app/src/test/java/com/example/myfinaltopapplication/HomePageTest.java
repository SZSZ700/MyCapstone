// Define package of the test
package com.example.myfinaltopapplication;
// Import Android Intent for inspecting navigation between Activities
import android.content.Intent;
// Import Android Looper for running pending main-thread tasks
import android.os.Looper;
// Import Android widget Button to access buttons from layout
import android.widget.Button;
// Import JUnit annotations
import org.junit.Test;
import org.junit.runner.RunWith;
// Import static assertions
import static org.junit.Assert.*;
// Import Robolectric test runner
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
// Import Robolectric configuration for SDK level
import org.robolectric.annotation.Config;
// Import Shadows helpers to inspect started Activities
import org.robolectric.Shadows;
import org.robolectric.shadows.ShadowActivity;
import java.util.Objects;

// -----------------------------------------------------------------------------
// HomePageTest
// Purpose: deep tests for HomePage navigation using Robolectric
// I test that each button starts the correct Activity:
//  - BMI button  -> BMIActivity
//  - Water button -> WaterActivity
//  - Graph button -> WaterChartActivity
//  - DailyGoal button -> DailyWaterGoal
// -----------------------------------------------------------------------------
@SuppressWarnings("SpellCheckingInspection")
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class HomePageTest {

    // -------------------------------------------------------------------------
    // Helper method: build a fully created HomePage activity
    // -------------------------------------------------------------------------
    private HomePage buildActivity() {
        // Use Robolectric to build HomePage and call onCreate()
        // Return created instance
        return Robolectric.buildActivity(HomePage.class)
                .setup()
                .get();
    }

    // -------------------------------------------------------------------------
    // TEST 1: Verify that all buttons are present in the layout and not null.
    // -------------------------------------------------------------------------
    @Test
    public void homePage_onCreate_buttonsAreNotNull() {
        // Build HomePage activity
        HomePage activity = buildActivity();

        // Find BMI button by id
        Button bmiBtn = activity.findViewById(R.id.button3);
        // Find Water button by id
        Button waterBtn = activity.findViewById(R.id.button4);
        // Find Graph button by id
        Button graphBtn = activity.findViewById(R.id.button5);
        // Find Daily Goal button by id
        Button dailyGoalBtn = activity.findViewById(R.id.button6);

        // Assert that BMI button exists
        assertNotNull(bmiBtn);
        // Assert that Water button exists
        assertNotNull(waterBtn);
        // Assert that Graph button exists
        assertNotNull(graphBtn);
        // Assert that Daily Goal button exists
        assertNotNull(dailyGoalBtn);
    }

    // -------------------------------------------------------------------------
    // TEST 2: Clicking BMI button should start BMIActivity.
    // -------------------------------------------------------------------------
    @Test
    public void clickingBmiButton_startsBMIActivity() {
        // Build HomePage activity
        HomePage activity = buildActivity();

        // Find BMI button
        Button bmiBtn = activity.findViewById(R.id.button3);

        // Perform click on BMI button
        bmiBtn.performClick();

        // Run pending tasks in main looper (defensive)
        Shadows.shadowOf(Looper.getMainLooper()).idle();

        // Get ShadowActivity for navigation inspection
        ShadowActivity shadowActivity = Shadows.shadowOf(activity);

        // Get next started activity Intent
        Intent startedIntent = shadowActivity.getNextStartedActivity();

        // Assert that an Activity was started
        assertNotNull(startedIntent);
        // Assert that the Activity class is BMIActivity
        assertEquals(BMIActivity.class.getName(),
                Objects.requireNonNull(startedIntent.getComponent()).getClassName());
    }

    // -------------------------------------------------------------------------
    // TEST 3: Clicking Water button should start WaterActivity.
    // -------------------------------------------------------------------------
    @Test
    public void clickingWaterButton_startsWaterActivity() {
        // Build HomePage activity
        HomePage activity = buildActivity();

        // Find Water button
        Button waterBtn = activity.findViewById(R.id.button4);

        // Perform click on Water button
        waterBtn.performClick();

        // Run pending tasks in main looper
        Shadows.shadowOf(Looper.getMainLooper()).idle();

        // Get ShadowActivity
        ShadowActivity shadowActivity = Shadows.shadowOf(activity);

        // Get next started activity Intent
        Intent startedIntent = shadowActivity.getNextStartedActivity();

        // Assert that we navigated somewhere
        assertNotNull(startedIntent);
        // Assert that target is WaterActivity
        assertEquals(WaterActivity.class.getName(),
                Objects.requireNonNull(startedIntent.getComponent()).getClassName());
    }

    // -------------------------------------------------------------------------
    // TEST 4: Clicking Graph button should start WaterChartActivity.
    // -------------------------------------------------------------------------
    @Test
    public void clickingGraphButton_startsWaterChartActivity() {
        // Build HomePage activity
        HomePage activity = buildActivity();

        // Find Graph button
        Button graphBtn = activity.findViewById(R.id.button5);

        // Perform click on Graph button
        graphBtn.performClick();

        // Run pending tasks in main looper
        Shadows.shadowOf(Looper.getMainLooper()).idle();

        // Get ShadowActivity
        ShadowActivity shadowActivity = Shadows.shadowOf(activity);

        // Get next started activity Intent
        Intent startedIntent = shadowActivity.getNextStartedActivity();

        // Assert that an Activity was started
        assertNotNull(startedIntent);
        // Assert that the Activity is WaterChartActivity
        assertEquals(WaterChartActivity.class.getName(),
                Objects.requireNonNull(startedIntent.getComponent()).getClassName());
    }

    // -------------------------------------------------------------------------
    // TEST 5: Clicking Daily Goal button should start DailyWaterGoal activity.
    // -------------------------------------------------------------------------
    @Test
    public void clickingDailyGoalButton_startsDailyWaterGoalActivity() {
        // Build HomePage activity
        HomePage activity = buildActivity();

        // Find Daily Goal button
        Button dailyGoalBtn = activity.findViewById(R.id.button6);

        // Perform click on Daily Goal button
        dailyGoalBtn.performClick();

        // Run pending tasks in main looper
        Shadows.shadowOf(Looper.getMainLooper()).idle();

        // Get ShadowActivity
        ShadowActivity shadowActivity = Shadows.shadowOf(activity);

        // Get next started activity Intent
        Intent startedIntent = shadowActivity.getNextStartedActivity();

        // Assert that an Activity was started
        assertNotNull(startedIntent);
        // Assert that destination is DailyWaterGoal
        assertEquals(DailyWaterGoal.class.getName(),
                Objects.requireNonNull(startedIntent.getComponent()).getClassName());
    }
}
