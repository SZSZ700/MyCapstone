// Define package for the test class
package com.example.myfinaltopapplication;
// Import Android classes used inside the Activity
import android.app.Application;
import android.os.Looper;
import android.widget.ImageButton;
import android.widget.TextView;
// Import JSON for building fake server responses
import org.json.JSONObject;
// Import JUnit test and runner
import org.junit.Test;
import org.junit.runner.RunWith;
// Import assertions
import static org.junit.Assert.*;
// Import Robolectric core classes
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.Shadows;
// Import Mockito for static mocking of RestClient
import org.mockito.Mockito;
// Import Java collections and concurrency
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
// Import MPAndroidChart classes for checking chart data
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.data.BarDataSet;

// -----------------------------------------------------------------------------
// WaterChartActivityTest
// Purpose: Deep tests for WaterChartActivity using Robolectric + Mockito.
// I test:
//   1) Behavior when there is no history data at all (history == null)
//   2) Drawing the 7-days chart with real JSON history data
//   3) Drawing the weekly averages chart with a real Map<String,Integer>
//   4) Back button navigation to HomePage
// -----------------------------------------------------------------------------
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class WaterChartActivityTest {

    // -------------------------------------------------------------------------
    // Helper: store a user in SharedPreferences before creating the Activity
    // -------------------------------------------------------------------------
    private void putUserInPrefs(Application app) {
        // Get SharedPreferences file by name
        var prefs = app.getSharedPreferences(
                app.getString(R.string.myprefs),
                Application.MODE_PRIVATE
        );
        // Store current user in SharedPreferences
        var editor = prefs.edit();
        // Store username in SharedPreferences
        editor.putString(app.getString(R.string.currentuser), "john");
        // Commit changes to SharedPreferences
        editor.commit();
    }

    // -------------------------------------------------------------------------
    // TEST 1:
    // When getWaterHistoryMap returns null:
    //   - The title should be "No data available"
    //   - Verify that RestClient.getWaterHistoryMap and getWeeklyAverages
    //     were each called exactly once.
    // -------------------------------------------------------------------------
    @Test
    public void waterChart_noHistoryData_showsNoDataTitle() {
        // Mock static methods of RestClient
        try (var restClientMock = Mockito.mockStatic(RestClient.class)) {

            // Get application instance from Robolectric
            var app = RuntimeEnvironment.getApplication();

            // Put logged-in user "john" into SharedPreferences
            this.putUserInPrefs(app);

            // Prepare future for getWaterHistoryMap returning null
            CompletableFuture<JSONObject> historyFuture =
                    CompletableFuture.completedFuture(null);

            // Stub RestClient.getWaterHistoryMap("john", 7) to return null future
            restClientMock.when(
                    // Lambda for RestClient.getWaterHistoryMap(...)
                    () -> RestClient.getWaterHistoryMap("john", 7)
            ).thenReturn(historyFuture);

            // Prepare future for weekly averages: empty map
            CompletableFuture<Map<String, Integer>> weeklyFuture =
                    CompletableFuture.completedFuture(Collections.emptyMap());

            // Stub RestClient.getWeeklyAverages("john") to return empty map
            restClientMock.when(
                    // Lambda for RestClient.getWeeklyAverages(...)
                    () -> RestClient.getWeeklyAverages("john")
            ).thenReturn(weeklyFuture);

            // Build and start WaterChartActivity
            var activity = Robolectric.buildActivity(WaterChartActivity.class).setup().get();

            // Run all pending UI tasks (runOnUiThread)
            Shadows.shadowOf(Looper.getMainLooper()).idle();

            // Find chartTitle TextView
            TextView title = activity.findViewById(R.id.chartTitle);

            // Assert that the title shows "No data available"
            assertEquals("No data available", title.getText().toString());

            // Verify that getWaterHistoryMap was called once with "john",7
            restClientMock.verify(
                    // Lambda for RestClient.getWaterHistoryMap(...)
                    () -> RestClient.getWaterHistoryMap("john", 7),
                    Mockito.times(1)
            );

            // Verify that getWeeklyAverages was called once with "john"
            restClientMock.verify(
                    // Lambda for RestClient.getWeeklyAverages(...)
                    () -> RestClient.getWeeklyAverages("john"),
                    // Called once
                    Mockito.times(1)
            );
        }
    }

    // -------------------------------------------------------------------------
    // TEST 2:
    // There is real history data for up to 7 days:
    //   - Check that the 7-day chart is not empty
    //   - Check that Y values match the JSON
    //   - Check that the chart title is correct
    // -------------------------------------------------------------------------
    @Test
    public void waterChart_withHistoryData_draws7DayChart() throws Exception {
        // Mock static methods of RestClient
        try (var restClientMock = Mockito.mockStatic(RestClient.class)) {

            // Get application from Robolectric
            var app = RuntimeEnvironment.getApplication();

            // Put logged in user "john"
            putUserInPrefs(app);

            // Build fake JSON history:
            // 2025-09-28 → 1000 ml
            // 2025-09-29 → 2000 ml
            var historyJson = new JSONObject();
            historyJson.put("2025-09-28", 1000);
            historyJson.put("2025-09-29", 2000);

            // Future for getWaterHistoryMap full of data
            CompletableFuture<JSONObject> historyFuture =
                    CompletableFuture.completedFuture(historyJson);

            // Stub getWaterHistoryMap to return our JSON for "john",7
            restClientMock.when(
                    // Lambda for RestClient.getWaterHistoryMap(...)
                    () -> RestClient.getWaterHistoryMap("john", 7)
            ).thenReturn(historyFuture);

            // Prepare weekly averages: empty map (not the focus of this test)
            CompletableFuture<Map<String, Integer>> weeklyFuture =
                    CompletableFuture.completedFuture(Collections.emptyMap());

            // Stub getWeeklyAverages
            restClientMock.when(
                    // Lambda for RestClient.getWeeklyAverages(...)
                    () -> RestClient.getWeeklyAverages("john")
            ).thenReturn(weeklyFuture);

            // Build and start WaterChartActivity
            var activity = Robolectric.buildActivity(WaterChartActivity.class).setup().get();

            // Run pending UI tasks
            Shadows.shadowOf(Looper.getMainLooper()).idle();

            // Find the 7-day BarChart
            BarChart chart7 = activity.findViewById(R.id.barChart7days);

            // Make sure chart has data
            var data = chart7.getData();
            // Assert that data is not null
            assertNotNull(data);

            // There should be exactly 1 DataSet
            assertEquals(1, data.getDataSetCount());

            // Get the single DataSet
            var set = (BarDataSet) data.getDataSetByIndex(0);

            // There should be 2 entries (for two dates)
            assertEquals(2, set.getEntryCount());

            // Get first and second entries
            var first = set.getEntryForIndex(0);
            var second = set.getEntryForIndex(1);

            // Assert Y-values match our JSON (1000, 2000)
            assertEquals(1000f, first.getY(), 0.001f);
            assertEquals(2000f, second.getY(), 0.001f);

            // Check chart title text
            TextView title = activity.findViewById(R.id.chartTitle);
            // Assert that the title is correct
            assertEquals("Water History - Last 7 days", title.getText().toString());
        }
    }

    // -------------------------------------------------------------------------
    // TEST 3:
    // There is a Map of weekly averages:
    //   - Check that the weekly chart has correct entries
    //   - Check that Y-values match the Map (order preserved via LinkedHashMap)
    // -------------------------------------------------------------------------
    @Test
    public void waterChart_withWeeklyAverages_drawsWeeklyChart() {
        // Mock static methods of RestClient
        try (var restClientMock = Mockito.mockStatic(RestClient.class)) {

            // Get application instance
            var app = RuntimeEnvironment.getApplication();

            // Put user "john" into SharedPreferences
            putUserInPrefs(app);

            // History JSON can be empty for this test (not relevant here)
            var historyJson = new JSONObject();

            CompletableFuture<JSONObject> historyFuture =
                    CompletableFuture.completedFuture(historyJson);

            // Stub getWaterHistoryMap with empty JSON
            restClientMock.when(
                    // Lambda for RestClient.getWaterHistoryMap(...)
                    () -> RestClient.getWaterHistoryMap("john", 7)
            ).thenReturn(historyFuture);

            // Build LinkedHashMap for weekly averages to preserve insertion order
            var weekly = new LinkedHashMap<String, Integer>();
            weekly.put("Week 1", 1000);
            weekly.put("Week 2", 1500);
            weekly.put("Week 3", 2000);
            weekly.put("Week 4", 2500);

            // Prepare future for weekly averages
            CompletableFuture<Map<String, Integer>> weeklyFuture =
                    CompletableFuture.completedFuture(weekly);

            // Stub getWeeklyAverages
            restClientMock.when(
                    // Lambda for RestClient.getWeeklyAverages(...)
                    () -> RestClient.getWeeklyAverages("john")
            ).thenReturn(weeklyFuture);

            // Build and start WaterChartActivity
            var activity = Robolectric.buildActivity(WaterChartActivity.class).setup().get();

            // Run pending UI tasks
            Shadows.shadowOf(Looper.getMainLooper()).idle();

            // Find weekly BarChart
            BarChart weeklyChart = activity.findViewById(R.id.barChartWeekly);

            // Make sure it has data
            var weeklyData = weeklyChart.getData();
            // Assert that data is not null
            assertNotNull(weeklyData);

            // Should be exactly 1 DataSet
            assertEquals(1, weeklyData.getDataSetCount());

            // Get DataSet
            var weekSet = (BarDataSet) weeklyData.getDataSetByIndex(0);

            // Should have 4 entries
            assertEquals(4, weekSet.getEntryCount());

            // Check Y-values of entries by index
            var e0 = weekSet.getEntryForIndex(0);
            var e1 = weekSet.getEntryForIndex(1);
            var e2 = weekSet.getEntryForIndex(2);
            var e3 = weekSet.getEntryForIndex(3);

            // Assert Y-values match our Map
            assertEquals(1000f, e0.getY(), 0.001f);
            assertEquals(1500f, e1.getY(), 0.001f);
            assertEquals(2000f, e2.getY(), 0.001f);
            assertEquals(2500f, e3.getY(), 0.001f);
        }
    }

    // -------------------------------------------------------------------------
    // TEST 4:
    // Clicking the Back button should start HomePage Activity.
    // -------------------------------------------------------------------------
    @Test
    public void waterChart_backButton_navigatesToHomePage() {
        // Mock static RestClient to avoid real calls during onCreate
        try (var restClientMock = Mockito.mockStatic(RestClient.class)) {

            // Get application
            var app = RuntimeEnvironment.getApplication();

            // Put user "john" into SharedPreferences
            putUserInPrefs(app);

            // Fake minimal futures for both calls so Activity can initialize
            CompletableFuture<JSONObject> historyFuture =
                    CompletableFuture.completedFuture(new JSONObject());

            CompletableFuture<Map<String, Integer>> weeklyFuture =
                    CompletableFuture.completedFuture(Collections.emptyMap());

            // Stub both RestClient methods
            restClientMock.when(
                    // Lambda for RestClient.getWaterHistoryMap(...)
                    () -> RestClient.getWaterHistoryMap("john", 7)
            ).thenReturn(historyFuture);
            restClientMock.when(
                    // Lambda for RestClient.getWeeklyAverages(...)
                    () -> RestClient.getWeeklyAverages("john")
            ).thenReturn(weeklyFuture);

            // Build and start WaterChartActivity
            var activity = Robolectric.buildActivity(WaterChartActivity.class).setup().get();

            // Run pending UI tasks
            Shadows.shadowOf(Looper.getMainLooper()).idle();

            // Find back button
            ImageButton backBtn = activity.findViewById(R.id.imageButton);

            // Click back button
            backBtn.performClick();

            // Get ShadowActivity to inspect next started Activity
            var shadowActivity = Shadows.shadowOf(activity);

            // Get next started Intent
            var started = shadowActivity.getNextStartedActivity();

            // Assert that an Activity was started
            assertNotNull(started);

            // Assert that the target Activity is HomePage
            assertEquals(
                    HomePage.class.getName(),
                    Objects.requireNonNull(started.getComponent()).getClassName()
            );
        }
    }
}
