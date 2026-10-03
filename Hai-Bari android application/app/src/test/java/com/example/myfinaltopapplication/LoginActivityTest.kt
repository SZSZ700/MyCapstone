// Define the package of the test.
@file:Suppress("unused")

package com.example.myfinaltopapplication
// Import Android Context for SharedPreferences access.
import android.content.Context
// Import Android Intent for checking started Activities.
import android.content.Intent
// Import Android Looper for controlling UI thread tasks.
import android.os.Looper
// Import Android widgets used inside the Activity.
import android.widget.Button
import android.widget.EditText
// Import JUnit annotations for tests.
import org.junit.Test
import org.junit.runner.RunWith
// Import assertions from JUnit.
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
// Import Mockito for static mocking of RestClient.
import org.mockito.Mockito
// Import Robolectric test runner.
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
// Import Robolectric configuration annotation.
import org.robolectric.annotation.Config
// Import Robolectric Shadows helpers.
import org.robolectric.Shadows
import org.robolectric.shadows.ShadowActivity
import org.robolectric.shadows.ShadowToast
// Import JSON object for fake getWater response.
import org.json.JSONObject
// Import Java concurrent class for CompletableFuture.
import java.util.concurrent.CompletableFuture

// -----------------------------------------------------------------------------
// LoginActivityTest
// Purpose: deep tests for LoginActivity using Robolectric + Mockito static mocks.
// I test: validation, interaction with RestClient, SharedPreferences, navigation,
// and UI feedback (Toast messages).
// -----------------------------------------------------------------------------
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class LoginActivityTest {

    // -------------------------------------------------------------------------
    // Helper method: create a fully set up LoginActivity instance.
    // -------------------------------------------------------------------------
    private fun buildActivity(): LoginActivity {
        // Build the Activity using Robolectric and call onCreate().
        // Return the created Activity instance.
        return Robolectric.buildActivity(LoginActivity::class.java)
            .setup()
            .get()
    }

    // -------------------------------------------------------------------------
    // TEST 1: Validation - empty fields -> Toast and NO RestClient.login call.
    // -------------------------------------------------------------------------
    @Test
    fun login_withEmptyFields_showsValidationToast_andDoesNotCallRestClient() {
        // Create static mock for RestClient so we can verify no calls are made.
        val restClientMock = Mockito.mockStatic(RestClient::class.java)

        restClientMock.use { restClientMock ->
            // Build the Activity under test.
            val activity = buildActivity()

            // Find username EditText in layout.
            val username: EditText = activity.findViewById(R.id.editTextText)
            // Find password EditText in layout.
            val password: EditText = activity.findViewById(R.id.editTextTextPassword)
            // Find login Button in layout.
            val loginBtn: Button = activity.findViewById(R.id.button)

            // Leave username empty.
            username.setText("")
            // Leave password empty.
            password.setText("")

            // Click the login button to trigger validation logic.
            loginBtn.performClick()

            // Run all pending UI thread tasks so Toast will be created.
            Shadows.shadowOf(Looper.getMainLooper()).idle()

            // Get the latest Toast text that was shown.
            val toastText: CharSequence? = ShadowToast.getTextOfLatestToast()

            // Assert that a Toast was indeed shown.
            assertNotNull(toastText)
            // Assert that the Toast text equals the validation message.
            assertEquals("Please fill all fields", toastText.toString())

            // Verify that RestClient.login was NEVER called.
            restClientMock.verify(
                { RestClient.login(Mockito.anyString(), Mockito.anyString()) },
                Mockito.never()
            )
        }
    }

    // -------------------------------------------------------------------------
    // TEST 2: Successful login -> save SharedPreferences, call getWater, start HomePage,
    // and show welcome Toast (deep test: logic + UI + integration with RestClient).
    // -------------------------------------------------------------------------
    @Test
    fun login_success_savesPrefs_callsGetWater_startsHomePage_andShowsWelcomeToast() {
        // Create static mock for RestClient.
        val restClientMock = Mockito.mockStatic(RestClient::class.java)

        restClientMock.use { restClientMock ->
            // Create a fake User object that RestClient.login will return.
            val fakeUser = User("john", 25, "John Doe")

            // Create a pre-completed future for login success.
            val loginFuture: CompletableFuture<User?> = CompletableFuture.completedFuture(fakeUser)

            // Create a fake JSONObject for water data.
            val waterJson = JSONObject()
            // Put todayWater = 1200 into the JSON.
            waterJson.put("todayWater", 1200)
            // Put yesterdayWater = 800 into the JSON.
            waterJson.put("yesterdayWater", 800)

            // Create a pre-completed future for getWater success.
            val waterFuture: CompletableFuture<JSONObject?> = CompletableFuture.completedFuture(waterJson)

            // Stub RestClient.login to return the loginFuture when called with "john","1234".
            restClientMock.`when`<CompletableFuture<User?>> {
                RestClient.login("john", "1234")
            }.thenReturn(loginFuture)

            // Stub RestClient.getWater to return the waterFuture when called with "john".
            restClientMock.`when`<CompletableFuture<JSONObject?>> {
                RestClient.getWater("john")
            }.thenReturn(waterFuture)

            // Build the Activity under test.
            val activity = buildActivity()

            // Find username EditText.
            val username: EditText = activity.findViewById(R.id.editTextText)
            // Find password EditText.
            val password: EditText = activity.findViewById(R.id.editTextTextPassword)
            // Find login Button.
            val loginBtn: Button = activity.findViewById(R.id.button)

            // Set username input to "john".
            username.setText("john")
            // Set password input to "1234".
            password.setText("1234")

            // Click the login button to trigger full login flow.
            loginBtn.performClick()

            // Run pending tasks on main looper (runOnUiThread callbacks and Toast).
            Shadows.shadowOf(Looper.getMainLooper()).idle()

            // Verify that RestClient.login was called exactly once with "john","1234".
            restClientMock.verify(
                { RestClient.login("john", "1234") },
                Mockito.times(1)
            )

            // Verify that RestClient.getWater was called exactly once with "john".
            restClientMock.verify(
                { RestClient.getWater("john") },
                Mockito.times(1)
            )

            // Obtain SharedPreferences used by LoginActivity.
            val prefs = activity.getSharedPreferences(activity.getString(R.string.myprefs), Context.MODE_PRIVATE)

            // Read stored current user from SharedPreferences.
            val storedUser = prefs.getString(activity.getString(R.string.currentuser), null)
            // Read stored age from SharedPreferences.
            val storedAge = prefs.getInt(activity.getString(R.string.age), -1)
            // Read stored fullName from SharedPreferences.
            val storedFullName = prefs.getString("fullName", null)
            // Read stored todayWater from SharedPreferences.
            val storedToday = prefs.getInt("todayWater", -1)
            // Read stored yesterdayWater from SharedPreferences.
            val storedYesterday = prefs.getInt("yesterdayWater", -1)

            // Assert that username was stored correctly.
            assertEquals("john", storedUser)
            // Assert that age was stored correctly.
            assertEquals(25, storedAge)
            // Assert that full name was stored correctly.
            assertEquals("John Doe", storedFullName)
            // Assert that todayWater was stored correctly.
            assertEquals(1200, storedToday)
            // Assert that yesterdayWater was stored correctly.
            assertEquals(800, storedYesterday)

            // Get the ShadowActivity to inspect started Activities.
            val shadowActivity: ShadowActivity = Shadows.shadowOf(activity)

            // Get the next started Activity Intent.
            val startedIntent: Intent? = shadowActivity.nextStartedActivity

            // Assert that an Activity was indeed started.
            assertNotNull(startedIntent)
            // Assert that the started Activity is HomePage.
            assertEquals(HomePage::class.java.name, startedIntent!!.component!!.className)

            // Get latest Toast text shown to the user.
            val toastText: CharSequence? = ShadowToast.getTextOfLatestToast()

            // Assert that a Toast was shown.
            assertNotNull(toastText)
            // Assert that the Toast message is the welcome text with full name.
            assertEquals("Welcome John Doe", toastText.toString())
        }
    }

    // -------------------------------------------------------------------------
    // TEST 3: Failed login (wrong password) -> NO navigation, NO prefs change for user,
    // and shows "Invalid username or password" Toast.
    // -------------------------------------------------------------------------
    @Test
    fun login_failure_showsErrorToast_andDoesNotNavigate() {
        // Create static mock for RestClient.
        val restClientMock = Mockito.mockStatic(RestClient::class.java)

        restClientMock.use { restClientMock ->
            // Create a pre-completed future that returns null (login failed).
            val failedFuture: CompletableFuture<User?> = CompletableFuture.completedFuture(null)

            // Stub RestClient.login to return failedFuture for these credentials.
            restClientMock.`when`<CompletableFuture<User?>> {
                RestClient.login("john", "bad pass")
            }.thenReturn(failedFuture)

            // Build the Activity under test.
            val activity = buildActivity()

            // Get SharedPreferences reference before login attempt.
            @Suppress("UNUSED_VARIABLE")
            val prefsBefore = activity.getSharedPreferences(activity.getString(R.string.myprefs), Context.MODE_PRIVATE)

            // Find username EditText.
            val username: EditText = activity.findViewById(R.id.editTextText)
            // Find password EditText.
            val password: EditText = activity.findViewById(R.id.editTextTextPassword)
            // Find login Button.
            val loginBtn: Button = activity.findViewById(R.id.button)

            // Set username to "john".
            username.setText("john")
            // Set password to wrong value "bad pass".
            password.setText("bad pass")

            // Click login button to trigger flow.
            loginBtn.performClick()

            // Run pending UI tasks.
            Shadows.shadowOf(Looper.getMainLooper()).idle()

            // Verify that RestClient.login was called once with "john","bad pass".
            restClientMock.verify(
                { RestClient.login("john", "bad pass") },
                Mockito.times(1)
            )

            // Get ShadowActivity to inspect navigation.
            val shadowActivity: ShadowActivity = Shadows.shadowOf(activity)

            // Try to get started Activity Intent.
            val startedIntent: Intent? = shadowActivity.nextStartedActivity

            // Assert that NO Activity was started.
            assertNull(startedIntent)

            // Get latest Toast text.
            val toastText: CharSequence? = ShadowToast.getTextOfLatestToast()

            // Assert that Toast was shown.
            assertNotNull(toastText)
            // Assert that the Toast message is the invalid credentials text.
            assertEquals("Invalid username or password", toastText.toString())

            // Read SharedPreferences after login attempt.
            val prefsAfter = activity.getSharedPreferences(activity.getString(R.string.myprefs), Context.MODE_PRIVATE)

            // Read current user from prefs after login.
            val storedUser = prefsAfter.getString(activity.getString(R.string.currentuser), null)

            // Assert that user was NOT stored (still null).
            assertNull(storedUser)
        }
    }

    // -------------------------------------------------------------------------
    // TEST 4: Signup button -> starts signup Activity.
    // -------------------------------------------------------------------------
    @Test
    fun signUpButton_click_startsSignupActivity() {
        // Build the Activity under test.
        val activity = buildActivity()

        // Find the signup button in the layout.
        val signUpBtn: Button = activity.findViewById(R.id.button2)

        // Click the signup button.
        signUpBtn.performClick()

        // Get ShadowActivity to inspect started Activities.
        val shadowActivity: ShadowActivity = Shadows.shadowOf(activity)

        // Get the next started Activity Intent.
        val startedIntent: Intent? = shadowActivity.nextStartedActivity

        // Assert that an Activity was indeed started.
        assertNotNull(startedIntent)

        // Assert that the started Activity is signup.
        assertEquals(signup::class.java.name, startedIntent!!.component!!.className)
    }

    // -------------------------------------------------------------------------
    // TEST 5: Successful login but getWater returns null -> water defaults to 0,
    // prefs saved, navigation still happens, and welcome Toast is shown.
    // -------------------------------------------------------------------------
    @Test
    fun login_success_getWaterReturnsNull_savesZeroWater_andStillNavigates() {
        // Create static mock for RestClient.
        val restClientMock = Mockito.mockStatic(RestClient::class.java)

        restClientMock.use { restClientMock ->
            // Fake user returned from RestClient.login.
            val fakeUser = User("john", 25, "John Doe")

            // Pre-completed future for login success.
            val loginFuture: CompletableFuture<User?> = CompletableFuture.completedFuture(fakeUser)

            // getWater will return null inside the future.
            val waterFuture: CompletableFuture<JSONObject?> = CompletableFuture.completedFuture(null)

            // Stub RestClient.login("john","1234") -> loginFuture.
            restClientMock.`when`<CompletableFuture<User?>> {
                RestClient.login("john", "1234")
            }.thenReturn(loginFuture)

            // Stub RestClient.getWater("john") -> waterFuture (null JSON).
            restClientMock.`when`<CompletableFuture<JSONObject?>> {
                RestClient.getWater("john")
            }.thenReturn(waterFuture)

            // Build the Activity under test.
            val activity = buildActivity()

            // Find username EditText.
            val username: EditText = activity.findViewById(R.id.editTextText)
            // Find password EditText.
            val password: EditText = activity.findViewById(R.id.editTextTextPassword)
            // Find login Button.
            val loginBtn: Button = activity.findViewById(R.id.button)

            // Set valid credentials.
            username.setText("john")
            password.setText("1234")

            // Click login button to trigger flow.
            loginBtn.performClick()

            // Run pending tasks on main looper (thenAccept + runOnUiThread).
            Shadows.shadowOf(Looper.getMainLooper()).idle()

            // Verify that RestClient.login was called exactly once.
            restClientMock.verify(
                { RestClient.login("john", "1234") },
                Mockito.times(1)
            )

            // Verify that RestClient.getWater was called exactly once.
            restClientMock.verify(
                { RestClient.getWater("john") },
                Mockito.times(1)
            )

            // Obtain SharedPreferences.
            val prefs = activity.getSharedPreferences(activity.getString(R.string.myprefs), Context.MODE_PRIVATE)

            // Read stored current user.
            val storedUser = prefs.getString(activity.getString(R.string.currentuser), null)
            // Read stored age.
            val storedAge = prefs.getInt(activity.getString(R.string.age), -1)
            // Read stored full name.
            val storedFullName = prefs.getString("fullName", null)

            // Read stored today water.
            val storedToday = prefs.getInt("todayWater", -1)
            // Read stored yesterday water.
            val storedYesterday = prefs.getInt("yesterdayWater", -1)

            // Assert base user data saved correctly.
            assertEquals("john", storedUser)
            assertEquals(25, storedAge)
            assertEquals("John Doe", storedFullName)

            // Because getWater returned null, activity should save 0 / 0.
            assertEquals(0, storedToday)
            assertEquals(0, storedYesterday)

            // Verify navigation to HomePage still happened.
            val shadowActivity: ShadowActivity = Shadows.shadowOf(activity)
            val startedIntent: Intent? = shadowActivity.nextStartedActivity

            // Assert that an Activity was indeed started.
            assertNotNull(startedIntent)
            // Assert that the started Activity is HomePage.
            assertEquals(HomePage::class.java.name, startedIntent!!.component!!.className)

            // Verify welcome Toast is shown.
            val toastText: CharSequence? = ShadowToast.getTextOfLatestToast()
            // Assert that Toast was shown.
            assertNotNull(toastText)
            // Assert that the Toast message is the welcome text with full name.
            assertEquals("Welcome John Doe", toastText.toString())
        }
    }
}