// Define package for the test class.
package com.example.myfinaltopapplication
// Import Android classes used inside the Activity.
import android.app.Application
import android.os.Looper
import android.widget.ImageButton
import android.widget.TextView
// Import JSON for building fake server responses.
import org.json.JSONObject
// Import JUnit test and runner.
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
// Import Robolectric core classes.
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows
import org.robolectric.annotation.Config
// Import Mockito for static mocking of RestClient.
import org.mockito.Mockito
// Import Java collections and concurrency.
import java.util.LinkedHashMap
import java.util.concurrent.CompletableFuture
// Import MPAndroidChart classes for checking chart data.
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.data.BarDataSet

// -----------------------------------------------------------------------------
// WaterChartActivityTest
// Purpose: Deep tests for WaterChartActivity using Robolectric + Mockito.
// I test:
//   1) Behavior when there is no history data at all (history == null)
//   2) Drawing the 7-days chart with real JSON history data
//   3) Drawing the weekly averages chart with a real Map<String, Int>
//   4) Back button navigation to HomePage
// -----------------------------------------------------------------------------
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class WaterChartActivityTest {

    // -------------------------------------------------------------------------
    // Helper: store a user in SharedPreferences before creating the Activity.
    // -------------------------------------------------------------------------
    private fun putUserInPrefs(app: Application) {
        // Get SharedPreferences file by name.
        val prefs = app.getSharedPreferences(app.getString(R.string.myprefs), Application.MODE_PRIVATE)

        // Store current user in SharedPreferences.
        val editor = prefs.edit()
        // Store username in SharedPreferences.
        editor.putString(app.getString(R.string.currentuser), "john")
        // Commit changes to SharedPreferences.
        editor.commit()
    }

    // -------------------------------------------------------------------------
    // TEST 1:
    // When getWaterHistoryMap returns null:
    //   - The title should be "No data available"
    //   - Verify that RestClient.getWaterHistoryMap and getWeeklyAverages
    //     were each called exactly once.
    // -------------------------------------------------------------------------
    @Test
    fun waterChart_noHistoryData_showsNoDataTitle() {
        // Mock static methods of RestClient.
        val restClientMock = Mockito.mockStatic(RestClient::class.java)

        restClientMock.use { restClientMock ->
            // Get application instance from Robolectric.
            val app = RuntimeEnvironment.getApplication()

            // Put logged-in user "john" into SharedPreferences.
            putUserInPrefs(app)

            // Prepare future for getWaterHistoryMap returning null.
            val historyFuture: CompletableFuture<JSONObject?> =
                CompletableFuture.completedFuture(null)

            // Stub RestClient.getWaterHistoryMap("john", 7) to return null future.
            restClientMock.`when`<CompletableFuture<JSONObject?>> {
                RestClient.getWaterHistoryMap("john", 7)
            }.thenReturn(historyFuture)

            // Prepare future for weekly averages: empty map.
            val weeklyFuture: CompletableFuture<Map<String, Int>> =
                CompletableFuture.completedFuture(emptyMap())

            // Stub RestClient.getWeeklyAverages("john") to return empty map.
            restClientMock.`when`<CompletableFuture<Map<String, Int>>> {
                RestClient.getWeeklyAverages("john")
            }.thenReturn(weeklyFuture)

            // Build and start WaterChartActivity.
            val activity = Robolectric.buildActivity(WaterChartActivity::class.java).setup().get()

            // Run all pending UI tasks (runOnUiThread).
            Shadows.shadowOf(Looper.getMainLooper()).idle()

            // Find chartTitle TextView.
            val title: TextView = activity.findViewById(R.id.chartTitle)

            // Assert that the title shows "No data available".
            assertEquals("No data available", title.text.toString())

            // Verify that getWaterHistoryMap was called once with "john",7.
            restClientMock.verify(
                { RestClient.getWaterHistoryMap("john", 7) },
                Mockito.times(1)
            )

            // Verify that getWeeklyAverages was called once with "john".
            restClientMock.verify(
                { RestClient.getWeeklyAverages("john") },
                Mockito.times(1)
            )
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
    fun waterChart_withHistoryData_draws7DayChart() {
        // Mock static methods of RestClient.
        val restClientMock = Mockito.mockStatic(RestClient::class.java)

        restClientMock.use { restClientMock ->
            // Get application from Robolectric.
            val app = RuntimeEnvironment.getApplication()

            // Put logged-in user "john".
            putUserInPrefs(app)

            // Build fake JSON history:
            // 2025-09-28 -> 1000 ml
            // 2025-09-29 -> 2000 ml
            val historyJson = JSONObject()
            historyJson.put("2025-09-28", 1000)
            historyJson.put("2025-09-29", 2000)

            // Future for getWaterHistoryMap full of data.
            val historyFuture: CompletableFuture<JSONObject?> =
                CompletableFuture.completedFuture(historyJson)

            // Stub getWaterHistoryMap to return our JSON for "john",7.
            restClientMock.`when`<CompletableFuture<JSONObject?>> {
                RestClient.getWaterHistoryMap("john", 7)
            }.thenReturn(historyFuture)

            // Prepare weekly averages: empty map (not the focus of this test).
            val weeklyFuture: CompletableFuture<Map<String, Int>> =
                CompletableFuture.completedFuture(emptyMap())

            // Stub getWeeklyAverages.
            restClientMock.`when`<CompletableFuture<Map<String, Int>>> {
                RestClient.getWeeklyAverages("john")
            }.thenReturn(weeklyFuture)

            // Build and start WaterChartActivity.
            val activity = Robolectric.buildActivity(WaterChartActivity::class.java).setup().get()

            // Run pending UI tasks.
            Shadows.shadowOf(Looper.getMainLooper()).idle()

            // Find the 7-day BarChart.
            val chart7: BarChart = activity.findViewById(R.id.barChart7days)

            // Make sure chart has data.
            val data = chart7.data
            // Assert that data is not null.
            assertNotNull(data)

            // There should be exactly 1 DataSet.
            assertEquals(1, data.dataSetCount)

            // Get the single DataSet.
            val set = data.getDataSetByIndex(0) as BarDataSet

            // There should be 2 entries (for two dates).
            assertEquals(2, set.entryCount)

            // Get first and second entries.
            val first = set.getEntryForIndex(0)
            val second = set.getEntryForIndex(1)

            // Assert Y-values match our JSON (1000, 2000).
            assertEquals(1000f, first.y, 0.001f)
            assertEquals(2000f, second.y, 0.001f)

            // Check chart title text.
            val title: TextView = activity.findViewById(R.id.chartTitle)
            // Assert that the title is correct.
            assertEquals("Water History - Last 7 days", title.text.toString())
        }
    }

    // -------------------------------------------------------------------------
    // TEST 3:
    // There is a Map of weekly averages:
    //   - Check that the weekly chart has correct entries
    //   - Check that Y-values match the Map (order preserved via LinkedHashMap)
    // -------------------------------------------------------------------------
    @Test
    fun waterChart_withWeeklyAverages_drawsWeeklyChart() {
        // Mock static methods of RestClient.
        val restClientMock = Mockito.mockStatic(RestClient::class.java)

        restClientMock.use { restClientMock ->
            // Get application instance.
            val app = RuntimeEnvironment.getApplication()

            // Put user "john" into SharedPreferences.
            putUserInPrefs(app)

            // History JSON can be empty for this test (not relevant here).
            val historyJson = JSONObject()

            // Prepare history future.
            val historyFuture: CompletableFuture<JSONObject?> =
                CompletableFuture.completedFuture(historyJson)

            // Stub getWaterHistoryMap with empty JSON.
            restClientMock.`when`<CompletableFuture<JSONObject?>> {
                RestClient.getWaterHistoryMap("john", 7)
            }.thenReturn(historyFuture)

            // Build LinkedHashMap for weekly averages to preserve insertion order.
            val weekly = LinkedHashMap<String, Int>()
            weekly["Week 1"] = 1000
            weekly["Week 2"] = 1500
            weekly["Week 3"] = 2000
            weekly["Week 4"] = 2500

            // Prepare future for weekly averages.
            val weeklyFuture: CompletableFuture<Map<String, Int>> =
                CompletableFuture.completedFuture(weekly)

            // Stub getWeeklyAverages.
            restClientMock.`when`<CompletableFuture<Map<String, Int>>> {
                RestClient.getWeeklyAverages("john")
            }.thenReturn(weeklyFuture)

            // Build and start WaterChartActivity.
            val activity = Robolectric.buildActivity(WaterChartActivity::class.java).setup().get()

            // Run pending UI tasks.
            Shadows.shadowOf(Looper.getMainLooper()).idle()

            // Find weekly BarChart.
            val weeklyChart: BarChart = activity.findViewById(R.id.barChartWeekly)

            // Make sure it has data.
            val weeklyData = weeklyChart.data
            // Assert that data is not null.
            assertNotNull(weeklyData)

            // Should be exactly 1 DataSet.
            assertEquals(1, weeklyData.dataSetCount)

            // Get DataSet.
            val weekSet = weeklyData.getDataSetByIndex(0) as BarDataSet

            // Should have 4 entries.
            assertEquals(4, weekSet.entryCount)

            // Check Y-values of entries by index.
            val e0 = weekSet.getEntryForIndex(0)
            val e1 = weekSet.getEntryForIndex(1)
            val e2 = weekSet.getEntryForIndex(2)
            val e3 = weekSet.getEntryForIndex(3)

            // Assert Y-values match our Map.
            assertEquals(1000f, e0.y, 0.001f)
            assertEquals(1500f, e1.y, 0.001f)
            assertEquals(2000f, e2.y, 0.001f)
            assertEquals(2500f, e3.y, 0.001f)
        }
    }

    // -------------------------------------------------------------------------
    // TEST 4:
    // Clicking the Back button should start HomePage Activity.
    // -------------------------------------------------------------------------
    @Test
    fun waterChart_backButton_navigatesToHomePage() {
        // Mock static RestClient to avoid real calls during onCreate.
        val restClientMock = Mockito.mockStatic(RestClient::class.java)

        restClientMock.use { restClientMock ->
            // Get application.
            val app = RuntimeEnvironment.getApplication()

            // Put user "john" into SharedPreferences.
            putUserInPrefs(app)

            // Fake minimal future for history so Activity can initialize.
            val historyFuture: CompletableFuture<JSONObject?> =
                CompletableFuture.completedFuture(JSONObject())

            // Fake minimal future for weekly averages.
            val weeklyFuture: CompletableFuture<Map<String, Int>> =
                CompletableFuture.completedFuture(emptyMap())

            // Stub getWaterHistoryMap.
            restClientMock.`when`<CompletableFuture<JSONObject?>> {
                RestClient.getWaterHistoryMap("john", 7)
            }.thenReturn(historyFuture)

            // Stub getWeeklyAverages.
            restClientMock.`when`<CompletableFuture<Map<String, Int>>> {
                RestClient.getWeeklyAverages("john")
            }.thenReturn(weeklyFuture)

            // Build and start WaterChartActivity.
            val activity = Robolectric.buildActivity(WaterChartActivity::class.java).setup().get()

            // Run pending UI tasks.
            Shadows.shadowOf(Looper.getMainLooper()).idle()

            // Find back button.
            val backBtn: ImageButton = activity.findViewById(R.id.imageButton)

            // Click back button.
            backBtn.performClick()

            // Get ShadowActivity to inspect next started Activity.
            val shadowActivity = Shadows.shadowOf(activity)

            // Get next started Intent.
            val started = shadowActivity.nextStartedActivity

            // Assert that an Activity was started.
            assertNotNull(started)

            // Assert that the target Activity is HomePage.
            assertEquals(HomePage::class.java.name, started.component!!.className)
        }
    }
}