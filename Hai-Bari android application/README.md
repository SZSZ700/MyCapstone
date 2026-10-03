# 📱 Hi-Bari Android Application

## 📌 Overview

This directory contains the Android client of the Hi-Bari health and water tracking system.

The Android application is implemented entirely in Kotlin.

The application allows users to:

- Register
- Log in
- Restore an authenticated session
- Calculate and store BMI
- Track calories
- Define a daily water goal
- Track water consumption
- View water history
- View weekly water statistics
- View BMI distribution statistics
- Configure water reminder notifications

The Android client communicates with the Kotlin Spring Boot backend through REST API requests over HTTPS/TLS.

The backend persists application data in MongoDB.

The Android application does not communicate with MongoDB directly.

The application also handles JWT-based authenticated sessions and sends Bearer tokens on protected API requests.

---

## 🎥 Application Demo

[![Watch the Hi-Bari application demo](https://img.youtube.com/vi/3k6u2FfhNGw/hqdefault.jpg)](https://youtube.com/shorts/3k6u2FfhNGw)

Click the image above to watch a short demonstration of the application.

---

## 🧠 Application Architecture

```text
Android Activities
        ↓
RestClient
        ↓
OkHttp
        ↓
HTTPS / TLS
        ↓
Authorization: Bearer <JWT>
        ↓
Spring Boot REST API
        ↓
JwtAuthenticationFilter
        ↓
Backend Services
        ↓
Repository Layer
        ↓
MongoDB
```

The Android application is responsible for:

- Displaying the user interface
- Collecting user input
- Managing local session data
- Storing and restoring the JWT
- Sending HTTPS requests to the backend
- Adding `Authorization: Bearer <JWT>` to protected requests
- Processing server responses
- Handling network errors
- Displaying water tracking data
- Displaying BMI information
- Displaying calories
- Displaying daily water goals
- Displaying charts and statistics
- Scheduling water reminders
- Displaying reminder notifications
- Trusting the configured local development certificate during local HTTPS development

The Android application does not communicate with MongoDB directly.

All MongoDB access is performed through the Spring Boot backend.

---

## 🗂 Project Structure

```text
Hai-Bari android application/
├── README.md
├── .gitignore
├── build.gradle.kts
├── settings.gradle.kts
├── gradle.properties
├── gradlew
├── gradlew.bat
│
├── gradle/
│   ├── libs.versions.toml
│   ├── gradle-daemon-jvm.properties
│   └── wrapper/
│       ├── gradle-wrapper.jar
│       └── gradle-wrapper.properties
│
└── app/
    ├── .gitignore
    ├── build.gradle.kts
    ├── proguard-rules.pro
    │
    └── src/
        ├── main/
        │   ├── AndroidManifest.xml
        │   │
        │   ├── java/
        │   │   └── com/example/myfinaltopapplication/
        │   │       ├── BMIActivity.kt
        │   │       ├── DailyWaterGoal.kt
        │   │       ├── HomePage.kt
        │   │       ├── LoginActivity.kt
        │   │       ├── MainActivity.kt
        │   │       ├── RestClient.kt
        │   │       ├── signup.kt
        │   │       ├── User.kt
        │   │       ├── WaterActivity.kt
        │   │       ├── WaterChartActivity.kt
        │   │       └── WaterReminderReceiver.kt
        │   │
        │   └── res/
        │       ├── drawable/
        │       │   ├── bottlemini.png
        │       │   ├── cartonmini.png
        │       │   ├── dropy.png
        │       │   ├── ic_launcher_background.xml
        │       │   ├── ic_launcher_foreground.xml
        │       │   ├── plasticmini2.png
        │       │   └── waterdropmini.png
        │       │
        │       ├── layout/
        │       │   ├── activity_bmiactivity.xml
        │       │   ├── activity_daily_water_goal.xml
        │       │   ├── activity_home_page.xml
        │       │   ├── activity_login.xml
        │       │   ├── activity_main.xml
        │       │   ├── activity_signup.xml
        │       │   ├── activity_water.xml
        │       │   └── activity_water_chart.xml
        │       │
        │       ├── mipmap-anydpi-v26/
        │       │   ├── ic_launcher.xml
        │       │   └── ic_launcher_round.xml
        │       │
        │       ├── mipmap-hdpi/
        │       ├── mipmap-mdpi/
        │       ├── mipmap-xhdpi/
        │       ├── mipmap-xxhdpi/
        │       ├── mipmap-xxxhdpi/
        │       │
        │       ├── raw/
        │       │   └── hibari_local.crt
        │       │
        │       ├── values/
        │       │   ├── arrays.xml
        │       │   ├── colors.xml
        │       │   ├── strings.xml
        │       │   └── themes.xml
        │       │
        │       ├── values-night/
        │       │   └── themes.xml
        │       │
        │       └── xml/
        │           ├── backup_rules.xml
        │           ├── data_extraction_rules.xml
        │           └── network_security_config.xml
        │
        ├── test/
        │   └── java/
        │       └── com/example/myfinaltopapplication/
        │           ├── BMIActivityTest.kt
        │           ├── DailyWaterGoalActivityTest.kt
        │           ├── HomePageTest.kt
        │           ├── LoginActivityTest.kt
        │           ├── RestClientTest.kt
        │           ├── SignupActivityTest.kt
        │           ├── WaterActivityTest.kt
        │           └── WaterChartActivityTest.kt
        │
        └── androidTest/
            └── java/
                └── com/example/myfinaltopapplication/
```

> The Android project is written entirely in Kotlin. The `java/` directory names are Android/Gradle source-set directories; the application and test source files inside them are Kotlin `.kt` files.

> The current `androidTest` package contains no test source files.

> Generated directories such as `.gradle/`, `.idea/`, `build/`, and `app/build/` are intentionally not shown in the tree.

---

## ⚙️ Android Build Configuration

The application uses:

```text
Android Gradle Plugin: 9.4.1
Gradle:                 9.6.1
compileSdk:             37
targetSdk:              37
minSdk:                 24
JVM compatibility:      17
```

The project uses Gradle Kotlin DSL:

```text
build.gradle.kts
settings.gradle.kts
```

Kotlin support is provided through the Android Gradle Plugin's built-in Kotlin support.

The application namespace is:

```text
com.example.myfinaltopapplication
```

The application ID is:

```text
com.example.myfinaltopapplication
```

---

## 🧩 Main Components

### `MainActivity.kt`

The launcher Activity and entry point of the Android application.

Responsibilities include:

- Loading the initial application screen
- Reading the locally saved JWT from `SharedPreferences`
- Restoring the JWT into `RestClient`
- Navigating the user to `LoginActivity`

Conceptually:

```text
Application starts
        ↓
MainActivity
        ↓
Read jwtToken from SharedPreferences
        ↓
Token exists?
        ↓
RestClient.setAuthToken(...)
        ↓
User can continue to login flow
```

---

### `LoginActivity.kt`

Handles user login and communicates with the backend to validate user credentials.

A successful login receives user information together with a JWT access token.

The application stores locally:

- Username
- Age
- Full name
- JWT access token
- Today's water value
- Yesterday's water value

The JWT is saved in `SharedPreferences` and reused for protected API requests.

After successful login, the application navigates to `HomePage`.

---

### `signup.kt`

Handles new user registration.

It collects:

- Username
- Password
- Full name
- Age

The registration request is sent through:

```text
RestClient.register(...)
```

Signup is a public backend endpoint and does not require a JWT.

---

### `HomePage.kt`

Acts as the main dashboard after login.

It provides navigation to:

- BMI tracking
- Water tracking
- Water charts and statistics
- Daily water goal configuration

---

### `WaterActivity.kt`

Handles daily water tracking.

Responsibilities include:

- Loading today's water value
- Loading yesterday's water value
- Adding water intake
- Updating local UI state
- Synchronizing water data with the backend
- Loading water history
- Loading daily water goal information
- Displaying water-related statistics
- Scheduling water reminder alarms
- Canceling water reminder alarms

Supported drink amounts include:

- 150 ml
- 200 ml
- 1000 ml

Water update requests are protected and include the saved JWT.

---

### `DailyWaterGoal.kt`

Allows users to view and update their daily water intake goal.

It also displays current goal progress using MPAndroidChart.

Goal-related user requests require authentication.

---

### `BMIActivity.kt`

Handles BMI and calorie-related functionality.

Responsibilities include:

- Calculating BMI
- Retrieving saved BMI
- Updating BMI on the backend
- Retrieving calories
- Updating calories
- Displaying BMI information
- Loading global BMI distribution statistics
- Displaying BMI distribution using MPAndroidChart

User-specific BMI and calorie requests are authenticated.

The global BMI distribution endpoint is public.

---

### `WaterChartActivity.kt`

Displays water consumption statistics using MPAndroidChart.

It loads:

- Recent water history
- Weekly water averages

The Activity displays this information using bar charts.

---

### `WaterReminderReceiver.kt`

A `BroadcastReceiver` responsible for displaying water reminder notifications.

The notification uses the channel:

```text
water_reminders
```

The displayed notification contains:

```text
Title: Water reminder
Text:  Time to drink water 💧
```

---

### `User.kt`

Represents user-related data used by the Android application.

The Android client does not receive MongoDB persistence details such as:

```text
ObjectId
passwordHash
transactionVersion
```

The backend exposes only the data needed by the Android REST contract.

---

## 🌐 `RestClient.kt`

`RestClient` is the Android networking component.

It is implemented as a Kotlin:

```kotlin
object RestClient
```

The local Android Emulator backend base URL is:

```text
https://10.0.2.2:8443/myapp/api/users
```

Main responsibilities include:

- Sending REST API requests
- Using OkHttp
- Using HTTPS for real backend communication
- Creating request bodies
- Processing HTTP responses
- Parsing JSON responses
- Handling network errors
- Maintaining the current JWT in memory
- Adding Bearer authentication to protected requests
- Supporting authenticated communication after the JWT is restored

The current REST methods include operations for:

- Registration
- Login
- User patch
- User deletion
- User existence checks
- BMI retrieval
- BMI updates
- Water updates
- Water retrieval
- Water history
- Weekly water averages
- Daily water goals
- BMI distribution
- Calories retrieval
- Calories updates

The asynchronous API currently uses:

```kotlin
CompletableFuture
```

Examples:

```kotlin
CompletableFuture<Boolean>
CompletableFuture<User?>
CompletableFuture<Double?>
CompletableFuture<Int?>
CompletableFuture<JSONObject?>
CompletableFuture<Map<String, Int>>
```

---

## 🔐 JWT Handling

`RestClient` keeps the current authentication token in memory.

The token can be managed through:

```kotlin
RestClient.getAuthToken()
RestClient.setAuthToken(...)
RestClient.clearAuthToken()
```

After successful login:

```text
Spring Boot
        ↓
JWT returned
        ↓
RestClient receives JWT
        ↓
LoginActivity saves jwtToken
        ↓
SharedPreferences
```

After the application starts again:

```text
MainActivity
        ↓
SharedPreferences
        ↓
Read jwtToken
        ↓
RestClient.setAuthToken(...)
```

Protected requests use:

```http
Authorization: Bearer <JWT>
```

The Android application does not:

- Generate JWT signatures
- Validate JWT signatures
- Store the JWT signing secret

Those operations remain backend responsibilities.

---

## 🔄 Application Flow

```text
Launch Application
        ↓
MainActivity
        ↓
Restore saved JWT when available
        ↓
Login / Signup
        ↓
HomePage
        ↓
Choose Feature
        ↓
Water / BMI / Calories / Daily Goal / Charts
        ↓
RestClient
        ↓
HTTPS REST Request
        ↓
Bearer JWT on protected endpoints
        ↓
Spring Boot Server
        ↓
MongoDB
        ↓
HTTPS Response
        ↓
Android UI
```

---

## 🔐 Authentication Flow

Login works as follows:

```text
User enters username and password
        ↓
LoginActivity
        ↓
RestClient
        ↓
HTTPS POST /login
        ↓
Spring Boot
        ↓
BCrypt password verification
        ↓
JWT generated
        ↓
JWT returned over HTTPS
        ↓
RestClient receives JWT
        ↓
Android stores JWT
```

After login:

```text
Protected Android Request
        ↓
RestClient
        ↓
Authorization: Bearer <JWT>
        ↓
HTTPS / TLS
        ↓
Spring Boot
        ↓
JwtAuthenticationFilter
        ↓
Protected endpoint
```

The Android application does not generate or validate the JWT itself.

JWT signing and validation are backend responsibilities.

---

## 💧 Water Tracking Flow

When the user adds water:

1. The user selects a drink amount.
2. `WaterActivity` calls `RestClient.updateWater(...)`.
3. `RestClient` creates a PATCH request.
4. The saved JWT is added to the `Authorization` header.
5. The request is sent to the Spring Boot server over HTTPS.
6. The backend validates the JWT.
7. The backend performs the water update.
8. MongoDB stores the water data.
9. The server returns the operation result.
10. The Android interface displays the updated daily total.

```text
User presses Add Water
        ↓
WaterActivity
        ↓
RestClient
        ↓
Authorization: Bearer <JWT>
        ↓
HTTPS PATCH Request
        ↓
Spring Boot Server
        ↓
MongoDB
        ↓
HTTPS Response
        ↓
Android UI
```

The Android application does not know or depend on MongoDB document structure.

Database persistence remains a backend responsibility.

---

## 🎯 Daily Water Goal Flow

The application can retrieve the current daily water goal through:

```text
GET /{username}/goal
```

and update it through:

```text
PUT /{username}/goal
```

Protected goal requests contain:

```http
Authorization: Bearer <JWT>
```

`DailyWaterGoal` combines the current goal with today's water intake to display progress.

---

## ⚖️ BMI Flow

`BMIActivity` calculates BMI locally from the user's data.

When BMI is saved:

```text
BMIActivity
        ↓
RestClient.updateBmi(...)
        ↓
PATCH /{username}/bmi
        ↓
Bearer JWT
        ↓
HTTPS
        ↓
Spring Boot
        ↓
MongoDB
```

The Activity can also retrieve the stored BMI from the backend.

---

## 🔥 Calories Flow

Calories are managed through `BMIActivity`.

The Android application can retrieve calories through:

```text
GET /{username}/calories
```

and update them through:

```text
PUT /{username}/calories
```

These are authenticated user-specific requests.

---

## 📈 Statistics

The Android application displays multiple types of statistics.

### Water History

Retrieved through:

```text
GET /{username}/waterHistoryMap
```

---

### Weekly Water Averages

Retrieved through:

```text
GET /{username}/weeklyAverages
```

---

### BMI Distribution

Retrieved through:

```text
GET /stats/bmiDistribution
```

This endpoint provides global BMI distribution information.

`BMIActivity` displays the returned distribution using MPAndroidChart.

---

## 🔔 Water Reminder Flow

`WaterActivity` allows the user to enable or disable water reminders.

The reminder state is stored locally using:

```text
waterReminderEnabled
```

When reminders are enabled:

```text
WaterActivity
        ↓
AlarmManager
        ↓
Scheduled reminder
        ↓
WaterReminderReceiver
        ↓
Android Notification
```

When reminders are disabled, the scheduled alarm is canceled.

---

## 🔐 Local Session Management

The application uses `SharedPreferences` for local session-related information.

Stored values include information such as:

- Current username
- User age
- Full name
- JWT access token
- Local water values
- Water reminder state

The JWT allows authenticated communication to continue after Activity changes or application restarts.

Conceptually:

```text
Successful Login
        ↓
JWT received
        ↓
SharedPreferences
        ↓
Application restarted
        ↓
MainActivity
        ↓
JWT restored
        ↓
RestClient
        ↓
Authenticated requests continue
```

The JWT is used only as an access credential for the Spring Boot REST API.

The Android application does not contain:

- MongoDB connection credentials
- MongoDB database credentials
- JWT signing secret
- Spring Boot private key
- PKCS#12 server keystore

---

## 🔒 HTTPS / TLS

The Android application communicates with the Spring Boot backend through HTTPS.

The local development backend is accessed through:

```text
https://10.0.2.2:8443/myapp/api/users
```

HTTPS protects information while it travels between Android and the backend, including:

- Login passwords
- JWT Bearer tokens
- User information
- Water tracking data
- BMI information
- Calories
- Daily water goals

---

## 🔑 Local Development Certificate

The Spring Boot development server uses a self-signed development certificate.

Because a self-signed certificate is not automatically trusted by Android, the public development certificate is included in:

```text
app/src/main/res/raw/hibari_local.crt
```

This file is used only as a trust anchor.

It does not contain the Spring Boot server private key.

The private key remains inside the backend's local:

```text
keystore.p12
```

and is never included in the Android application.

Conceptually:

```text
Spring Boot
    │
    │ owns private key
    ▼
keystore.p12
    │
    │ public certificate exported
    ▼
hibari_local.crt
    │
    │ bundled as public trust certificate
    ▼
Android Application
```

---

## 🛡 Android Network Security Configuration

The Android network security configuration is located at:

```text
app/src/main/res/xml/network_security_config.xml
```

The application manifest references it through:

```text
android:networkSecurityConfig="@xml/network_security_config"
```

The application also declares:

```text
android:usesCleartextTraffic="false"
```

The network security configuration contains specific rules.

### Android Emulator Backend

The real development backend uses:

```text
10.0.2.2
```

Cleartext traffic is disabled for this endpoint.

Android trusts:

- `hibari_local.crt`
- System Certificate Authorities

The backend connection therefore remains HTTPS.

---

### Localhost

The current network security configuration contains a specific cleartext exception for:

```text
localhost
```

This is used for local testing scenarios such as MockWebServer.

All other traffic falls back to the base HTTPS-only configuration.

The application does not use:

- Trust-all certificate managers
- Disabled hostname verification
- Insecure custom TLS bypasses

---

## 📊 Data Visualization

The application uses MPAndroidChart.

Visualization features include:

- Water history charts
- Weekly water averages
- Daily goal progress
- BMI distribution

The current project uses:

```text
MPAndroidChart v3.1.0
```

---

# 🧪 Android Testing

The Android project includes a local JVM test suite.

The current test classes are located under:

```text
app/src/test/java/com/example/myfinaltopapplication/
```

Current tests:

```text
BMIActivityTest.kt
DailyWaterGoalActivityTest.kt
HomePageTest.kt
LoginActivityTest.kt
RestClientTest.kt
SignupActivityTest.kt
WaterActivityTest.kt
WaterChartActivityTest.kt
```

The current `androidTest` package contains no test source classes.

Therefore, the current automated Android test suite runs without requiring:

- Android Emulator
- Physical Android device

---

## Testing Technologies

The current test stack includes:

- JUnit 4.13.2
- Robolectric 4.17
- Mockito 5.24.0
- Mockito Kotlin 6.4.0
- OkHttp MockWebServer 5.5.0
- AndroidX Test Core 1.7.0

AndroidX instrumentation dependencies also remain configured in Gradle, but there are currently no test source files under `androidTest`.

---

## JUnit 4

JUnit 4 is used for:

- Test methods
- Setup and teardown
- Assertions

Examples include:

```kotlin
@Test
@Before
@After
```

---

## Robolectric

Robolectric is used to execute Android framework behavior directly on the JVM.

This allows tests to work with:

- Activities
- Android lifecycle behavior
- Views
- Buttons
- EditText
- SharedPreferences
- Toast messages
- Intents
- AlarmManager
- Notifications
- Application context
- Android resources

without starting an emulator.

The current Robolectric version is:

```text
4.17
```

---

## Activity Lifecycle Cleanup

Activity-based tests retain their Robolectric `ActivityController` objects.

After each test, the controllers are explicitly closed.

Conceptually:

```kotlin
@After
fun tearDownActivities() {
    activityControllers.asReversed().forEach { controller ->
        controller.close()
    }

    activityControllers.clear()
}
```

This explicitly destroys Activities and releases lifecycle resources created during the test.

Activity cleanup is used in:

```text
BMIActivityTest
DailyWaterGoalActivityTest
HomePageTest
LoginActivityTest
SignupActivityTest
WaterActivityTest
WaterChartActivityTest
```

`RestClientTest` does not create an Activity and therefore does not require ActivityController cleanup.

---

## Mockito

Mockito is used to isolate dependencies and test Activity behavior without calling the real backend.

The current project uses:

```text
Mockito Core 5.24.0
Mockito Kotlin 6.4.0
```

Kotlin-aware Mockito matchers are used.

Examples:

```kotlin
any<String>()
any<Int>()
eq("john")
eq(200)
```

Static `RestClient` methods are mocked where needed using Mockito static mocking.

---

## OkHttp MockWebServer

`RestClientTest.kt` tests the real `RestClient` networking behavior using OkHttp MockWebServer.

It runs as a local JVM test together with Robolectric.

MockWebServer listens on a local random port:

```text
http://localhost:<random-port>
```

The test creates a dedicated OkHttp client.

An interceptor rewrites the destination of outgoing `RestClient` requests to the local MockWebServer.

The original request information remains available for verification, including:

- HTTP method
- Request path
- Query parameters
- Request body
- Request headers
- Authorization header

This allows `RestClient` to be tested without starting the real Spring Boot server.

The production `RestClient` base URL remains:

```text
https://10.0.2.2:8443/myapp/api/users
```

The HTTP MockWebServer connection exists only inside the isolated automated test environment.

---

## `RestClientTest`

`RestClientTest` covers REST communication behavior such as:

- Signup requests
- Login requests
- JWT extraction
- Bearer-token headers
- User patch requests
- User deletion requests
- HEAD requests
- BMI requests
- Water updates
- Water retrieval
- Water history
- Weekly averages
- Daily goals
- Calories
- BMI distribution
- HTTP response handling
- Invalid responses
- Network-related behavior

The test injects a test OkHttp client into `RestClient` and restores the original client after each test.

---

## Activity Test Coverage

The Activity tests cover areas including:

- Login validation
- Successful login
- Failed login
- JWT-related session behavior
- Signup validation
- Successful signup
- Failed signup
- Home page navigation
- BMI calculations
- BMI persistence
- Calories behavior
- Daily water goal validation
- Daily water goal updates
- Water intake
- Water statistics
- Water reminder scheduling
- Water reminder cancellation
- Notifications
- Water charts
- Weekly averages
- Navigation
- Toast messages
- SharedPreferences behavior

---

## Running Android Tests

On Windows:

```powershell
gradlew.bat :app:testDebugUnitTest --tests "com.example.myfinaltopapplication.*"
```

On Unix-like systems:

```bash
./gradlew :app:testDebugUnitTest --tests "com.example.myfinaltopapplication.*"
```

These tests run locally on the JVM.

No Android emulator is required.

---

## JVM Test Configuration

The Android Gradle configuration enables Android resources for local unit tests:

```kotlin
testOptions {
    unitTests.isIncludeAndroidResources = true
}
```

Additional JVM module access is configured for the current Robolectric/JDK environment using JVM arguments such as:

```text
--add-opens=java.base/java.lang=ALL-UNNAMED
--add-opens=java.base/java.util=ALL-UNNAMED
--add-opens=java.base/java.io=ALL-UNNAMED
--add-opens=java.base/java.net=ALL-UNNAMED
--add-opens=java.base/java.security=ALL-UNNAMED
--add-opens=java.base/java.text=ALL-UNNAMED
--add-opens=java.base/jdk.internal.access=ALL-UNNAMED
--add-opens=java.desktop/java.awt.font=ALL-UNNAMED
--add-opens=jdk.compiler/com.sun.tools.javac.api=ALL-UNNAMED
```

The configuration also enables:

```text
-XX:+EnableDynamicAgentLoading
```

These settings support the current local JVM test environment.

---

# 🔐 Security

The Android application follows several security rules.

---

## HTTPS

Real communication with the Spring Boot backend uses HTTPS.

The real backend base URL is:

```text
https://10.0.2.2:8443/myapp/api/users
```

The application does not use cleartext HTTP for the Spring Boot backend.

---

## JWT Authentication

Successful login returns a JWT.

The Android application:

- Receives the JWT from the backend
- Stores it locally
- Restores it when needed
- Keeps it in `RestClient` memory while the application is running
- Sends it through the `Authorization` header
- Uses the format `Bearer <JWT>`
- Does not generate JWT signatures
- Does not contain the JWT signing secret

Example:

```http
Authorization: Bearer <JWT>
```

---

## Backend Secrets

The Android application does not contain:

```text
MongoDB connection credentials
MongoDB database credentials
JWT signing secret
Spring Boot private key
PKCS#12 server keystore
```

These remain backend-only resources.

---

## Certificate Trust

The Android application contains:

```text
hibari_local.crt
```

This is a public development certificate used for local trust configuration.

It does not contain the server private key.

---

## Cleartext Traffic

Cleartext HTTP is disabled for the real backend connection.

The current network security configuration contains a specific localhost cleartext exception used for local development/testing traffic.

The real backend at:

```text
10.0.2.2
```

remains HTTPS-only.

---

## Security Layers

The complete application security flow can be viewed as:

```text
User Password
        ↓
HTTPS / TLS
        ↓
Spring Boot
        ↓
BCrypt verification
        ↓
JWT generated
        ↓
HTTPS / TLS
        ↓
Android stores JWT
        ↓
Bearer JWT
        ↓
HTTPS / TLS
        ↓
Spring Boot authorization
```

Each mechanism solves a different problem:

```text
HTTPS / TLS
→ Protects data in transit

BCrypt
→ Protects stored passwords on the backend

JWT
→ Authenticates and authorizes API requests

Android Network Security Configuration
→ Controls trusted certificates and cleartext traffic
```

MongoDB persistence remains entirely behind the Spring Boot API.

---

# 🛠 Technologies Used

## Application

- Kotlin
- Android SDK
- Android Gradle Plugin 9.4.1
- Gradle 9.6.1
- Gradle Kotlin DSL
- AndroidX
- OkHttp 5.5.0
- SharedPreferences
- MPAndroidChart v3.1.0
- JSON
- CompletableFuture

---

## Android Configuration

```text
compileSdk = 37
targetSdk  = 37
minSdk     = 24
JVM        = 17
```

---

## Android Libraries

The project currently uses libraries including:

- AndroidX Core KTX
- AndroidX Activity KTX
- AndroidX AppCompat
- Material Components
- ConstraintLayout
- OkHttp
- MPAndroidChart

---

## Security

- HTTPS / TLS
- JWT Bearer authentication
- Android Network Security Configuration
- Local development certificate trust
- Cleartext traffic restrictions
- SharedPreferences-based JWT persistence

---

## Testing

- JUnit 4.13.2
- Robolectric 4.17
- Mockito 5.24.0
- Mockito Kotlin 6.4.0
- OkHttp MockWebServer 5.5.0
- AndroidX Test Core 1.7.0
- Local JVM Android testing

---

# ▶️ Running the Application

## Requirements

- Android Studio
- JDK compatible with the Android/Gradle toolchain
- Android SDK
- Running Hi-Bari Spring Boot server
- Running MongoDB environment required by the backend
- Local development certificate configured in the Android project
- Spring Boot server running with HTTPS enabled
- Android Emulator or physical Android device for running the application

---

## Steps

1. Open:

```text
Hai-Bari android application
```

in Android Studio.

2. Allow Gradle to synchronize and download the required dependencies.

3. Make sure MongoDB is running for the backend.

4. Make sure the Spring Boot backend is running.

5. Verify that the local backend is available from the host machine at:

```text
https://localhost:8443/myapp/api/users
```

6. Verify that `RestClient` uses the Android Emulator backend URL:

```text
https://10.0.2.2:8443/myapp/api/users
```

7. Make sure the public development certificate exists at:

```text
app/src/main/res/raw/hibari_local.crt
```

8. Make sure `AndroidManifest.xml` references:

```text
@xml/network_security_config
```

9. Select an Android emulator or connected physical Android device.

10. Run the application.

The project includes the Gradle Wrapper, so the configured Gradle version can be used automatically.

---

# 🌐 Backend Dependency

The Android application requires the Spring Boot backend for server-side operations.

The backend project is located in:

```text
../Spring Server/
```

The main repository documentation is located in:

```text
../README.md
```

The backend is implemented in Kotlin and uses MongoDB for persistence.

For the Android Emulator, the backend is accessed through:

```text
https://10.0.2.2:8443/myapp/api/users
```

Android never accesses MongoDB directly.

---

# 🔐 Security Notes

Sensitive or machine-specific files should not be committed.

Examples include:

```text
local.properties
*.jks
*.keystore
*.p12
*.pfx
build/
app/build/
.idea/
.gradle/
```

The Android application does not contain MongoDB connection credentials.

The Android application does not contain the JWT signing secret.

The Android application does not contain the Spring Boot server private key.

The public development certificate:

```text
app/src/main/res/raw/hibari_local.crt
```

may be included because it contains public certificate information only.

All MongoDB operations are handled by the Spring Boot backend.

---

# 🚀 Future Improvements

Possible future improvements include:

- Improved UI and UX
- More detailed health statistics
- Smart hydration suggestions
- Improved notification scheduling
- Stronger encrypted JWT storage
- Refresh-token support when supported by the backend
- Explicit logout and server-side token invalidation
- Removal of sensitive request-body and token logging from debug networking
- Trusted CA-issued certificates for public deployment
- Additional charts
- Offline data support
- Improved error messages
- Additional automated test coverage

---

# 👨‍💻 Author

Sharbel Zarzour
