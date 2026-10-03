// Define the package of the test.
package com.example.myfinaltopapplication
import android.os.Looper
import android.widget.Button
import android.widget.EditText
// Import JUnit annotations.
import org.junit.Test
import org.junit.runner.RunWith
// Import assertions.
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
// Import Mockito for static mocking of RestClient.
import org.mockito.Mockito
// Import Robolectric runner + config.
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
// Import Robolectric Shadows helpers.
import org.robolectric.Shadows
import org.robolectric.shadows.ShadowToast
// Import CompletableFuture for async return values.
import java.util.concurrent.CompletableFuture

// -----------------------------------------------------------------------------
// SignupActivityTest
// Purpose: deep tests for signup Activity using Robolectric + Mockito.
// I test: validation, interaction with RestClient, SharedPreferences, navigation,
// and UI feedback (Toast messages).
// -----------------------------------------------------------------------------
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SignupActivityTest {

    // -------------------------------------------------------------------------
    // Helper method: build signup Activity and run onCreate().
    // -------------------------------------------------------------------------
    private fun buildActivity(): signup {
        // Build Activity instance with Robolectric and call lifecycle methods.
        // Return created Activity instance.
        return Robolectric.buildActivity(signup::class.java)
            .setup()
            .get()
    }

    // -------------------------------------------------------------------------
    // TEST 1: Empty fields -> validation Toast + no RestClient.register call.
    // -------------------------------------------------------------------------
    @Test
    fun signup_withEmptyFields_showsValidationToast_andDoesNotCallRegister() {
        // Create static mock for RestClient so we can verify there are no calls.
        val restClientMock = Mockito.mockStatic(RestClient::class.java)

        restClientMock.use { restClientMock ->
            // Build Activity under test.
            val activity = buildActivity()

            // Find username EditText.
            val usernameInput: EditText = activity.findViewById(R.id.editUsername)
            // Find password EditText.
            val passwordInput: EditText = activity.findViewById(R.id.editPassword)
            // Find full name EditText.
            val fullNameInput: EditText = activity.findViewById(R.id.editFullName)
            // Find age EditText.
            val ageInput: EditText = activity.findViewById(R.id.editAge)
            // Find register Button.
            val registerButton: Button = activity.findViewById(R.id.btnRegister)

            // Leave all fields empty (validation should fail).
            usernameInput.setText("")
            passwordInput.setText("")
            fullNameInput.setText("")
            ageInput.setText("")

            // Click register button to trigger validation logic.
            registerButton.performClick()
            // Run pending UI tasks so Toast will be created.
            Shadows.shadowOf(Looper.getMainLooper()).idle()
            // Read the latest Toast text.
            val toastText: CharSequence? = ShadowToast.getTextOfLatestToast()

            // Assert that Toast was shown.
            assertNotNull(toastText)
            // Assert that Toast message equals "fill_all_fields" string resource.
            assertEquals(activity.getString(R.string.fill_all_fields), toastText.toString())

            // Verify that RestClient.register was NEVER called.
            restClientMock.verify(
                { RestClient.register(Mockito.any(User::class.java)) },
                Mockito.never()
            )
        }
    }

    // -------------------------------------------------------------------------
    // TEST 2: Successful signup -> RestClient.register returns true.
    // -------------------------------------------------------------------------
    @Test
    fun signup_success_callsRegister_showsSuccessToast_andNavigatesToLogin() {
        // Create static mock for RestClient.
        val restClientMock = Mockito.mockStatic(RestClient::class.java)

        restClientMock.use { restClientMock ->
            // Create pre-completed future with true (registration success).
            val successFuture: CompletableFuture<Boolean> = CompletableFuture.completedFuture(true)

            // Stub RestClient.register to return successFuture for any User.
            restClientMock.`when`<CompletableFuture<Boolean>> {
                RestClient.register(Mockito.any(User::class.java))
            }.thenReturn(successFuture)

            // Build Activity under test.
            val activity = buildActivity()

            // Find username input.
            val usernameInput: EditText = activity.findViewById(R.id.editUsername)
            // Find password input.
            val passwordInput: EditText = activity.findViewById(R.id.editPassword)
            // Find full name input.
            val fullNameInput: EditText = activity.findViewById(R.id.editFullName)
            // Find age input.
            val ageInput: EditText = activity.findViewById(R.id.editAge)
            // Find register button.
            val registerButton: Button = activity.findViewById(R.id.btnRegister)

            // Fill valid username.
            usernameInput.setText("john")
            // Fill valid password.
            passwordInput.setText("1234")
            // Fill valid full name.
            fullNameInput.setText("John Doe")
            // Fill valid age.
            ageInput.setText("25")

            // Click register button to trigger full signup flow.
            registerButton.performClick()
            // Run pending UI tasks (thenAccept + Toast + startActivity).
            Shadows.shadowOf(Looper.getMainLooper()).idle()

            // Verify that RestClient.register was called exactly once.
            restClientMock.verify(
                { RestClient.register(Mockito.any(User::class.java)) },
                Mockito.times(1)
            )

            // Get ShadowActivity to inspect navigation.
            val shadowActivity = Shadows.shadowOf(activity)
            // Read started Activity Intent (should be LoginActivity).
            val startedIntent = shadowActivity.nextStartedActivity

            // Assert that we navigated to another Activity.
            assertNotNull(startedIntent)
            // Assert that the target Activity is LoginActivity.
            assertEquals(LoginActivity::class.java.name, startedIntent.component!!.className)

            // Read latest Toast text.
            val toastText: CharSequence? = ShadowToast.getTextOfLatestToast()

            // Assert that Toast was shown.
            assertNotNull(toastText)
            // Assert that Toast message equals "sign_up_succesfully".
            assertEquals(activity.getString(R.string.sign_up_succesfully), toastText.toString())
        }
    }

    // -------------------------------------------------------------------------
    // TEST 3: Failed signup -> RestClient.register returns false.
    // -------------------------------------------------------------------------
    @Test
    fun signup_failure_showsErrorToast_andDoesNotNavigate() {
        // Create static mock for RestClient.
        val restClientMock = Mockito.mockStatic(RestClient::class.java)

        restClientMock.use { restClientMock ->
            // Create pre-completed future with false (registration failed).
            val failFuture: CompletableFuture<Boolean> = CompletableFuture.completedFuture(false)

            // Stub RestClient.register to return false for any User.
            restClientMock.`when`<CompletableFuture<Boolean>> {
                RestClient.register(Mockito.any(User::class.java))
            }.thenReturn(failFuture)

            // Build Activity under test.
            val activity = buildActivity()

            // Find username input.
            val usernameInput: EditText = activity.findViewById(R.id.editUsername)
            // Find password input.
            val passwordInput: EditText = activity.findViewById(R.id.editPassword)
            // Find full name input.
            val fullNameInput: EditText = activity.findViewById(R.id.editFullName)
            // Find age input.
            val ageInput: EditText = activity.findViewById(R.id.editAge)
            // Find register button.
            val registerButton: Button = activity.findViewById(R.id.btnRegister)

            // Fill valid username.
            usernameInput.setText("john")
            // Fill valid password.
            passwordInput.setText("1234")
            // Fill valid full name.
            fullNameInput.setText("John Doe")
            // Fill valid age.
            ageInput.setText("25")

            // Click register button.
            registerButton.performClick()
            // Run pending UI tasks.
            Shadows.shadowOf(Looper.getMainLooper()).idle()

            // Verify that RestClient.register was called once.
            restClientMock.verify(
                { RestClient.register(Mockito.any(User::class.java)) },
                Mockito.times(1)
            )

            // Get ShadowActivity to inspect navigation.
            val shadowActivity = Shadows.shadowOf(activity)

            // Try to get started Activity Intent (should be null on failure).
            val startedIntent = shadowActivity.nextStartedActivity

            // Assert that no navigation happened.
            assertNull(startedIntent)

            // Read latest Toast text.
            val toastText: CharSequence? = ShadowToast.getTextOfLatestToast()

            // Assert that Toast was shown.
            assertNotNull(toastText)
            // Assert that Toast message equals "username_allready_exists".
            assertEquals(activity.getString(R.string.username_allready_exists), toastText.toString())
        }
    }
}