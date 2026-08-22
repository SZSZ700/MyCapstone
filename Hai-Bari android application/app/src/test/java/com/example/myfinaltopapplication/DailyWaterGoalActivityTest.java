// Define package for the test class
package com.example.myfinaltopapplication;
// Android imports used in the Activity
import android.app.Application;
import android.os.Looper;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
// JSON for fake server responses
import org.json.JSONObject;
// JUnit imports
import org.junit.Test;
import org.junit.runner.RunWith;
// Assertions
import static org.junit.Assert.*;
// Robolectric imports
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowToast;
import org.mockito.Mockito;
// Java utilities
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
// MPAndroidChart imports to inspect donut chart state
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.PieDataSet;

// -----------------------------------------------------------------------------
// DailyWaterGoalActivityTest
// Purpose: Deep tests for DailyWaterGoal using Robolectric + Mockito.
// I test:
//   1) Validation errors when saving a goal (empty / non-numeric / out-of-range)
//   2) Successful goal update flow (calls RestClient, updates labels + chart)
//   3) Initial fetchAndRender: goal + today from JSON → labels + chart
//   4) Back button navigation to HomePage
// -----------------------------------------------------------------------------
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class DailyWaterGoalActivityTest {

    // -------------------------------------------------------------------------
    // Helper: store a username in SharedPreferences before creating the Activity
    // -------------------------------------------------------------------------
    private void putUserInPrefs(Application app) {
        // Get SharedPreferences by name defined in strings.xml
        var prefs = app.getSharedPreferences(
                app.getString(R.string.myprefs),
                Application.MODE_PRIVATE
        );
        // Edit SharedPreferences to store current user
        var editor = prefs.edit();
        editor.putString(app.getString(R.string.currentuser), "john");
        editor.commit();
    }

    // -------------------------------------------------------------------------
    // TEST 1: Empty goal input → validation Toast, no RestClient.setGoal call.
    // -------------------------------------------------------------------------
    @Test
    public void saveGoal_emptyInput_showsValidationToast_andDoesNotCallSetGoal() {
        // Mock static methods of RestClient
        try (var restClientMock = Mockito.mockStatic(RestClient.class)) {

            // Get application instance
            var app = RuntimeEnvironment.getApplication();

            // Put logged-in user "john" into SharedPreferences
            putUserInPrefs(app);

            // Prepare minimal futures for initial fetchAndRender to avoid real calls
            CompletableFuture<JSONObject> goalFuture =
                    CompletableFuture.completedFuture(new JSONObject());
            CompletableFuture<JSONObject> waterFuture =
                    CompletableFuture.completedFuture(new JSONObject());

            // Stub getGoal and getWater for initial load
            restClientMock.when(
                    // Lambda for RestClient.getGoal call
                    () -> RestClient.getGoal("john")
            ).thenReturn(goalFuture);

            // Stub getWater for initial load
            restClientMock.when(
                    // Lambda for RestClient.getWater call
                    () -> RestClient.getWater("john")
            ).thenReturn(waterFuture);

            // Build and start DailyWaterGoal Activity
            var activity = Robolectric.buildActivity(DailyWaterGoal.class).setup().get();

            // Run all pending UI tasks
            Shadows.shadowOf(Looper.getMainLooper()).idle();

            // Find views
            EditText goalInput = activity.findViewById(R.id.goalInput);
            Button saveGoalBtn = activity.findViewById(R.id.saveGoalBtn);

            // Leave input empty
            goalInput.setText("");

            // Click "Save goal"
            saveGoalBtn.performClick();

            // Run pending UI tasks for Toast
            Shadows.shadowOf(Looper.getMainLooper()).idle();

            // Capture latest Toast text
            CharSequence toastText = ShadowToast.getTextOfLatestToast();

            // Assert Toast is shown
            assertNotNull(toastText);
            // Assert message is the validation message
            assertEquals("Enter a daily goal in ml", toastText.toString());

            // Verify that setGoal was never called
            restClientMock.verify(
                    // Lambda for RestClient.setGoal call
                    () -> RestClient.setGoal(Mockito.anyString(), Mockito.anyInt()),
                    // Never called
                    Mockito.never()
            );
        }
    }

    // -------------------------------------------------------------------------
    // TEST 2: Non-numeric input → "Goal must be a number" Toast, no setGoal call.
    // -------------------------------------------------------------------------
    @Test
    public void saveGoal_nonNumericInput_showsNumberError_andDoesNotCallSetGoal() {
        // Mock RestClient statics
        try (var restClientMock = Mockito.mockStatic(RestClient.class)) {

            // Get application
            var app = RuntimeEnvironment.getApplication();

            // Put user "john"
            putUserInPrefs(app);

            // Minimal futures for fetchAndRender
            CompletableFuture<JSONObject> goalFuture =
                    CompletableFuture.completedFuture(new JSONObject());
            CompletableFuture<JSONObject> waterFuture =
                    CompletableFuture.completedFuture(new JSONObject());

            // Stub getGoal and getWater for "john"
            restClientMock.when(
                    // Lambda for RestClient.getGoal call
                    () -> RestClient.getGoal("john")
            ).thenReturn(goalFuture);

            // Stub getWater for "john"
            restClientMock.when(
                    // Lambda for RestClient.getWater call
                    () -> RestClient.getWater("john")
            ).thenReturn(waterFuture);

            // Build Activity
            var activity = Robolectric.buildActivity(DailyWaterGoal.class).setup().get();

            // Run pending tasks
            Shadows.shadowOf(Looper.getMainLooper()).idle();

            // Find views
            EditText goalInput = activity.findViewById(R.id.goalInput);
            Button saveGoalBtn = activity.findViewById(R.id.saveGoalBtn);

            // Enter non-numeric value
            goalInput.setText("abc");

            // Click save
            saveGoalBtn.performClick();

            // Run pending UI tasks
            Shadows.shadowOf(Looper.getMainLooper()).idle();

            // Get latest Toast text
            CharSequence toastText = ShadowToast.getTextOfLatestToast();

            // Assert Toast text
            assertNotNull(toastText);
            // Assert Toast message
            assertEquals("Goal must be a number", toastText.toString());

            // Verify setGoal not called
            restClientMock.verify(
                    // Lambda for RestClient.setGoal call
                    () -> RestClient.setGoal(Mockito.anyString(), Mockito.anyInt()),
                    // Never called
                    Mockito.never()
            );
        }
    }

    // -------------------------------------------------------------------------
    // TEST 3: Out-of-range input (<500 or >10000) → range Toast, no setGoal call.
    // Here we test upper bound; lower bound is symmetric.
    // -------------------------------------------------------------------------
    @Test
    public void saveGoal_outOfRange_showsRangeToast_andDoesNotCallSetGoal() {
        // Mock RestClient statics
        try (var restClientMock = Mockito.mockStatic(RestClient.class)) {

            // Get application instance
            var app = RuntimeEnvironment.getApplication();

            // Put user
            putUserInPrefs(app);

            // Minimal futures for initial load
            CompletableFuture<JSONObject> goalFuture =
                    CompletableFuture.completedFuture(new JSONObject());
            CompletableFuture<JSONObject> waterFuture =
                    CompletableFuture.completedFuture(new JSONObject());

            // Stub getGoal and getWater for "john"
            restClientMock.when(
                    // Lambda for RestClient.getGoal call
                    () -> RestClient.getGoal("john")
            ).thenReturn(goalFuture);

            // Stub getWater for "john"
            restClientMock.when(
                    // Lambda for RestClient.getWater call
                    () -> RestClient.getWater("john")
            ).thenReturn(waterFuture);

            // Build Activity
            var activity = Robolectric.buildActivity(DailyWaterGoal.class).setup().get();

            // Run pending UI tasks
            Shadows.shadowOf(Looper.getMainLooper()).idle();

            // Find views
            EditText goalInput = activity.findViewById(R.id.goalInput);
            Button saveGoalBtn = activity.findViewById(R.id.saveGoalBtn);

            // Put an out-of-range value (e.g. 20000)
            goalInput.setText("20000");

            // Click save
            saveGoalBtn.performClick();

            // Run pending tasks
            Shadows.shadowOf(Looper.getMainLooper()).idle();

            // Get latest Toast
            CharSequence toastText = ShadowToast.getTextOfLatestToast();

            // Assert Toast message is correct
            assertNotNull(toastText);
            // Assert Toast message
            assertEquals("Goal should be between 500 and 10000 ml", toastText.toString());

            // Verify setGoal was not invoked
            restClientMock.verify(
                    // Lambda for RestClient.setGoal call
                    () -> RestClient.setGoal(Mockito.anyString(), Mockito.anyInt()),
                    // Never called
                    Mockito.never()
            );
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
    public void saveGoal_success_updatesGoal_andRendersDonutCorrectly() throws Exception {
        // Mock RestClient statics
        try (var restClientMock = Mockito.mockStatic(RestClient.class)) {

            // Get application
            var app = RuntimeEnvironment.getApplication();

            // Put user "john"
            putUserInPrefs(app);

            // Build fake JSON for initial goal
            var goalJson = new JSONObject();
            goalJson.put("goalMl", 3000);

            // Build fake JSON for water: today=1000
            var waterJson = new JSONObject();
            waterJson.put("todayWater", 1000);
            waterJson.put("yesterdayWater", 0);

            // Futures for initial fetchAndRender
            CompletableFuture<JSONObject> goalFuture =
                    CompletableFuture.completedFuture(goalJson);
            CompletableFuture<JSONObject> waterFuture =
                    CompletableFuture.completedFuture(waterJson);

            // Stub getGoal and getWater for "john"
            restClientMock.when(
                    // Lambda for RestClient.getGoal call
                    () -> RestClient.getGoal("john")
            ).thenReturn(goalFuture);

            // Stub getWater for "john"
            restClientMock.when(
                    // Lambda for RestClient.getWater call
                    () -> RestClient.getWater("john")
            ).thenReturn(waterFuture);

            // Stub setGoal("john", 2600) to succeed
            CompletableFuture<Boolean> setGoalFuture =
                    CompletableFuture.completedFuture(true);

            // Stub setGoal for "john"
            restClientMock.when(
                    // Lambda for RestClient.setGoal call
                    () -> RestClient.setGoal("john", 2600)
            ).thenReturn(setGoalFuture);

            // Build Activity
            var activity = Robolectric.buildActivity(DailyWaterGoal.class).setup().get();

            // Run pending tasks (to apply initial fetchAndRender)
            Shadows.shadowOf(Looper.getMainLooper()).idle();

            // Find views
            EditText goalInput = activity.findViewById(R.id.goalInput);
            Button saveGoalBtn = activity.findViewById(R.id.saveGoalBtn);
            TextView goalText = activity.findViewById(R.id.goalText);
            TextView todayText = activity.findViewById(R.id.todayText);
            PieChart donutChart = activity.findViewById(R.id.donutChart);

            // Assert initial labels: goal 3000, today 1000
            assertEquals("Goal: 3,000 ml", goalText.getText().toString());
            assertEquals("Today: 1,000 ml", todayText.getText().toString());

            // Now change goal to 2600
            goalInput.setText("2600");

            // Click save goal to trigger onSaveGoalClicked
            saveGoalBtn.performClick();

            // Run pending UI tasks for thenAccept
            Shadows.shadowOf(Looper.getMainLooper()).idle();

            // Verify setGoal was called exactly once with "john", 2600
            restClientMock.verify(
                    // Lambda for RestClient.setGoal call
                    () -> RestClient.setGoal("john", 2600),
                    // Called once
                    Mockito.times(1)
            );

            // Labels should now show updated goal
            assertEquals("Goal: 2,600 ml", goalText.getText().toString());
            // Today remains 1000
            assertEquals("Today: 1,000 ml", todayText.getText().toString());

            // Check donut center text "1000 / 2600 ml"
            String centerText = donutChart.getCenterText().toString();
            assertEquals("1,000 / 2,600 ml", centerText);

            // verify chart data entries (1000 consumed, 1600 remaining)
            var data = donutChart.getData();
            // Assert data is not null
            assertNotNull(data);
            // Assert data has 1 set
            assertEquals(1, data.getDataSetCount());

            // Get set
            var set = (PieDataSet) data.getDataSetByIndex(0);
            // Assert set has 2 entries
            assertEquals(2, set.getEntryCount());

            // Get consumed and remaining entries
            var consumed = set.getEntryForIndex(0);
            var remaining = set.getEntryForIndex(1);

            // Assert entries have correct values
            assertEquals(1000f, consumed.getY(), 0.001f);
            assertEquals(1600f, remaining.getY(), 0.001f);
        }
    }

    // -------------------------------------------------------------------------
    // TEST 5:
    // Initial fetchAndRender with no data (null futures) → keeps defaults:
    //  goal 3000, today 0, chart still renders with 0 / 3000.
    // -------------------------------------------------------------------------
    @Test
    public void fetchAndRender_noData_keepsDefaultsAndRendersChart() {
        // Mock RestClient statics
        try (var restClientMock = Mockito.mockStatic(RestClient.class)) {

            // Get application instance
            var app = RuntimeEnvironment.getApplication();

            // Put user
            putUserInPrefs(app);

            // Futures for goal and water that return null
            CompletableFuture<JSONObject> goalFuture =
                    CompletableFuture.completedFuture(null);
            CompletableFuture<JSONObject> waterFuture =
                    CompletableFuture.completedFuture(null);

            // Stub methods

            // Stub getGoal and getWater for "john"
            restClientMock.when(
                    // Lambda for RestClient.getGoal call
                    () -> RestClient.getGoal("john")
            ).thenReturn(goalFuture);

            // Stub getWater for "john"
            restClientMock.when(
                    // Lambda for RestClient.getWater call
                    () -> RestClient.getWater("john")
            ).thenReturn(waterFuture);

            // Build Activity
            var activity = Robolectric.buildActivity(DailyWaterGoal.class).setup().get();

            // Run pending UI tasks
            Shadows.shadowOf(Looper.getMainLooper()).idle();

            // Find views
            TextView goalText = activity.findViewById(R.id.goalText);
            TextView todayText = activity.findViewById(R.id.todayText);
            PieChart donutChart = activity.findViewById(R.id.donutChart);

            // Default goal: 3000, default today: 0
            assertEquals("Goal: 3,000 ml", goalText.getText().toString());
            assertEquals("Today: 0 ml", todayText.getText().toString());

            // Donut center text should reflect 0 / 3000
            var centerText = donutChart.getCenterText().toString();
            // Assert center text
            assertEquals("0 / 3,000 ml", centerText);
        }
    }

    // -------------------------------------------------------------------------
    // TEST 6:
    // Back button click → navigate to HomePage Activity.
    // -------------------------------------------------------------------------
    @Test
    public void backButton_click_navigatesToHomePage() {
        // Mock RestClient to avoid real calls
        try (var restClientMock = Mockito.mockStatic(RestClient.class)) {

            // Get application
            var app = RuntimeEnvironment.getApplication();

            // Put user in prefs
            putUserInPrefs(app);

            // Minimal futures for getGoal and getWater
            CompletableFuture<JSONObject> goalFuture =
                    CompletableFuture.completedFuture(new JSONObject());
            CompletableFuture<JSONObject> waterFuture =
                    CompletableFuture.completedFuture(new JSONObject());

            // Stub getGoal and getWater for "john"
            restClientMock.when(
                    // Lambda for RestClient.getGoal call
                    () -> RestClient.getGoal("john")
            ).thenReturn(goalFuture);

            // Stub getWater for "john"
            restClientMock.when(
                    // Lambda for RestClient.getWater call
                    () -> RestClient.getWater("john")
            ).thenReturn(waterFuture);

            // Build Activity
            var activity = Robolectric.buildActivity(DailyWaterGoal.class).setup().get();

            // Run pending tasks
            Shadows.shadowOf(Looper.getMainLooper()).idle();

            // Find back button
            ImageButton backButton = activity.findViewById(R.id.backButton);

            // Click back button
            backButton.performClick();

            // Inspect next started Activity via ShadowActivity
            var shadowActivity = Shadows.shadowOf(activity);
            // Get started Activity
            var started = shadowActivity.getNextStartedActivity();

            // Assert navigation happened
            assertNotNull(started);
            // Assert started Activity is HomePage
            assertEquals(HomePage.class.getName(),
                    Objects.requireNonNull(started.getComponent()).getClassName());
        }
    }
}

