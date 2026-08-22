package com.example.myfinaltopapplication;
// Android imports
import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Looper;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.Switch;
import android.widget.TextView;
// JUnit + assertions
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;
// Mockito static mocking
import androidx.test.core.app.ApplicationProvider;

import org.mockito.MockedStatic;
import org.mockito.Mockito;
// Robolectric
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.Shadows;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.shadows.ShadowAlarmManager;
import org.robolectric.shadows.ShadowNotificationManager;
import org.robolectric.shadows.ShadowToast;
// JSON + Future
import org.json.JSONObject;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

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
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class WaterActivityTest {

    // -------------------------------------------------------------------------
    // Helper: build activity with NO user in SharedPreferences
    // -------------------------------------------------------------------------
    private WaterActivity buildActivityNoUser(MockedStatic<RestClient> restClientMock) {
        // Stub backend calls so they won't actually run (defensive)
        // Stub getWater
        restClientMock.when(
                // Lambda for RestClient.getWater(...)
                () -> RestClient.getWater(Mockito.anyString())
        ).thenReturn(CompletableFuture.completedFuture(null));

        // Stub getWaterHistoryMap (may or may not be used depending on history)
        restClientMock.when(
                // Lambda for RestClient.getWaterHistoryMap(...)
                () -> RestClient.getWaterHistoryMap(Mockito.anyString(), Mockito.anyInt())
        ).thenReturn(CompletableFuture.completedFuture(null));

        // Stub getGoal (may or may not be used depending on history)
        restClientMock.when(
                // Lambda for RestClient.getGoal(...)
                () -> RestClient.getGoal(Mockito.anyString())
        ).thenReturn(CompletableFuture.completedFuture(null));

        // Build controller, but DO NOT touch SharedPreferences before setup()
        ActivityController<WaterActivity> controller =
                Robolectric.buildActivity(WaterActivity.class);

        // Run onCreate / onStart / onResume
        WaterActivity activity = controller.setup().get();

        // Process any pending UI tasks
        Shadows.shadowOf(Looper.getMainLooper()).idle();

        return activity;
    }

    // -------------------------------------------------------------------------
    // Helper: build activity WITH logged-in user + initial prefs + backend stubs
    // userName: username to set in prefs
    // todayLocal / yestLocal: values to place in SharedPreferences BEFORE server sync
    // waterJson: JSON that backend getWater(...) should return
    // historyJson: JSON that backend getWaterHistoryMap(...) should return
    // goalJson: JSON that backend getGoal(...) should return
    // -------------------------------------------------------------------------
    private WaterActivity buildActivityWithUser(
            int todayLocal,
            int yestLocal,
            JSONObject waterJson,
            JSONObject historyJson,
            JSONObject goalJson,
            MockedStatic<RestClient> restClientMock
    ) {

        // Stub backend getWater(...) to return given JSON for this user
        restClientMock.when(
                // Lambda for RestClient.getWater(...)
                () -> RestClient.getWater("john")
        ).thenReturn(CompletableFuture.completedFuture(waterJson));

        // Stub backend history
        restClientMock.when(
                // Lambda for RestClient.getWaterHistoryMap(...)
                () -> RestClient.getWaterHistoryMap("john", 7)
        ).thenReturn(CompletableFuture.completedFuture(historyJson));

        // Stub getGoal
        restClientMock.when(
                // Lambda for RestClient.getGoal(...)
                () -> RestClient.getGoal("john")
        ).thenReturn(CompletableFuture.completedFuture(goalJson));

        // Prepare controller (onCreate not called yet)
        ActivityController<WaterActivity> controller =
                Robolectric.buildActivity(WaterActivity.class);

        // Get activity instance BEFORE setup, to access SharedPreferences
        WaterActivity activity = controller.get();

        // Fill SharedPreferences with currentuser + initial water values
        var prefs = activity.getSharedPreferences(
                activity.getString(R.string.myprefs),
                Context.MODE_PRIVATE
        );

        prefs.edit()
                .putString(activity.getString(R.string.currentuser), "john")
                .putInt("todayWater", todayLocal)
                .putInt("yesterdayWater", yestLocal)
                .commit();

        // Now run lifecycle (onCreate, etc.)
        controller.setup();

        // Process pending UI tasks (runOnUiThread callbacks)
        Shadows.shadowOf(Looper.getMainLooper()).idle();

        // Return activity instance
        return activity;
    }

    // -------------------------------------------------------------------------
    // TEST 1: No logged-in user -> redirect to LoginActivity + Toast message.
    // -------------------------------------------------------------------------
    @Test
    public void noCurrentUser_redirectsToLoginAndShowsToast() {
        try (var restClientMock = Mockito.mockStatic(RestClient.class)) {
            // Build activity with NO user in prefs
            var activity = buildActivityNoUser(restClientMock);

            // Inspect navigation using ShadowActivity
            var shadowActivity = Shadows.shadowOf(activity);
            // Get started Intent
            var startedIntent = shadowActivity.getNextStartedActivity();

            // We expect navigation to LoginActivity
            // Assert that we navigated to another Activity
            assertNotNull(startedIntent);
            // Assert that the target Activity is LoginActivity
            assertEquals(LoginActivity.class.getName(),
                    Objects.requireNonNull(startedIntent.getComponent()).getClassName());

            // Check latest Toast text
            CharSequence toastText = ShadowToast.getTextOfLatestToast();
            // Assert that Toast was shown
            assertNotNull(toastText);
            // Assert that Toast message equals "You must log in first"
            assertEquals("You must log in first", toastText.toString());

            // Activity should be finishing
            assertTrue(activity.isFinishing());
        }
    }

    // -------------------------------------------------------------------------
    // TEST 2: With logged-in user, server getWater overrides local prefs and
    // updates UI + SharedPreferences.
    // -------------------------------------------------------------------------
    @Test
    public void onCreate_withUser_syncsWaterFromServerAndUpdatesPrefsAndUi() throws Exception {
        try (var restClientMock = Mockito.mockStatic(RestClient.class)) {

            // Backend JSON that should override local values
            var waterJson = new JSONObject();
            waterJson.put("todayWater", 1200);
            waterJson.put("yesterdayWater", 800);

            // For this test we don't care about stats → history empty, goal null
            var emptyHistory = new JSONObject(); // length() == 0

            // Build activity with user "john" and local values (will be overridden)
            var activity = buildActivityWithUser(
                    50,   // local todayWater
                    30,   // local yesterdayWater
                    waterJson,
                    emptyHistory,
                    null,
                    restClientMock
            );

            // Find text views
            TextView totalWaterText = activity.findViewById(R.id.totalWaterText);
            TextView yesterdayText = activity.findViewById(R.id.yesterdayText);

            // UI should show values from server (1200 & 800)
            assertEquals("So far today: 1200 ml", totalWaterText.getText().toString());
            assertEquals("Yesterday: 800 ml", yesterdayText.getText().toString());

            // SharedPreferences should also be updated
            var prefs = activity.getSharedPreferences(
                    activity.getString(R.string.myprefs),
                    Context.MODE_PRIVATE
            );

            // Assert that SharedPreferences were updated
            assertEquals(1200, prefs.getInt("todayWater", -1));
            assertEquals(800, prefs.getInt("yesterdayWater", -1));
        }
    }

    // -------------------------------------------------------------------------
    // TEST 3: updateWater success (click 200 ml):
    // - RestClient.updateWater(...) returns true
    // - TextView updated
    // - SharedPreferences updated
    // - Success Toast shown.
    // -------------------------------------------------------------------------
    @Test
    public void updateWater_success_updatesTextPrefsAndShowsSuccessToast() throws Exception {
        try (var restClientMock = Mockito.mockStatic(RestClient.class)) {

            // Initial backend state: 0 today, 0 yesterday
            var waterJson = new JSONObject();
            waterJson.put("todayWater", 0);
            waterJson.put("yesterdayWater", 0);

            // History / goal not important in this test
            var emptyHistory = new JSONObject();

            // Stub updateWater("john", 200) to succeed
            restClientMock.when(
                    // Lambda for RestClient.updateWater(...)
                    () -> RestClient.updateWater("john", 200)
            ).thenReturn(CompletableFuture.completedFuture(true));

            // Stub getWater / history / goal used in onCreate
            restClientMock.when(
                    // Lambda for RestClient.getWater(...)
                    () -> RestClient.getWater("john")
            ).thenReturn(CompletableFuture.completedFuture(waterJson));

            // Stub getWaterHistoryMap
            restClientMock.when(
                    // Lambda for RestClient.getWaterHistoryMap(...)
                    () -> RestClient.getWaterHistoryMap("john", 7)
            ).thenReturn(CompletableFuture.completedFuture(emptyHistory));

            // Stub getGoal
            restClientMock.when(
                    // Lambda for RestClient.getGoal(...)
                    () -> RestClient.getGoal("john")
            ).thenReturn(CompletableFuture.completedFuture(null));

            // Build activity with logged-in user "john" (local today/yesterday = 0)
            var activity = buildActivityWithUser(
                    0,
                    0,
                    waterJson,
                    emptyHistory,
                    null,
                    restClientMock
            );

            // Find views
            TextView totalWaterText = activity.findViewById(R.id.totalWaterText);
            Button drink200 = activity.findViewById(R.id.drink200);

            // Click 200 ml button
            drink200.performClick();

            // Process UI tasks (runOnUiThread callbacks + Toast)
            Shadows.shadowOf(Looper.getMainLooper()).idle();

            // Text should show 200 ml (0 + 200)
            assertEquals("So far today: 200 ml", totalWaterText.getText().toString());

            // SharedPreferences todayWater should be 200
            var prefs = activity.getSharedPreferences(
                    activity.getString(R.string.myprefs),
                    Context.MODE_PRIVATE
            );
            // Assert that SharedPreferences were updated
            assertEquals(200, prefs.getInt("todayWater", -1));

            // Toast text should be "+200 ml saved!"
            CharSequence toastText = ShadowToast.getTextOfLatestToast();
            // Assert that Toast was shown
            assertNotNull(toastText);
            // Assert that Toast message equals "+200 ml saved!"
            assertEquals("+200 ml saved!", toastText.toString());
        }
    }

    // -------------------------------------------------------------------------
    // TEST 4: updateWater failure (backend returns false):
    // - UI text still updates locally (200 ml)
    // - SharedPreferences NOT updated (remain 0)
    // - Error Toast shown.
    // -------------------------------------------------------------------------
    @Test
    public void updateWater_failure_doesNotChangePrefsAndShowsErrorToast() throws Exception {
        try (var restClientMock = Mockito.mockStatic(RestClient.class)) {

            // Backend state for onCreate
            var waterJson = new JSONObject();
            waterJson.put("todayWater", 0);
            waterJson.put("yesterdayWater", 0);

            var emptyHistory = new JSONObject();

            // Stub updateWater to FAIL
            restClientMock.when(
                    // Lambda for RestClient.updateWater(...)
                    () -> RestClient.updateWater("john", 200)
            ).thenReturn(CompletableFuture.completedFuture(false));

            // Stub calls from onCreate
            restClientMock.when(
                    // Lambda for RestClient.getWater(...)
                    () -> RestClient.getWater("john")
            ).thenReturn(CompletableFuture.completedFuture(waterJson));

            // Stub getWaterHistoryMap
            restClientMock.when(
                    // Lambda for RestClient.getWaterHistoryMap(...)
                    () -> RestClient.getWaterHistoryMap("john", 7)
            ).thenReturn(CompletableFuture.completedFuture(emptyHistory));

            // Stub getGoal
            restClientMock.when(
                    // Lambda for RestClient.getGoal(...)
                    () -> RestClient.getGoal("john")
            ).thenReturn(CompletableFuture.completedFuture(null));

            // Build activity
            var activity = buildActivityWithUser(
                    0,
                    0,
                    waterJson,
                    emptyHistory,
                    null,
                    restClientMock
            );

            // Find button & label
            TextView totalWaterText = activity.findViewById(R.id.totalWaterText);
            Button drink200 = activity.findViewById(R.id.drink200);

            // Click 200 ml button
            drink200.performClick();

            // Process UI tasks
            Shadows.shadowOf(Looper.getMainLooper()).idle();

            // UI still shows 200 ml (since local counter increased)
            assertEquals("So far today: 200 ml", totalWaterText.getText().toString());

            // BUT SharedPreferences should remain 0 (updateWater failed)
            var prefs = activity.getSharedPreferences(
                    activity.getString(R.string.myprefs),
                    Context.MODE_PRIVATE
            );
            // Assert that SharedPreferences were NOT updated
            assertEquals(0, prefs.getInt("todayWater", 0));

            // Toast message should indicate failure
            CharSequence toastText = ShadowToast.getTextOfLatestToast();
            // Assert that Toast was shown
            assertNotNull(toastText);
            // Assert that Toast message equals "Failed to update water"
            assertEquals("Failed to update water", toastText.toString());
        }
    }

    // -------------------------------------------------------------------------
    // TEST 5: History is empty JSON -> stats section shows "No history data"
    // and progress bar is 0; best/lowest day show "no data".
    // -------------------------------------------------------------------------
    @Test
    public void historyEmpty_showsNoHistoryStats() throws Exception {
        try (var restClientMock = Mockito.mockStatic(RestClient.class)) {

            // getWater: some basic values
            var waterJson = new JSONObject();
            waterJson.put("todayWater", 500);
            waterJson.put("yesterdayWater", 300);

            // Empty history → triggers "No history data available"
            var emptyHistory = new JSONObject();

            // Goal JSON
            var goalJson = new JSONObject();
            goalJson.put("goalMl", 3000);

            // Build activity with user "john"
            var activity = buildActivityWithUser(
                    0,
                    0,
                    waterJson,
                    emptyHistory,
                    goalJson,
                    restClientMock
            );

            // Find stats views
            TextView goalSummaryTitle = activity.findViewById(R.id.goalSummaryTitle);
            TextView goalSummaryText = activity.findViewById(R.id.goalSummaryText);
            ProgressBar goalProgressBar = activity.findViewById(R.id.goalProgressBar);
            TextView bestDayText = activity.findViewById(R.id.bestDayText);
            TextView lowestDayText = activity.findViewById(R.id.lowestDayText);

            // Title mentions last 7 days
            assertEquals("Goal consistency (last 7 days)", goalSummaryTitle.getText().toString());
            // Text indicates no history
            assertEquals("No history data available", goalSummaryText.getText().toString());
            // Progress bar 0
            assertEquals(0, goalProgressBar.getProgress());
            // Best/lowest labels
            // Best/lowest should be "no data"
            assertEquals("Best day: no data", bestDayText.getText().toString());
            // Best/lowest should be "no data"
            assertEquals("Lowest day: no data", lowestDayText.getText().toString());
        }
    }

    // -------------------------------------------------------------------------
    // TEST 6: Home button starts HomePage activity.
    // -------------------------------------------------------------------------
    @Test
    public void clickingHomeButton_startsHomePage() throws Exception {
        try (var restClientMock = Mockito.mockStatic(RestClient.class)) {

            // Basic backend stubs for onCreate
            var waterJson = new JSONObject();
            waterJson.put("todayWater", 0);
            waterJson.put("yesterdayWater", 0);
            var emptyHistory = new JSONObject();

            restClientMock.when(
                    // Lambda for RestClient.getWater(...)
                    () -> RestClient.getWater("john")
            ).thenReturn(CompletableFuture.completedFuture(waterJson));

            // Stub getWaterHistoryMap
            restClientMock.when(
                    // Lambda for RestClient.getWaterHistoryMap(...)
                    () -> RestClient.getWaterHistoryMap("john", 7)
            ).thenReturn(CompletableFuture.completedFuture(emptyHistory));

            // Stub getGoal
            restClientMock.when(
                    // Lambda for RestClient.getGoal(...)
                    () -> RestClient.getGoal("john")
            ).thenReturn(CompletableFuture.completedFuture(null));

            // Build activity
            var activity = buildActivityWithUser(
                    0,
                    0,
                    waterJson,
                    emptyHistory,
                    null,
                    restClientMock
            );

            // Find home ImageButton
            ImageButton homeBtn = activity.findViewById(R.id.imageButton4);

            // Click home
            homeBtn.performClick();

            // Process UI tasks
            Shadows.shadowOf(Looper.getMainLooper()).idle();

            // Inspect started activity
            var shadowActivity = Shadows.shadowOf(activity);
            // Get started Intent
            var startedIntent = shadowActivity.getNextStartedActivity();

            // We expect navigation to HomePage
            // Assert that we navigated to another Activity
            assertNotNull(startedIntent);
            // Assert that the target Activity is HomePage
            assertEquals(HomePage.class.getName(),
                    Objects.requireNonNull(startedIntent.getComponent()).getClassName());
        }
    }

    // Define the preference key exactly as in WaterActivity
    private static final String KEY_WATER_REMINDER_ENABLED = "waterReminderEnabled";

    // -------------------------------------------------------------------------
    // Helper: Seed SharedPreferences with a logged-in user so WaterActivity won't finish()
    // -------------------------------------------------------------------------
    private void seedLoggedInUserPrefs(Context context) {

        // Get the same SharedPreferences file WaterActivity uses
        SharedPreferences prefs = context.getSharedPreferences(
                context.getString(R.string.myprefs),
                Context.MODE_PRIVATE
        );

        // Save a non-null current user so WaterActivity does not redirect to LoginActivity
        prefs.edit()
                .putString(context.getString(R.string.currentuser), "testUser")
                .putInt("todayWater", 0)
                .putInt("yesterdayWater", 0)
                .putBoolean(KEY_WATER_REMINDER_ENABLED, false)
                .apply();
    }

    // -------------------------------------------------------------------------
    // Helper: Build WaterActivity safely with RestClient mocked
    // -------------------------------------------------------------------------
    private WaterActivity buildWaterActivityWithRestClientMock(MockedStatic<RestClient> restClientMock) throws Exception {

        // Get application context
        Context context = ApplicationProvider.getApplicationContext();

        // Seed prefs so activity continues normally
        seedLoggedInUserPrefs(context);

        // Create fake JSON response for getWater
        JSONObject waterJson = new JSONObject();
        // Put today water amount
        waterJson.put("todayWater", 0);
        // Put yesterday water amount
        waterJson.put("yesterdayWater", 0);

        // Stub RestClient.getWater to return completed future immediately
        restClientMock.when(() -> RestClient.getWater("testUser"))
                .thenReturn(CompletableFuture.completedFuture(waterJson));

        // Create fake JSON history map for last 7 days
        JSONObject historyJson = new JSONObject();
        // Put at least one date so WaterActivity stats code won't behave oddly
        historyJson.put("2025-01-01", 0);

        // Stub RestClient.getWaterHistoryMap to return completed future immediately
        restClientMock.when(() -> RestClient.getWaterHistoryMap("testUser", 7))
                .thenReturn(CompletableFuture.completedFuture(historyJson));

        // Create fake goal JSON response
        JSONObject goalJson = new JSONObject();
        // Put goal ml
        goalJson.put("goalMl", 3000);

        // Stub RestClient.getGoal to return completed future immediately
        restClientMock.when(() -> RestClient.getGoal("testUser"))
                .thenReturn(CompletableFuture.completedFuture(goalJson));

        // Build the activity
        WaterActivity activity = Robolectric.buildActivity(WaterActivity.class)
                .create()
                .start()
                .resume()
                .get();

        // Flush main looper tasks scheduled by onCreate async callbacks
        Shadows.shadowOf(Looper.getMainLooper()).idle();

        // Return the built activity
        return activity;
    }

    // -------------------------------------------------------------------------
    // Helper: Get "next alarm" in a version-safe way
    // -------------------------------------------------------------------------
    private ShadowAlarmManager.ScheduledAlarm getNextAlarm(ShadowAlarmManager shadowAm) {
        // Try to peek without consuming
        try {
            // Call peekNextScheduledAlarm via reflection-less direct method if present
            return shadowAm.peekNextScheduledAlarm();
        } catch (Throwable ignored) {
            // Fall back to getNextScheduledAlarm if peek is not available
            return shadowAm.getScheduledAlarms().get(0);
        }
    }

    // -------------------------------------------------------------------------
    // TEST 1: Toggle ON -> saves prefs + registers alarm (deep + stable)
    // -------------------------------------------------------------------------
    @Test
    public void reminderToggle_on_savesPrefs_andRegistersAlarm() throws Exception {
        // Create static mock for RestClient
        try (MockedStatic<RestClient> restClientMock = Mockito.mockStatic(RestClient.class)) {

            // Build WaterActivity with seeded prefs and RestClient stubs
            WaterActivity activity = buildWaterActivityWithRestClientMock(restClientMock);

            // Find the reminder switch in the layout
            Switch reminderSwitch = activity.findViewById(R.id.switchWaterReminder);

            // Turn ON the switch (this triggers OnCheckedChangeListener)
            reminderSwitch.setChecked(true);

            // Flush UI tasks to ensure the listener completed
            Shadows.shadowOf(Looper.getMainLooper()).idle();

            // Read preferences from the same prefs file
            SharedPreferences prefs = activity.getSharedPreferences(
                    activity.getString(R.string.myprefs),
                    Context.MODE_PRIVATE
            );

            // Assert the boolean was saved as true
            assertTrue(prefs.getBoolean(KEY_WATER_REMINDER_ENABLED, false));

            // Get AlarmManager service
            AlarmManager am = (AlarmManager) activity.getSystemService(Context.ALARM_SERVICE);

            // Get shadow AlarmManager
            ShadowAlarmManager shadowAm = Shadows.shadowOf(am);

            // Obtain the next scheduled alarm in a stable way
            ShadowAlarmManager.ScheduledAlarm alarm = getNextAlarm(shadowAm);

            // Assert alarm exists (meaning schedule happened)
            assertNotNull(alarm);
        }
    }

    // -------------------------------------------------------------------------
    // TEST 2: Toggle OFF -> saves prefs false + cancels alarm (deep + stable)
    // -------------------------------------------------------------------------
    @Test
    public void reminderToggle_off_savesPrefs_andCancelsAlarm() throws Exception {

        // Create static mock for RestClient
        try (MockedStatic<RestClient> restClientMock = Mockito.mockStatic(RestClient.class)) {

            // Build WaterActivity with seeded prefs and RestClient stubs
            WaterActivity activity = buildWaterActivityWithRestClientMock(restClientMock);

            // Find the reminder switch in the layout
            Switch reminderSwitch = activity.findViewById(R.id.switchWaterReminder);

            // Turn ON first so we have an alarm to cancel
            reminderSwitch.setChecked(true);

            // Flush UI tasks to ensure scheduling happened
            Shadows.shadowOf(Looper.getMainLooper()).idle();

            // Get AlarmManager service
            AlarmManager am = (AlarmManager) activity.getSystemService(Context.ALARM_SERVICE);

            // Get shadow AlarmManager
            ShadowAlarmManager shadowAm = Shadows.shadowOf(am);

            // Verify there is some scheduled alarm
            assertNotNull(getNextAlarm(shadowAm));

            // Turn OFF the switch (should call stopWaterReminder)
            reminderSwitch.setChecked(false);

            // Flush UI tasks to ensure cancellation happened
            Shadows.shadowOf(Looper.getMainLooper()).idle();

            // Read preferences again
            SharedPreferences prefs = activity.getSharedPreferences(
                    activity.getString(R.string.myprefs),
                    Context.MODE_PRIVATE
            );

            // Assert the boolean was saved as false
            assertFalse(prefs.getBoolean(KEY_WATER_REMINDER_ENABLED, true));

            // After cancel, next alarm should be null (or no scheduled alarms remain)
            assertNull(getNextAlarm(shadowAm));
        }
    }

    // -------------------------------------------------------------------------
    // TEST 3: Receiver -> posts EXACT notification title/text as in your code
    // -------------------------------------------------------------------------
    @Test
    public void receiver_onReceive_postsNotification_withExpectedTitleAndText() {
        // Get application context
        Context context = ApplicationProvider.getApplicationContext();

        // Get NotificationManager service
        NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        // Get shadow NotificationManager
        ShadowNotificationManager shadowNm = Shadows.shadowOf(nm);

        // Assert no notifications exist at start
        assertEquals(0, shadowNm.getAllNotifications().size());

        // Create the receiver instance
        WaterReminderReceiver receiver = new WaterReminderReceiver();

        // Create an intent targeting the receiver
        Intent intent = new Intent(context, WaterReminderReceiver.class);

        // Trigger onReceive manually
        receiver.onReceive(context, intent);

        // Fetch notifications posted so far
        assertEquals(1, shadowNm.getAllNotifications().size());

        // Grab the notification object
        Notification n = shadowNm.getAllNotifications().get(0);

        // Read title from extras
        String title = n.extras.getString(Notification.EXTRA_TITLE);

        // Read text from extras
        String text = n.extras.getString(Notification.EXTRA_TEXT);

        // Assert title matches your receiver code
        assertEquals("Water reminder", title);

        // Assert text matches your receiver code
        assertEquals("Time to drink water 💧", text);
    }

    // -------------------------------------------------------------------------
    // TEST 4: End-to-end: Toggle ON registers alarm + receiver posts notification
    // -------------------------------------------------------------------------
    @Test
    public void reminderToggle_on_thenReceiver_postsNotification_endToEnd() throws Exception {

        // Create static mock for RestClient
        try (MockedStatic<RestClient> restClientMock = Mockito.mockStatic(RestClient.class)) {

            // Build WaterActivity with seeded prefs and RestClient stubs
            WaterActivity activity = buildWaterActivityWithRestClientMock(restClientMock);

            // Find the reminder switch
            Switch reminderSwitch = activity.findViewById(R.id.switchWaterReminder);

            // Turn ON reminder
            reminderSwitch.setChecked(true);

            // Flush UI tasks
            Shadows.shadowOf(Looper.getMainLooper()).idle();

            // Get AlarmManager service
            AlarmManager am = (AlarmManager) activity.getSystemService(Context.ALARM_SERVICE);

            // Get shadow AlarmManager
            ShadowAlarmManager shadowAm = Shadows.shadowOf(am);

            // Assert alarm exists
            assertNotNull(getNextAlarm(shadowAm));

            // Get NotificationManager service
            NotificationManager nm = (NotificationManager) activity.getSystemService(Context.NOTIFICATION_SERVICE);

            // Get shadow NotificationManager
            ShadowNotificationManager shadowNm = Shadows.shadowOf(nm);

            // Assert no notifications exist before receiver triggers
            assertEquals(0, shadowNm.getAllNotifications().size());

            // Create receiver
            WaterReminderReceiver receiver = new WaterReminderReceiver();

            // Trigger receiver
            receiver.onReceive(activity, new Intent(activity, WaterReminderReceiver.class));

            // Assert notification exists
            assertEquals(1, shadowNm.getAllNotifications().size());

            // Grab the notification
            Notification n = shadowNm.getAllNotifications().get(0);

            // Assert title
            assertEquals("Water reminder", n.extras.getString(Notification.EXTRA_TITLE));

            // Assert text
            assertEquals("Time to drink water 💧", n.extras.getString(Notification.EXTRA_TEXT));
        }
    }
}
