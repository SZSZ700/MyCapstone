package com.example.myfinaltopapplication
// Android imports.
import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Looper
import android.widget.Button
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.Switch
import android.widget.TextView
// JUnit + assertions.
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
// Mockito static mocking.
import androidx.test.core.app.ApplicationProvider
import org.mockito.MockedStatic
import org.mockito.Mockito
// Mockito Kotlin matchers.
import org.mockito.kotlin.any
// Robolectric.
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.android.controller.ActivityController
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowAlarmManager
import org.robolectric.shadows.ShadowNotificationManager
import org.robolectric.shadows.ShadowToast
// JSON + Future.
import org.json.JSONObject
import java.util.concurrent.CompletableFuture

// Define the preference key exactly as in WaterActivity.
private const val KEY_WATER_REMINDER_ENABLED = "waterReminderEnabled"

// -----------------------------------------------------------------------------
// WaterActivityTest
// Deep tests for WaterActivity:
// 1. No current user -> redirect to Log in + Toast.
// 2. Logged-in user -> initial server sync updates UI + SharedPreferences.
// 3. updateWater success -> updates text, prefs, and shows success Toast.
// 4. updateWater failure -> does NOT update prefs and shows error Toast.
// 5. History empty -> stats section shows "No history data available".
// 6. Home button -> navigates to HomePage.
// -----------------------------------------------------------------------------
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class WaterActivityTest {

    // -------------------------------------------------------------------------
    // Helper: build activity with NO user in SharedPreferences.
    // -------------------------------------------------------------------------
    private fun buildActivityNoUser(
        restClientMock: MockedStatic<RestClient>
    ): WaterActivity {

        // Stub backend calls so they won't actually run (defensive).
        // Stub getWater.
        restClientMock.`when`<CompletableFuture<JSONObject?>> {
            RestClient.getWater(any<String>())
        }.thenReturn(CompletableFuture.completedFuture(null))

        // Stub getWaterHistoryMap (may or may not be used depending on history).
        restClientMock.`when`<CompletableFuture<JSONObject?>> {
            RestClient.getWaterHistoryMap(any<String>(), any<Int>())
        }.thenReturn(CompletableFuture.completedFuture(null))

        // Stub getGoal (may or may not be used depending on history).
        restClientMock.`when`<CompletableFuture<JSONObject?>> {
            RestClient.getGoal(any<String>())
        }.thenReturn(CompletableFuture.completedFuture(null))

        // Build controller, but DO NOT touch SharedPreferences before setup().
        val controller: ActivityController<WaterActivity> =
            Robolectric.buildActivity(WaterActivity::class.java)

        // Run onCreate / onStart / onResume.
        val activity = controller.setup().get()

        // Process any pending UI tasks.
        Shadows.shadowOf(Looper.getMainLooper()).idle()

        // Return activity.
        return activity
    }

    // -------------------------------------------------------------------------
    // Helper: build activity WITH logged-in user + initial prefs + backend stubs.
    // todayLocal / yestLocal: values to place in SharedPreferences BEFORE server sync.
    // waterJson: JSON that backend getWater(...) should return.
    // historyJson: JSON that backend getWaterHistoryMap(...) should return.
    // goalJson: JSON that backend getGoal(...) should return.
    // -------------------------------------------------------------------------
    private fun buildActivityWithUser(
        todayLocal: Int,
        yestLocal: Int,
        waterJson: JSONObject?,
        historyJson: JSONObject?,
        goalJson: JSONObject?,
        restClientMock: MockedStatic<RestClient>
    ): WaterActivity {

        // Stub backend getWater(...) to return given JSON for this user.
        restClientMock.`when`<CompletableFuture<JSONObject?>> {
            RestClient.getWater("john")
        }.thenReturn(CompletableFuture.completedFuture(waterJson))

        // Stub backend history.
        restClientMock.`when`<CompletableFuture<JSONObject?>> {
            RestClient.getWaterHistoryMap("john", 7)
        }.thenReturn(CompletableFuture.completedFuture(historyJson))

        // Stub getGoal.
        restClientMock.`when`<CompletableFuture<JSONObject?>> {
            RestClient.getGoal("john")
        }.thenReturn(CompletableFuture.completedFuture(goalJson))

        // Prepare controller (onCreate not called yet).
        val controller: ActivityController<WaterActivity> =
            Robolectric.buildActivity(WaterActivity::class.java)

        // Get activity instance BEFORE setup, to access SharedPreferences.
        val activity = controller.get()

        // Fill SharedPreferences with currentuser + initial water values.
        val prefs = activity.getSharedPreferences(
            activity.getString(R.string.myprefs),
            Context.MODE_PRIVATE
        )

        // Save current user and initial water values.
        prefs.edit()
            .putString(activity.getString(R.string.currentuser), "john")
            .putInt("todayWater", todayLocal)
            .putInt("yesterdayWater", yestLocal)
            .commit()

        // Now run lifecycle (onCreate, etc.).
        controller.setup()

        // Process pending UI tasks (runOnUiThread callbacks).
        Shadows.shadowOf(Looper.getMainLooper()).idle()

        // Return activity instance.
        return activity
    }

    // -------------------------------------------------------------------------
    // TEST 1: No logged-in user -> redirect to LoginActivity + Toast message.
    // -------------------------------------------------------------------------
    @Test
    fun noCurrentUser_redirectsToLoginAndShowsToast() {
        // Create static mock for RestClient.
        val restClientMock = Mockito.mockStatic(RestClient::class.java)

        restClientMock.use { restClientMock ->
            // Build activity with NO user in prefs.
            val activity = buildActivityNoUser(restClientMock)

            // Inspect navigation using ShadowActivity.
            val shadowActivity = Shadows.shadowOf(activity)
            // Get started Intent.
            val startedIntent = shadowActivity.nextStartedActivity

            // We expect navigation to LoginActivity.
            // Assert that we navigated to another Activity.
            assertNotNull(startedIntent)
            // Assert that the target Activity is LoginActivity.
            assertEquals(
                LoginActivity::class.java.name,
                startedIntent.component!!.className
            )

            // Check latest Toast text.
            val toastText: CharSequence? = ShadowToast.getTextOfLatestToast()
            // Assert that Toast was shown.
            assertNotNull(toastText)
            // Assert that Toast message equals "You must log in first".
            assertEquals("You must log in first", toastText.toString())

            // Activity should be finishing.
            assertTrue(activity.isFinishing)
        }
    }

    // -------------------------------------------------------------------------
    // TEST 2: With logged-in user, server getWater overrides local prefs and
    // updates UI + SharedPreferences.
    // -------------------------------------------------------------------------
    @Test
    fun onCreate_withUser_syncsWaterFromServerAndUpdatesPrefsAndUi() {
        // Create static mock for RestClient.
        val restClientMock = Mockito.mockStatic(RestClient::class.java)

        restClientMock.use { restClientMock ->
            // Backend JSON that should override local values.
            val waterJson = JSONObject()
            // Set today's water amount.
            waterJson.put("todayWater", 1200)
            // Set yesterday's water amount.
            waterJson.put("yesterdayWater", 800)

            // For this test we don't care about stats -> history empty, goal null.
            val emptyHistory = JSONObject()

            // Build activity with user "john" and local values (will be overridden).
            val activity = buildActivityWithUser(
                50,
                30,
                waterJson,
                emptyHistory,
                null,
                restClientMock
            )

            // Find total water TextView.
            val totalWaterText: TextView = activity.findViewById(R.id.totalWaterText)
            // Find yesterday TextView.
            val yesterdayText: TextView = activity.findViewById(R.id.yesterdayText)

            // UI should show values from server (1200 & 800).
            assertEquals("So far today: 1200 ml",
                totalWaterText.text.toString()
            )
            assertEquals("Yesterday: 800 ml",
                yesterdayText.text.toString()
            )

            // SharedPreferences should also be updated.
            val prefs = activity.getSharedPreferences(
                activity.getString(R.string.myprefs),
                Context.MODE_PRIVATE
            )

            // Assert that SharedPreferences were updated.
            assertEquals(1200, prefs.getInt("todayWater", -1))
            assertEquals(800, prefs.getInt("yesterdayWater", -1))
        }
    }

    // -------------------------------------------------------------------------
    // TEST 3: updateWater success (click 200 ml):
    // - RestClient.updateWater(...) returns true.
    // - TextView updated.
    // - SharedPreferences updated.
    // - Success Toast shown.
    // -------------------------------------------------------------------------
    @Test
    fun updateWater_success_updatesTextPrefsAndShowsSuccessToast() {
        // Create static mock for RestClient.
        val restClientMock = Mockito.mockStatic(RestClient::class.java)

        restClientMock.use { restClientMock ->
            // Initial backend state: 0 today, 0 yesterday.
            val waterJson = JSONObject()
            // Set today's water.
            waterJson.put("todayWater", 0)
            // Set yesterday's water.
            waterJson.put("yesterdayWater", 0)

            // History / goal not important in this test.
            val emptyHistory = JSONObject()

            // Stub updateWater("john", 200) to succeed.
            restClientMock.`when`<CompletableFuture<Boolean>> {
                RestClient.updateWater("john", 200)
            }.thenReturn(CompletableFuture.completedFuture(true))

            // Build activity with logged-in user "john" (local today/yesterday = 0).
            val activity = buildActivityWithUser(
                0,
                0,
                waterJson,
                emptyHistory,
                null,
                restClientMock
            )

            // Find total water TextView.
            val totalWaterText: TextView = activity.findViewById(R.id.totalWaterText)
            // Find 200 ml button.
            val drink200: Button = activity.findViewById(R.id.drink200)

            // Click 200 ml button.
            drink200.performClick()

            // Process UI tasks (runOnUiThread callbacks + Toast).
            Shadows.shadowOf(Looper.getMainLooper()).idle()

            // Text should show 200 ml (0 + 200).
            assertEquals("So far today: 200 ml",
                totalWaterText.text.toString()
            )

            // Get SharedPreferences.
            val prefs = activity.getSharedPreferences(
                activity.getString(R.string.myprefs),
                Context.MODE_PRIVATE
            )
            // SharedPreferences todayWater should be 200.
            assertEquals(200, prefs.getInt("todayWater", -1))

            // Toast text should be "+200 ml saved!".
            val toastText: CharSequence? = ShadowToast.getTextOfLatestToast()
            // Assert that Toast was shown.
            assertNotNull(toastText)
            // Assert that Toast message equals "+200 ml saved!".
            assertEquals("+200 ml saved!", toastText.toString())
        }
    }

    // -------------------------------------------------------------------------
    // TEST 4: updateWater failure (backend returns false):
    // - UI text still updates locally (200 ml).
    // - SharedPreferences NOT updated (remain 0).
    // - Error Toast shown.
    // -------------------------------------------------------------------------
    @Test
    fun updateWater_failure_doesNotChangePrefsAndShowsErrorToast() {
        // Create static mock for RestClient.
        val restClientMock = Mockito.mockStatic(RestClient::class.java)

        restClientMock.use { restClientMock ->
            // Backend state for onCreate.
            val waterJson = JSONObject()
            // Set today's water.
            waterJson.put("todayWater", 0)
            // Set yesterday's water.
            waterJson.put("yesterdayWater", 0)

            // Empty history.
            val emptyHistory = JSONObject()

            // Stub updateWater to FAIL.
            restClientMock.`when`<CompletableFuture<Boolean>> {
                RestClient.updateWater("john", 200)
            }.thenReturn(CompletableFuture.completedFuture(false))

            // Build activity.
            val activity = buildActivityWithUser(0, 0, waterJson,
                emptyHistory,
                null,
                restClientMock
            )

            // Find total water TextView.
            val totalWaterText: TextView = activity.findViewById(R.id.totalWaterText)
            // Find 200 ml button.
            val drink200: Button = activity.findViewById(R.id.drink200)

            // Click 200 ml button.
            drink200.performClick()

            // Process UI tasks.
            Shadows.shadowOf(Looper.getMainLooper()).idle()

            // UI still shows 200 ml (since local counter increased).
            assertEquals("So far today: 200 ml",
                totalWaterText.text.toString()
            )

            // Get SharedPreferences.
            val prefs = activity.getSharedPreferences(
                activity.getString(R.string.myprefs),
                Context.MODE_PRIVATE
            )
            // SharedPreferences should remain 0 because updateWater failed.
            assertEquals(0, prefs.getInt("todayWater", 0))

            // Toast message should indicate failure.
            val toastText: CharSequence? = ShadowToast.getTextOfLatestToast()
            // Assert that Toast was shown.
            assertNotNull(toastText)
            // Assert that Toast message equals "Failed to update water".
            assertEquals("Failed to update water", toastText.toString())
        }
    }

    // -------------------------------------------------------------------------
    // TEST 5: History is empty JSON -> stats section shows "No history data"
    // and progress bar is 0; best/lowest day show "no data".
    // -------------------------------------------------------------------------
    @Test
    fun historyEmpty_showsNoHistoryStats() {
        // Create static mock for RestClient.
        val restClientMock = Mockito.mockStatic(RestClient::class.java)

        restClientMock.use { restClientMock ->
            // getWater: some basic values.
            val waterJson = JSONObject()
            // Set today's water.
            waterJson.put("todayWater", 500)
            // Set yesterday's water.
            waterJson.put("yesterdayWater", 300)

            // Empty history -> triggers "No history data available".
            val emptyHistory = JSONObject()

            // Goal JSON.
            val goalJson = JSONObject()
            // Set daily goal.
            goalJson.put("goalMl", 3000)

            // Build activity with user "john".
            val activity = buildActivityWithUser(0, 0, waterJson,
                emptyHistory,
                goalJson,
                restClientMock
            )

            // Find goal summary title.
            val goalSummaryTitle: TextView = activity.findViewById(R.id.goalSummaryTitle)
            // Find goal summary text.
            val goalSummaryText: TextView = activity.findViewById(R.id.goalSummaryText)
            // Find goal ProgressBar.
            val goalProgressBar: ProgressBar = activity.findViewById(R.id.goalProgressBar)
            // Find best day TextView.
            val bestDayText: TextView = activity.findViewById(R.id.bestDayText)
            // Find lowest day TextView.
            val lowestDayText: TextView = activity.findViewById(R.id.lowestDayText)

            // Title mentions last 7 days.
            assertEquals("Goal consistency (last 7 days)",
                goalSummaryTitle.text.toString()
            )
            // Text indicates no history.
            assertEquals("No history data available",
                goalSummaryText.text.toString()
            )
            // Progress bar 0.
            assertEquals(0, goalProgressBar.progress)
            // Best day should be "no data".
            assertEquals("Best day: no data",
                bestDayText.text.toString()
            )
            // Lowest day should be "no data".
            assertEquals("Lowest day: no data",
                lowestDayText.text.toString()
            )
        }
    }

    // -------------------------------------------------------------------------
    // TEST 6: Home button starts HomePage activity.
    // -------------------------------------------------------------------------
    @Test
    fun clickingHomeButton_startsHomePage() {
        // Create static mock for RestClient.
        val restClientMock = Mockito.mockStatic(RestClient::class.java)

        restClientMock.use { restClientMock ->
            // Basic backend values for onCreate.
            val waterJson = JSONObject()
            // Set today's water.
            waterJson.put("todayWater", 0)
            // Set yesterday's water.
            waterJson.put("yesterdayWater", 0)

            // Empty history.
            val emptyHistory = JSONObject()

            // Build activity.
            val activity = buildActivityWithUser(0, 0, waterJson,
                emptyHistory,
                null,
                restClientMock
            )

            // Find home ImageButton.
            val homeBtn: ImageButton = activity.findViewById(R.id.imageButton4)

            // Click home.
            homeBtn.performClick()

            // Process UI tasks.
            Shadows.shadowOf(Looper.getMainLooper()).idle()

            // Inspect started activity.
            val shadowActivity = Shadows.shadowOf(activity)
            // Get started Intent.
            val startedIntent = shadowActivity.nextStartedActivity

            // We expect navigation to HomePage.
            // Assert that we navigated to another Activity.
            assertNotNull(startedIntent)
            // Assert that the target Activity is HomePage.
            assertEquals(HomePage::class.java.name,
                startedIntent.component!!.className
            )
        }
    }

    // -------------------------------------------------------------------------
    // Helper: Seed SharedPreferences with a logged-in user so WaterActivity won't finish().
    // -------------------------------------------------------------------------
    private fun seedLoggedInUserPrefs(context: Context) {
        // Get the same SharedPreferences file WaterActivity uses.
        val prefs: SharedPreferences = context.getSharedPreferences(
            context.getString(R.string.myprefs),
            Context.MODE_PRIVATE
        )

        // Save a non-null current user so WaterActivity does not redirect to LoginActivity.
        prefs.edit()
            .putString(context.getString(R.string.currentuser), "testUser")
            .putInt("todayWater", 0)
            .putInt("yesterdayWater", 0)
            .putBoolean(KEY_WATER_REMINDER_ENABLED, false)
            .apply()
    }

    // -------------------------------------------------------------------------
    // Helper: Build WaterActivity safely with RestClient mocked.
    // -------------------------------------------------------------------------
    private fun buildWaterActivityWithRestClientMock(restClientMock: MockedStatic<RestClient>
    ): WaterActivity {
        // Get application context.
        val context: Context = ApplicationProvider.getApplicationContext()

        // Seed prefs so activity continues normally.
        seedLoggedInUserPrefs(context)

        // Create fake JSON response for getWater.
        val waterJson = JSONObject()
        // Put today water amount.
        waterJson.put("todayWater", 0)
        // Put yesterday water amount.
        waterJson.put("yesterdayWater", 0)

        // Stub RestClient.getWater to return completed future immediately.
        restClientMock.`when`<CompletableFuture<JSONObject?>> {
            RestClient.getWater("testUser")
        }.thenReturn(CompletableFuture.completedFuture(waterJson))

        // Create fake JSON history map for last 7 days.
        val historyJson = JSONObject()
        // Put at least one date so WaterActivity stats code won't behave oddly.
        historyJson.put("2025-01-01", 0)

        // Stub RestClient.getWaterHistoryMap to return completed future immediately.
        restClientMock.`when`<CompletableFuture<JSONObject?>> {
            RestClient.getWaterHistoryMap("testUser", 7)
        }.thenReturn(CompletableFuture.completedFuture(historyJson))

        // Create fake goal JSON response.
        val goalJson = JSONObject()
        // Put goal ml.
        goalJson.put("goalMl", 3000)

        // Stub RestClient.getGoal to return completed future immediately.
        restClientMock.`when`<CompletableFuture<JSONObject?>> {
            RestClient.getGoal("testUser")
        }.thenReturn(CompletableFuture.completedFuture(goalJson))

        // Build the activity.
        val activity = Robolectric.buildActivity(WaterActivity::class.java)
            .create()
            .start()
            .resume()
            .get()

        // Flush main looper tasks scheduled by onCreate async callbacks.
        Shadows.shadowOf(Looper.getMainLooper()).idle()

        // Return the built activity.
        return activity
    }

    // -------------------------------------------------------------------------
    // Helper: Get "next alarm" in a version-safe way.
    // -------------------------------------------------------------------------
    private fun getNextAlarm(
        shadowAm: ShadowAlarmManager
    ): ShadowAlarmManager.ScheduledAlarm? {
        // Try to peek without consuming.
        return try {
            // Call peekNextScheduledAlarm if present.
            shadowAm.peekNextScheduledAlarm()
        } catch (_: Throwable) {
            // Fall back to the first scheduled alarm if peek is not available.
            shadowAm.scheduledAlarms.firstOrNull()
        }
    }

    // -------------------------------------------------------------------------
    // TEST 7: Toggle ON -> saves prefs + registers alarm (deep + stable).
    // -------------------------------------------------------------------------
    @Test
    fun reminderToggle_on_savesPrefs_andRegistersAlarm() {
        // Create static mock for RestClient.
        val restClientMock = Mockito.mockStatic(RestClient::class.java)

        restClientMock.use { restClientMock ->
            // Build WaterActivity with seeded prefs and RestClient stubs.
            val activity =
                buildWaterActivityWithRestClientMock(restClientMock)

            // Find the reminder switch in the layout.
            val reminderSwitch: Switch =
                activity.findViewById(R.id.switchWaterReminder)

            // Turn ON the switch (this triggers OnCheckedChangeListener).
            reminderSwitch.isChecked = true

            // Flush UI tasks to ensure the listener completed.
            Shadows.shadowOf(Looper.getMainLooper()).idle()

            // Read preferences from the same prefs file.
            val prefs = activity.getSharedPreferences(
                activity.getString(R.string.myprefs),
                Context.MODE_PRIVATE
            )

            // Assert the boolean was saved as true.
            assertTrue(
                prefs.getBoolean(
                    KEY_WATER_REMINDER_ENABLED,
                    false
                )
            )

            // Get AlarmManager service.
            val am = activity.getSystemService(Context.ALARM_SERVICE) as AlarmManager

            // Get shadow AlarmManager.
            val shadowAm = Shadows.shadowOf(am)

            // Obtain the next scheduled alarm in a stable way.
            val alarm = getNextAlarm(shadowAm)

            // Assert alarm exists (meaning schedule happened).
            assertNotNull(alarm)
        }
    }

    // -------------------------------------------------------------------------
    // TEST 8: Toggle OFF -> saves prefs false + cancels alarm (deep + stable).
    // -------------------------------------------------------------------------
    @Test
    fun reminderToggle_off_savesPrefs_andCancelsAlarm() {
        // Create static mock for RestClient.
        val restClientMock = Mockito.mockStatic(RestClient::class.java)

        restClientMock.use { restClientMock ->
            // Build WaterActivity with seeded prefs and RestClient stubs.
            val activity =
                buildWaterActivityWithRestClientMock(restClientMock)

            // Find the reminder switch in the layout.
            val reminderSwitch: Switch = activity.findViewById(R.id.switchWaterReminder)

            // Turn ON first so we have an alarm to cancel.
            reminderSwitch.isChecked = true

            // Flush UI tasks to ensure scheduling happened.
            Shadows.shadowOf(Looper.getMainLooper()).idle()

            // Get AlarmManager service.
            val am = activity.getSystemService(Context.ALARM_SERVICE) as AlarmManager

            // Get shadow AlarmManager.
            val shadowAm = Shadows.shadowOf(am)

            // Verify there is some scheduled alarm.
            assertNotNull(getNextAlarm(shadowAm))

            // Turn OFF the switch (should call stopWaterReminder).
            reminderSwitch.isChecked = false

            // Flush UI tasks to ensure cancellation happened.
            Shadows.shadowOf(Looper.getMainLooper()).idle()

            // Read preferences again.
            val prefs = activity.getSharedPreferences(
                activity.getString(R.string.myprefs),
                Context.MODE_PRIVATE
            )

            // Assert the boolean was saved as false.
            assertFalse(
                prefs.getBoolean(
                    KEY_WATER_REMINDER_ENABLED,
                    true
                )
            )

            // After cancel, next alarm should be null.
            assertNull(getNextAlarm(shadowAm))
        }
    }

    // -------------------------------------------------------------------------
    // TEST 9: Receiver -> posts EXACT notification title/text as in your code.
    // -------------------------------------------------------------------------
    @Test
    fun receiver_onReceive_postsNotification_withExpectedTitleAndText() {
        // Get application context.
        val context: Context = ApplicationProvider.getApplicationContext()

        // Get NotificationManager service.
        val nm =
            context.getSystemService(Context.NOTIFICATION_SERVICE)
                    as NotificationManager

        // Get shadow NotificationManager.
        val shadowNm: ShadowNotificationManager = Shadows.shadowOf(nm)

        // Assert no notifications exist at start.
        assertEquals(0, shadowNm.allNotifications.size)

        // Create the receiver instance.
        val receiver = WaterReminderReceiver()

        // Create an intent targeting the receiver.
        val intent =
            Intent(context, WaterReminderReceiver::class.java)

        // Trigger onReceive manually.
        receiver.onReceive(context, intent)

        // Fetch notifications posted so far.
        assertEquals(1, shadowNm.allNotifications.size)

        // Grab the notification object.
        val n: Notification = shadowNm.allNotifications[0]

        // Read title from extras.
        val title =
            n.extras.getString(Notification.EXTRA_TITLE)

        // Read text from extras.
        val text =
            n.extras.getString(Notification.EXTRA_TEXT)

        // Assert title matches your receiver code.
        assertEquals("Water reminder", title)

        // Assert text matches your receiver code.
        assertEquals("Time to drink water 💧", text)
    }

    // -------------------------------------------------------------------------
    // TEST 10: End-to-end: Toggle ON registers alarm + receiver posts notification.
    // -------------------------------------------------------------------------
    @Test
    fun reminderToggle_on_thenReceiver_postsNotification_endToEnd() {
        // Create static mock for RestClient.
        val restClientMock = Mockito.mockStatic(RestClient::class.java)

        restClientMock.use { restClientMock ->
            // Build WaterActivity with seeded prefs and RestClient stubs.
            val activity =
                buildWaterActivityWithRestClientMock(restClientMock)

            // Find the reminder switch.
            val reminderSwitch: Switch =
                activity.findViewById(R.id.switchWaterReminder)

            // Turn ON reminder.
            reminderSwitch.isChecked = true

            // Flush UI tasks.
            Shadows.shadowOf(Looper.getMainLooper()).idle()

            // Get AlarmManager service.
            val am =
                activity.getSystemService(Context.ALARM_SERVICE) as AlarmManager

            // Get shadow AlarmManager.
            val shadowAm = Shadows.shadowOf(am)

            // Assert alarm exists.
            assertNotNull(getNextAlarm(shadowAm))

            // Get NotificationManager service.
            val nm =
                activity.getSystemService(Context.NOTIFICATION_SERVICE)
                        as NotificationManager

            // Get shadow NotificationManager.
            val shadowNm: ShadowNotificationManager = Shadows.shadowOf(nm)

            // Assert no notifications exist before receiver triggers.
            assertEquals(0, shadowNm.allNotifications.size)

            // Create receiver.
            val receiver = WaterReminderReceiver()

            // Trigger receiver.
            receiver.onReceive(
                activity,
                Intent(
                    activity,
                    WaterReminderReceiver::class.java
                )
            )

            // Assert notification exists.
            assertEquals(1, shadowNm.allNotifications.size)

            // Grab the notification.
            val n: Notification = shadowNm.allNotifications[0]

            // Assert title.
            assertEquals(
                "Water reminder",
                n.extras.getString(Notification.EXTRA_TITLE)
            )

            // Assert text.
            assertEquals(
                "Time to drink water 💧",
                n.extras.getString(Notification.EXTRA_TEXT)
            )
        }
    }
}