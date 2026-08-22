// Define package for the test class
package com.example.myfinaltopapplication;
// Android imports
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
// Java concurrency
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
// MPAndroidChart imports for checking PieChart state
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;

// -----------------------------------------------------------------------------
// BMIActivityTest
// Purpose: Deep tests for BMIActivity using Robolectric + Mockito.
// I test:
//   1) Redirect to LoginActivity if no user in SharedPreferences
//   2) Initial load of BMI + calories from server into UI
//   3) Successful BMI calculation and update (updateBmi + SharedPreferences)
//   4) Calories add / subtract / reset flows (including setCalories calls)
//   5) BMI distribution PieChart data rendering
//   6) Back button navigation to HomePage
// -----------------------------------------------------------------------------
@SuppressWarnings("SpellCheckingInspection")
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class BMIActivityTest {

    // -------------------------------------------------------------------------
    // Helper: store a username in SharedPreferences before creating the Activity
    // -------------------------------------------------------------------------
    private void putUserInPrefs(Application app) {
        // Get SharedPreferences by name as defined in strings.xml
        var prefs = app.getSharedPreferences(
                app.getString(R.string.myprefs),
                Application.MODE_PRIVATE
        );

        // Store the current user name
        var editor = prefs.edit();
        editor.putString(app.getString(R.string.currentuser), "john");
        editor.commit();
    }

    // -------------------------------------------------------------------------
    // TEST 1:
    // If there is NO current user in SharedPreferences:
    //   - Activity should show Toast "You must log in first"
    //   - Activity should start LoginActivity
    //   - Activity should finish itself.
    // -------------------------------------------------------------------------
    @Test
    public void onCreate_noUser_redirectsToLoginAndFinishes() {
        // Mock static RestClient to avoid real calls
        try (var ignored = Mockito.mockStatic(RestClient.class)) {

            // Get application instance
            @SuppressWarnings("unused") var app = RuntimeEnvironment.getApplication();

            // Build BMIActivity
            var activity = Robolectric.buildActivity(BMIActivity.class)
                    .setup()
                    .get();

            // Run pending UI tasks
            Shadows.shadowOf(Looper.getMainLooper()).idle();

            // Check latest Toast message
            CharSequence toastText = ShadowToast.getTextOfLatestToast();
            // Assert that Toast was shown
            assertNotNull(toastText);
            // Assert that Toast message equals "You must log in first"
            assertEquals("You must log in first", toastText.toString());

            // Check that LoginActivity was started
            var shadowActivity = Shadows.shadowOf(activity);
            // Get started Activity
            var started = shadowActivity.getNextStartedActivity();

            // Assert that LoginActivity was started
            assertNotNull(started);
            // Assert that LoginActivity is the target
            assertEquals(LoginActivity.class.getName(),
                    Objects.requireNonNull(started.getComponent()).getClassName());

            // Activity should be finishing
            assertTrue(activity.isFinishing());
        }
    }

    // -------------------------------------------------------------------------
    // TEST 2:
    // Initial load with server data:
    //   - getBmi returns 24.5
    //   - getCalories returns 1500
    //   - UI should show "Saved BMI: 24.50"
    //   - caloriesStatusText should reflect 1500 with "Below target"
    //   - lastBmi and lastCalories should be stored in SharedPreferences.
    // -------------------------------------------------------------------------
    @Test
    public void onCreate_withServerData_loadsBmiAndCaloriesIntoUi() {
        // Mock RestClient
        try (var restClientMock = Mockito.mockStatic(RestClient.class)) {

            // Get application
            var app = RuntimeEnvironment.getApplication();

            // Put logged-in user
            putUserInPrefs(app);

            // Stub getBmi("john") -> 24.5
            CompletableFuture<Double> bmiFuture =
                    CompletableFuture.completedFuture(24.5);
            restClientMock.when(() -> RestClient.getBmi("john"))
                    .thenReturn(bmiFuture);

            // Stub getCalories("john") -> 1500
            CompletableFuture<Integer> caloriesFuture =
                    CompletableFuture.completedFuture(1500);
            restClientMock.when(() -> RestClient.getCalories("john"))
                    .thenReturn(caloriesFuture);

            // Stub getBmiDistribution() with empty JSON to simplify
            CompletableFuture<JSONObject> distFuture =
                    CompletableFuture.completedFuture(new JSONObject());
            restClientMock.when(RestClient::getBmiDistribution)
                    .thenReturn(distFuture);

            // Build BMIActivity
            BMIActivity activity = Robolectric.buildActivity(BMIActivity.class)
                    .setup()
                    .get();

            // Run pending UI tasks
            Shadows.shadowOf(Looper.getMainLooper()).idle();

            // Find UI components
            TextView resultText = activity.findViewById(R.id.resultText);
            TextView caloriesStatusText = activity.findViewById(R.id.caloriesStatusText);

            // Check BMI text
            assertEquals("Saved BMI: 24.50", resultText.getText().toString());

            // Check calories status text
            var statusText = caloriesStatusText.getText().toString();
            // assert that statusText contains "Today calories: 1500 kcal"
            assertTrue(statusText.contains("Today calories: 1500 kcal"));

            // Check SharedPreferences stored values
            var prefs = app.getSharedPreferences(
                    // Get name from strings.xml
                    app.getString(R.string.myprefs),
                    Application.MODE_PRIVATE
            );

            // Get stored values
            var lastBmi = prefs.getFloat("lastBmi", -1f);
            var lastCalories = prefs.getInt("lastCalories", -1);

            // Assertion
            assertEquals(24.5f, lastBmi, 0.001f);
            assertEquals(1500, lastCalories);
        }
    }

    // -------------------------------------------------------------------------
    // TEST 3 (simple, robust):
    // BMI calculation success:
    //   - weight = 70, height = 170
    //   - Text shows "Your BMI is ..."
    //   - updateBmi is called once for "john" with any double
    //   - lastBmi is stored in SharedPreferences (not -1)
    //   - Toast "BMI saved successfully" is shown.
    // -------------------------------------------------------------------------
    @Test
    public void calcButton_validInput_calculatesAndSavesBmi() {
        // Mock RestClient
        //noinspection GrazieInspection
        try (var restClientMock = Mockito.mockStatic(RestClient.class)) {

            // Get application instance
            var app = RuntimeEnvironment.getApplication();

            // Put logged in user "john" into SharedPreferences
            putUserInPrefs(app);

            // Stub getBmi("john") -> null (no previous BMI)
            CompletableFuture<Double> bmiFuture =
                    CompletableFuture.completedFuture(null);
            restClientMock.when(
                    // Lambda for RestClient.getBmi call
                    () -> RestClient.getBmi("john")
            ).thenReturn(bmiFuture);

            // Stub getCalories("john") -> 0
            CompletableFuture<Integer> caloriesFuture =
                    CompletableFuture.completedFuture(0);
            restClientMock.when(
                    // Lambda for RestClient.getCalories call
                    () -> RestClient.getCalories("john")
            ).thenReturn(caloriesFuture);

            // Stub getBmiDistribution() -> empty JSON
            CompletableFuture<JSONObject> distFuture =
                    CompletableFuture.completedFuture(new JSONObject());
            restClientMock.when(
                    // Lambda for RestClient.getBmiDistribution call
                    RestClient::getBmiDistribution
            ).thenReturn(distFuture);

            // Stub updateBmi("john", any double) -> success
            restClientMock.when(
                    // Lambda for RestClient.updateBmi call
                    () -> RestClient.updateBmi(Mockito.eq("john"), Mockito.anyDouble())
            ).thenReturn(CompletableFuture.completedFuture(true));

            // Build and start BMIActivity
            var activity = Robolectric.buildActivity(BMIActivity.class)
                    .setup()
                    .get();

            // Let initial async calls finish
            Shadows.shadowOf(Looper.getMainLooper()).idle();

            // Find views
            EditText weightEdit = activity.findViewById(R.id.weightEdit);
            EditText heightEdit = activity.findViewById(R.id.heightEdit);
            Button calcButton = activity.findViewById(R.id.calcButton);
            TextView resultText = activity.findViewById(R.id.resultText);

            // Enter sample input
            weightEdit.setText("70");
            heightEdit.setText("170");

            // Click calculate
            calcButton.performClick();

            // Let async updateBmi thenAccept run
            Shadows.shadowOf(Looper.getMainLooper()).idle();

            // 1) Check that resultText starts with "Your BMI is "
            String res = resultText.getText().toString();
            assertTrue(res.startsWith("Your BMI is "));

            // 2) Verify updateBmi was called once for "john" with any double
            restClientMock.verify(
                    // Lambda for RestClient.updateBmi call
                    () -> RestClient.updateBmi(Mockito.eq("john"), Mockito.anyDouble()),
                    // Called once
                    Mockito.times(1)
            );

            // 3) Check lastBmi is stored in SharedPreferences (not default -1)
            var prefs = app.getSharedPreferences(
                    app.getString(R.string.myprefs),
                    Application.MODE_PRIVATE
            );

            // Get stored value
            var storedBmi = prefs.getFloat("lastBmi", -1f);
            // Assert that lastBmi is not default value (-1)
            assertNotEquals(-1f, storedBmi, 0.0001f);

            // 4) Check success Toast message
            CharSequence toastText = ShadowToast.getTextOfLatestToast();
            // Assert that Toast was shown
            assertNotNull(toastText);
            // Assert that Toast message equals "BMI saved successfully"
            assertEquals("BMI saved successfully", toastText.toString());
        }
    }

    // -------------------------------------------------------------------------
    // TEST 4:
    // Calories add flow:
    //   - Initial server calories = 1000
    //   - User enters "500" and clicks add
    //   - currentCalories should become 1500
    //   - caloriesStatusText updated
    //   - setCalories called once with 1500
    //   - "Calories saved" Toast is shown.
    // -------------------------------------------------------------------------
    @Test
    public void addCalories_validInput_updatesCaloriesAndSaves() {
        // Mock RestClient
        try (var restClientMock = Mockito.mockStatic(RestClient.class)) {

            // Get app
            var app = RuntimeEnvironment.getApplication();

            // Put user
            putUserInPrefs(app);

            // Stub getBmi -> null
            CompletableFuture<Double> bmiFuture =
                    CompletableFuture.completedFuture(null);
            restClientMock.when(
                    // Lambda for RestClient.getBmi call
                    () -> RestClient.getBmi("john")
            ).thenReturn(bmiFuture);

            // Stub getCalories -> 1000
            CompletableFuture<Integer> caloriesFuture =
                    CompletableFuture.completedFuture(1000);
            restClientMock.when(
                    // Lambda for RestClient.getCalories call
                    () -> RestClient.getCalories("john")
            ).thenReturn(caloriesFuture);

            // Stub getBmiDistribution -> empty JSON
            CompletableFuture<JSONObject> distFuture =
                    CompletableFuture.completedFuture(new JSONObject());
            restClientMock.when(
                    // Lambda for RestClient.getBmiDistribution call
                    RestClient::getBmiDistribution
            ).thenReturn(distFuture);

            // Stub setCalories -> true
            restClientMock.when(
                    // Lambda for RestClient.setCalories call
                    () -> RestClient.setCalories(Mockito.eq("john"), Mockito.anyInt())
            ).thenReturn(CompletableFuture.completedFuture(true));

            // Build activity
            var activity = Robolectric.buildActivity(BMIActivity.class).setup().get();

            // Run pending tasks (initial futures)
            Shadows.shadowOf(Looper.getMainLooper()).idle();

            // Find views
            EditText caloriesInput = activity.findViewById(R.id.caloriesInput);
            Button addButton = activity.findViewById(R.id.addCaloriesButton);
            TextView caloriesStatusText = activity.findViewById(R.id.caloriesStatusText);

            // Enter "500"
            caloriesInput.setText("500");

            // Click add
            addButton.performClick();

            // Run pending tasks (setCalories thenAccept)
            Shadows.shadowOf(Looper.getMainLooper()).idle();

            // Status text should mention 1500
            var statusText = caloriesStatusText.getText().toString();
            // assert that statusText contains "Today calories: 1500 kcal"
            assertTrue(statusText.contains("Today calories: 1500 kcal"));

            // Check that setCalories was called with 1500
            restClientMock.verify(
                    // Lambda for RestClient.setCalories call
                    () -> RestClient.setCalories(Mockito.eq("john"), Mockito.eq(1500)),
                    // Called once
                    Mockito.times(1)
            );

            // Latest Toast should be "Calories saved"
            CharSequence toastText = ShadowToast.getTextOfLatestToast();
            // Assert that Toast was shown
            assertNotNull(toastText);
            // Assert that Toast message equals "Calories saved"
            assertEquals("Calories saved", toastText.toString());
        }
    }

    // -------------------------------------------------------------------------
    // TEST 5:
    // Calories reset flow:
    //   - Initial server calories = 1200
    //   - User clicks reset button
    //   - currentCalories becomes 0
    //   - status text shows "Today calories: 0 kcal ..."
    //   - setCalories called with 0.
    // -------------------------------------------------------------------------
    @Test
    public void resetCalories_setsToZero_andSaves() {
        // Mock RestClient
        try (var restClientMock = Mockito.mockStatic(RestClient.class)) {

            // Get app
            var app = RuntimeEnvironment.getApplication();

            // Put user
            putUserInPrefs(app);

            // getBmi -> null
            CompletableFuture<Double> bmiFuture =
                    CompletableFuture.completedFuture(null);
            restClientMock.when(
                    // Lambda for RestClient.getBmi call
                    () -> RestClient.getBmi("john")
            ).thenReturn(bmiFuture);

            // getCalories -> 1200
            CompletableFuture<Integer> caloriesFuture =
                    CompletableFuture.completedFuture(1200);
            restClientMock.when(
                    // Lambda for RestClient.getCalories call
                    () -> RestClient.getCalories("john")
            ).thenReturn(caloriesFuture);

            // getBmiDistribution -> empty
            CompletableFuture<JSONObject> distFuture =
                    CompletableFuture.completedFuture(new JSONObject());
            restClientMock.when(
                    // Lambda for RestClient.getBmiDistribution call
                    RestClient::getBmiDistribution
            ).thenReturn(distFuture);

            // setCalories -> true
            restClientMock.when(
                    // Lambda for RestClient.setCalories call
                    () -> RestClient.setCalories(Mockito.eq("john"), Mockito.anyInt())
            ).thenReturn(CompletableFuture.completedFuture(true));

            // Build activity
            var activity = Robolectric.buildActivity(BMIActivity.class).setup().get();

            // Run pending tasks
            Shadows.shadowOf(Looper.getMainLooper()).idle();

            // Find views
            Button resetButton = activity.findViewById(R.id.resetCaloriesButton);
            TextView caloriesStatusText = activity.findViewById(R.id.caloriesStatusText);

            // Click reset
            resetButton.performClick();

            // Run pending tasks
            Shadows.shadowOf(Looper.getMainLooper()).idle();

            // Status text should show 0
            var statusText = caloriesStatusText.getText().toString();
            // assert that statusText contains "Today calories: 0 kcal"
            assertTrue(statusText.contains("Today calories: 0 kcal"));

            // setCalories should be called with 0
            restClientMock.verify(
                    // Lambda for RestClient.setCalories call
                    () -> RestClient.setCalories(Mockito.eq("john"), Mockito.eq(0)),
                    // Called once
                    Mockito.times(1)
            );
        }
    }

    // -------------------------------------------------------------------------
    // TEST 6:
    // BMI distribution PieChart:
    //   - getBmiDistribution returns JSON with some counts
    //   - PieChart should have entries for non-zero categories
    //   - DataSet entry count should match number of non-zero categories.
    // -------------------------------------------------------------------------
    @Test
    public void loadBmiDistributionChart_withData_populatesPieChart() throws Exception {
        // Mock RestClient
        //noinspection SpellCheckingInspection
        try (var restClientMock = Mockito.mockStatic(RestClient.class)) {

            // Get app
            var app = RuntimeEnvironment.getApplication();

            // Put user
            putUserInPrefs(app);

            // getBmi -> null
            CompletableFuture<Double> bmiFuture =
                    CompletableFuture.completedFuture(null);
            restClientMock.when(
                    // Lambda for RestClient.getBmi call
                    () -> RestClient.getBmi("john")
            ).thenReturn(bmiFuture);

            // getCalories -> 0
            CompletableFuture<Integer> caloriesFuture =
                    CompletableFuture.completedFuture(0);
            restClientMock.when(
                    // Lambda for RestClient.getCalories call
                    () -> RestClient.getCalories("john")
            ).thenReturn(caloriesFuture);

            // Build JSON distribution:
            // Underweight = 2, Normal = 5, Overweight = 3, Obese = 0
            var distJson = new JSONObject();
            distJson.put("Underweight", 2);
            distJson.put("Normal", 5);
            distJson.put("Overweight", 3);
            distJson.put("Obese", 0);

            // Stub getBmiDistribution -> distJson
            CompletableFuture<JSONObject> distFuture =
                    CompletableFuture.completedFuture(distJson);
            restClientMock.when(
                    // Lambda for RestClient.getBmiDistribution call
                    RestClient::getBmiDistribution
            ).thenReturn(distFuture);

            // Build activity
            var activity = Robolectric.buildActivity(BMIActivity.class).setup().get();

            // Run pending tasks (including loadBmiDistributionChart)
            Shadows.shadowOf(Looper.getMainLooper()).idle();

            // Find PieChart
            PieChart chart = activity.findViewById(R.id.bmiPieChart);
            assertNotNull(chart);

            // Get data from chart
            PieData data = chart.getData();
            // Assert that data is not null
            assertNotNull(data);

            // There should be one dataset
            assertEquals(1, data.getDataSetCount());

            // Get first dataset
            var dataSet = (PieDataSet) data.getDataSetByIndex(0);

            // Non-zero categories are: Underweight, Normal, Overweight → 3 entries
            assertEquals(3, dataSet.getEntryCount());

            // Get entries
            var e0 = dataSet.getEntryForIndex(0);
            var e1 = dataSet.getEntryForIndex(1);
            var e2 = dataSet.getEntryForIndex(2);

            // Just check Y-values and labels
            // Order is not guaranteed by JSON, but in our implementation we add
            // in Underweight, Normal, Overweight, Obese order.
            assertEquals(2f, e0.getY(), 0.001f);
            assertEquals("Underweight", e0.getLabel());

            assertEquals(5f, e1.getY(), 0.001f);
            assertEquals("Normal", e1.getLabel());

            assertEquals(3f, e2.getY(), 0.001f);
            assertEquals("Overweight", e2.getLabel());
        }
    }

    // -------------------------------------------------------------------------
    // TEST 7:
    // Back button:
    //   - Clicking backHome should start HomePage Activity.
    // -------------------------------------------------------------------------
    @Test
    public void backHomeButton_click_navigatesToHomePage() {
        // Mock RestClient
        try (var restClientMock = Mockito.mockStatic(RestClient.class)) {

            // Get application
            var app = RuntimeEnvironment.getApplication();

            // Put user
            putUserInPrefs(app);

            // getBmi -> null
            CompletableFuture<Double> bmiFuture =
                    CompletableFuture.completedFuture(null);
            restClientMock.when(
                    // Lambda for RestClient.getBmi call
                    () -> RestClient.getBmi("john")
            ).thenReturn(bmiFuture);

            // getCalories -> 0
            CompletableFuture<Integer> caloriesFuture =
                    CompletableFuture.completedFuture(0);
            restClientMock.when(
                    // Lambda for RestClient.getCalories call
                    () -> RestClient.getCalories("john")
            ).thenReturn(caloriesFuture);

            // getBmiDistribution -> empty JSON
            CompletableFuture<JSONObject> distFuture =
                    CompletableFuture.completedFuture(new JSONObject());
            restClientMock.when(
                    // Lambda for RestClient.getBmiDistribution call
                    RestClient::getBmiDistribution
            ).thenReturn(distFuture);

            // Build activity
            var activity = Robolectric.buildActivity(BMIActivity.class).setup().get();

            // Run pending tasks
            Shadows.shadowOf(Looper.getMainLooper()).idle();

            // Find back button
            ImageButton backHome = activity.findViewById(R.id.imageButton3);

            // Click back button
            backHome.performClick();

            // Inspect next started activity
            var shadowActivity = Shadows.shadowOf(activity);
            var started = shadowActivity.getNextStartedActivity();

            // Assert navigation
            // Assert that we navigated to HomePage
            assertNotNull(started);
            // Assert that the target Activity is HomePage
            assertEquals(HomePage.class.getName(),
                    Objects.requireNonNull(started.getComponent()).getClassName());
        }
    }
}

