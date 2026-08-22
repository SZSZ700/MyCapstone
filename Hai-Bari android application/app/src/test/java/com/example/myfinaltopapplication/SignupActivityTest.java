// Define the package of the test
package com.example.myfinaltopapplication;
import android.os.Looper;
import android.widget.Button;
import android.widget.EditText;
// Import JUnit annotations
import org.junit.Test;
import org.junit.runner.RunWith;
// Import static assertions
import static org.junit.Assert.*;
// Import Mockito for static mocking of RestClient
import org.mockito.MockedStatic;
import org.mockito.Mockito;
// Import Robolectric runner + config
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
// Import Robolectric Shadows helpers
import org.robolectric.Shadows;
import org.robolectric.shadows.ShadowToast;
// Import CompletableFuture for async return values
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

// -----------------------------------------------------------------------------
// SignupActivityTest
// Purpose: deep tests for signup Activity using Robolectric + Mockito.
// I test: validation, interaction with RestClient, SharedPreferences, navigation,
// and UI feedback (Toast messages).
// -----------------------------------------------------------------------------
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class SignupActivityTest {

    // -------------------------------------------------------------------------
    // Helper method: build signup Activity and run onCreate()
    // -------------------------------------------------------------------------
    private signup buildActivity() {
        // Build Activity instance with Robolectric and call lifecycle methods
        // Return created Activity instance
        return Robolectric.buildActivity(signup.class)
                .setup()
                .get();
    }

    // -------------------------------------------------------------------------
    // TEST 1: Empty fields → validation Toast + no RestClient.register call
    // -------------------------------------------------------------------------
    @Test
    public void signup_withEmptyFields_showsValidationToast_andDoesNotCallRegister() {
        // Create static mock for RestClient so we can verify there are no calls
        try (MockedStatic<RestClient> restClientMock = Mockito.mockStatic(RestClient.class)) {

            // Build Activity under test
            var activity = buildActivity();

            // Find username EditText
            EditText usernameInput = activity.findViewById(R.id.editUsername);
            // Find password EditText
            EditText passwordInput = activity.findViewById(R.id.editPassword);
            // Find full name EditText
            EditText fullNameInput = activity.findViewById(R.id.editFullName);
            // Find age EditText
            EditText ageInput = activity.findViewById(R.id.editAge);
            // Find register Button
            Button registerButton = activity.findViewById(R.id.btnRegister);

            // Leave all fields empty (validation should fail)
            usernameInput.setText("");
            passwordInput.setText("");
            fullNameInput.setText("");
            ageInput.setText("");

            // Click register button to trigger validation logic
            registerButton.performClick();

            // Run pending UI tasks so Toast will be created
            Shadows.shadowOf(Looper.getMainLooper()).idle();

            // Read the latest Toast text
            CharSequence toastText = ShadowToast.getTextOfLatestToast();

            // Assert that Toast was shown
            assertNotNull(toastText);
            // Assert that Toast message equals "fill_all_fields" string resource
            assertEquals(
                    // Get string from Activity
                    activity.getString(R.string.fill_all_fields),
                    // Get string from Toast
                    toastText.toString()
            );

            // Verify that RestClient.register was NEVER called
            restClientMock.verify(
                    // Lambda for RestClient.register call
                    () -> RestClient.register(Mockito.any(User.class)),
                    // Never called (0 times)
                    Mockito.never()
            );
        }
    }

    // -------------------------------------------------------------------------
    // TEST 2: Successful signup → RestClient.register returns true
    // -------------------------------------------------------------------------
    @Test
    public void signup_success_callsRegister_showsSuccessToast_andNavigatesToLogin() {
        // Create static mock for RestClient
        try (MockedStatic<RestClient> restClientMock = Mockito.mockStatic(RestClient.class)) {

            // Create pre-completed future with true (registration success)
            CompletableFuture<Boolean> successFuture =
                    CompletableFuture.completedFuture(true);

            // Stub RestClient.register to return successFuture for any User
            restClientMock.when(
                    // Lambda for RestClient.register call
                    () -> RestClient.register(Mockito.any(User.class)))
                    // Return successFuture
                    .thenReturn(successFuture);

            // Build Activity under test
            var activity = buildActivity();

            // Find all input fields
            EditText usernameInput = activity.findViewById(R.id.editUsername);
            EditText passwordInput = activity.findViewById(R.id.editPassword);
            EditText fullNameInput = activity.findViewById(R.id.editFullName);
            EditText ageInput = activity.findViewById(R.id.editAge);
            Button registerButton = activity.findViewById(R.id.btnRegister);

            // Fill valid data into all fields
            usernameInput.setText("john");
            passwordInput.setText("1234");
            fullNameInput.setText("John Doe");
            ageInput.setText("25");

            // Click register button to trigger full signup flow
            registerButton.performClick();

            // Run pending UI tasks (thenAccept + Toast + startActivity)
            Shadows.shadowOf(Looper.getMainLooper()).idle();

            // Verify that RestClient.register was called exactly once
            restClientMock.verify(
                    // Lambda for RestClient.register call
                    () -> RestClient.register(Mockito.any(User.class)),
                    // Called once
                    Mockito.times(1)
            );

            // Get ShadowActivity to inspect navigation
            var shadowActivity = Shadows.shadowOf(activity);

            // Read started Activity Intent (should be LoginActivity)
            var startedIntent = shadowActivity.getNextStartedActivity();

            // Assert that we navigated to another Activity
            assertNotNull(startedIntent);
            // Assert that the target Activity is LoginActivity
            assertEquals(
                    LoginActivity.class.getName(),
                    Objects.requireNonNull(startedIntent.getComponent()).getClassName()
            );

            // Read latest Toast text
            CharSequence toastText = ShadowToast.getTextOfLatestToast();

            // Assert that Toast was shown
            assertNotNull(toastText);
            // Assert that Toast message equals "sign_up_succesfully"
            assertEquals(
                    activity.getString(R.string.sign_up_succesfully),
                    toastText.toString()
            );
        }
    }

    // -------------------------------------------------------------------------
    // TEST 3: Failed signup → RestClient.register returns false
    // -------------------------------------------------------------------------
    @Test
    public void signup_failure_showsErrorToast_andDoesNotNavigate() {
        // Create static mock for RestClient
        try (MockedStatic<RestClient> restClientMock = Mockito.mockStatic(RestClient.class)) {

            // Create pre-completed future with false (registration failed)
            CompletableFuture<Boolean> failFuture =
                    CompletableFuture.completedFuture(false);

            // Stub RestClient.register to return false for any User
            restClientMock.when(() -> RestClient.register(Mockito.any(User.class)))
                    .thenReturn(failFuture);

            // Build Activity under test
            var activity = buildActivity();

            // Find all input fields
            EditText usernameInput = activity.findViewById(R.id.editUsername);
            EditText passwordInput = activity.findViewById(R.id.editPassword);
            EditText fullNameInput = activity.findViewById(R.id.editFullName);
            EditText ageInput = activity.findViewById(R.id.editAge);
            Button registerButton = activity.findViewById(R.id.btnRegister);

            // Fill valid data (so validation passes, but server fails)
            usernameInput.setText("john");
            passwordInput.setText("1234");
            fullNameInput.setText("John Doe");
            ageInput.setText("25");

            // Click register button
            registerButton.performClick();

            // Run pending UI tasks
            Shadows.shadowOf(Looper.getMainLooper()).idle();

            // Verify that RestClient.register was called once
            restClientMock.verify(
                    () -> RestClient.register(Mockito.any(User.class)),
                    Mockito.times(1)
            );

            // Get ShadowActivity to inspect navigation
            var shadowActivity = Shadows.shadowOf(activity);

            // Try to get started Activity Intent (should be null on failure)
            var startedIntent = shadowActivity.getNextStartedActivity();

            // Assert that no navigation happened
            assertNull(startedIntent);

            // Read latest Toast text
            CharSequence toastText = ShadowToast.getTextOfLatestToast();

            // Assert that Toast was shown
            assertNotNull(toastText);
            // Assert that Toast message equals "username_allready_exists"
            assertEquals(
                    // Get string from Activity
                    activity.getString(R.string.username_allready_exists),
                    // Get string from Toast
                    toastText.toString()
            );
        }
    }
}

