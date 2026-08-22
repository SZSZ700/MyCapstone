# 📱 Hi-Bari Android Application

## 📌 Overview

This directory contains the Android client of the Hi-Bari health and water tracking system.

The application allows users to register, log in, calculate BMI, define a daily water goal, track water consumption, and view weekly water statistics.

The Android client communicates with the Spring Boot backend through REST API requests over HTTPS/TLS.

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
Firebase Realtime Database
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
- Displaying water tracking and BMI information
- Trusting the configured local development certificate during local HTTPS development

The Android application does not communicate with Firebase directly.

All Firebase access is performed through the Spring Boot backend.

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
        │   │       ├── BMIActivity.java
        │   │       ├── DailyWaterGoal.java
        │   │       ├── HomePage.java
        │   │       ├── LoginActivity.java
        │   │       ├── MainActivity.java
        │   │       ├── RestClient.java
        │   │       ├── signup.java
        │   │       ├── User.java
        │   │       ├── WaterActivity.java
        │   │       ├── WaterChartActivity.java
        │   │       └── WaterReminderReceiver.java
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
        ├── debug/
        │   └── res/
        │       └── xml/
        │           └── network_security_config.xml
        │
        ├── test/
        │   └── java/
        │       └── com/example/myfinaltopapplication/
        │           ├── BMIActivityTest.java
        │           ├── DailyWaterGoalActivityTest.java
        │           ├── ExampleUnitTest.java
        │           ├── HomePageTest.java
        │           ├── LoginActivityTest.java
        │           ├── SignupActivityTest.java
        │           ├── WaterActivityTest.java
        │           └── WaterChartActivityTest.java
        │
        └── androidTest/
            └── java/
                └── com/example/myfinaltopapplication/
                    ├── ExampleInstrumentedTest.java
                    └── RestClientTest.java
```

> Generated directories such as `.gradle/`, `.idea/`, `build/`, and `app/build/` are intentionally not shown in the tree.

---

## 🧩 Main Components

### `MainActivity`

The main entry point of the Android application.

---

### `LoginActivity`

Handles user login and communicates with the backend to validate user credentials.

A successful login receives:

- JWT access token
- Username
- Age
- Full name
- BMI

The JWT is saved locally and reused for protected API requests.

---

### `signup`

Handles new user registration and sends user information to the Spring Boot server.

Signup itself is a public endpoint and does not require a JWT.

---

### `HomePage`

Displays the main application screen and provides navigation to the application's features.

---

### `WaterActivity`

Allows users to add water consumption entries and view the current daily total.

Supported drink amounts include:

- 150 ml
- 200 ml
- 1000 ml

Water update requests are protected and include the saved JWT.

---

### `DailyWaterGoal`

Allows users to define and update their daily water intake goal.

Goal-related user requests require authentication.

---

### `BMIActivity`

Calculates BMI using user data and displays the result.

User-specific BMI update requests are sent through the authenticated backend API.

---

### `WaterChartActivity`

Displays weekly water consumption data using MPAndroidChart.

---

### `WaterReminderReceiver`

Handles water reminder notifications.

---

### `RestClient`

Handles communication between the Android application and the Spring Boot backend.

Main responsibilities:

- Sending REST API requests
- Using HTTPS for backend communication
- Processing HTTP response codes and response bodies
- Creating request bodies
- Handling network errors
- Connecting the Android client to backend endpoints
- Storing the current JWT in memory
- Adding `Authorization: Bearer <JWT>` to protected requests
- Restoring authenticated communication after application restart

The local emulator backend URL is:

```text
https://10.0.2.2:8443/myapp/api/users
```

`10.0.2.2` is the Android Emulator address used to access the host development machine.

---

### `User`

Represents user-related data used by the Android application.

The Android client does not receive stored password hashes from the backend.

---

## 🔄 Application Flow

```text
Launch Application
        ↓
Restore Local Session / JWT
        ↓
Login or Signup when required
        ↓
Home Page
        ↓
Choose Feature
        ↓
Water Tracking / BMI / Daily Goal / Weekly Chart
        ↓
HTTPS REST Request
        ↓
Bearer JWT on protected endpoints
        ↓
Spring Boot Server
        ↓
Updated Data Returned over HTTPS
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
HTTPS
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
2. `WaterActivity` sends the update through `RestClient`.
3. `RestClient` creates a PATCH request.
4. The saved JWT is added to the `Authorization` header.
5. The request is sent to the Spring Boot server over HTTPS.
6. The backend validates the JWT.
7. The backend updates the user's water log.
8. The server returns the updated result.
9. The Android interface displays the new daily total.

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
Firebase Realtime Database
        ↓
HTTPS Response
        ↓
Android UI
```

---

## 🔐 Local Session Management

The application uses `SharedPreferences` to store local session-related information.

The saved session includes the JWT access token so that authenticated requests can continue after activity changes or application restarts.

The application restores the saved JWT when required and provides it to `RestClient`.

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
JWT restored
        ↓
RestClient
        ↓
Authenticated requests continue
```

The JWT is used only as an access credential for the Spring Boot API.

It is not stored in Firebase.

Sensitive backend credentials such as Firebase Admin credentials, JWT signing secrets, and server private keys are not stored inside the Android application.

---

## 🔒 HTTPS / TLS

The Android application communicates with the Spring Boot backend through HTTPS.

The local development backend is accessed through:

```text
https://10.0.2.2:8443/myapp/api/users
```

HTTPS protects sensitive information while it travels between Android and the backend, including:

- Login passwords
- JWT Bearer tokens
- User information
- Water tracking data
- BMI information
- Calories
- Daily water goals

---

## 🔑 Local Development Certificate

The Spring Boot development server uses a self-signed certificate.

Because a self-signed certificate is not automatically trusted by Android, the public development certificate is included in:

```text
app/src/main/res/raw/hibari_local.crt
```

This certificate is used only as a trust anchor.

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

The main Android network security configuration is located at:

```text
app/src/main/res/xml/network_security_config.xml
```

The application manifest references this configuration through:

```text
android:networkSecurityConfig="@xml/network_security_config"
```

The application also disables general cleartext traffic:

```text
android:usesCleartextTraffic="false"
```

The network configuration allows Android to trust:

- The local development certificate for the emulator backend
- Normal system Certificate Authorities

The real backend connection remains HTTPS-only.

The application does not use:

- Trust-all certificate managers
- Disabled hostname verification
- Insecure custom TLS bypasses

---

## 🧪 Debug Network Configuration

Instrumented REST tests use `MockWebServer`.

MockWebServer runs over HTTP by default.

For this reason, the debug build uses a separate network security configuration:

```text
app/src/debug/res/xml/network_security_config.xml
```

This debug-only configuration allows cleartext traffic only for:

```text
localhost
```

This permits instrumented tests to communicate with MockWebServer without weakening the real backend connection.

Conceptually:

```text
Normal Application
        ↓
HTTPS only
        ↓
10.0.2.2:8443


Debug Instrumented Test
        ↓
HTTP allowed only for localhost
        ↓
MockWebServer
```

The debug resource overrides the main resource only for debug builds.

---

## 📊 Data Visualization

The application uses MPAndroidChart to display water consumption information.

Visualization features include:

- Weekly water chart
- Daily water totals
- Historical water consumption
- Progress toward the daily goal

---

## 🧪 Android Testing

The Android application includes automated tests for activities, UI behavior, application logic, REST communication, JWT handling, and authenticated requests.

### Testing Technologies

- JUnit 4
- Robolectric
- Mockito
- OkHttp MockWebServer
- AndroidX Test

---

### JUnit 4

JUnit 4 is used to define test methods and assertions.

---

### Robolectric

Robolectric is used to test Android activities and Android framework behavior directly on the JVM without requiring a physical device or emulator.

It is also used to work with Android-specific behavior such as:

- Activity lifecycle
- Application context
- Toast messages
- Android runtime components

---

### Mockito

Mockito is used to create mock objects and isolate components during unit testing.

---

### OkHttp MockWebServer

MockWebServer is used to simulate backend responses and inspect outgoing requests from the Android application.

It allows tests to verify:

- HTTP methods
- Request paths
- Request bodies
- Request headers
- Bearer-token headers
- Response handling
- Error handling
- REST communication behavior

The real `RestClient` base URL uses HTTPS.

During instrumented testing, the test OkHttp client rewrites requests to the local MockWebServer using HTTP.

This is necessary because MockWebServer uses HTTP by default.

The debug-only Android network security configuration permits this HTTP traffic only for `localhost`.

---

### AndroidX Test

AndroidX Test is used for Android instrumentation testing.

---

## ✅ Test Coverage

The Android tests cover areas such as:

- Login behavior
- Signup behavior
- Activity lifecycle
- User interface logic
- BMI calculations
- Daily water goal management
- Water intake updates
- Weekly chart behavior
- REST API communication
- Request validation
- Response handling
- JWT parsing after login
- JWT storage
- Bearer-token headers on protected requests
- Network error handling
- Toast messages
- Android runtime behavior
- MockWebServer request inspection

---

## 🔐 Security

The Android application follows several security rules.

### HTTPS

All real backend communication uses HTTPS.

The application does not use HTTP for communication with the Spring Boot backend.

---

### JWT Authentication

Successful login returns a JWT.

The Android application:

- Stores the JWT locally
- Restores it when needed
- Sends it through the `Authorization` header
- Uses the format `Bearer <JWT>`
- Does not generate JWT signatures
- Does not contain the JWT signing secret

Example:

```http
Authorization: Bearer <JWT>
```

---

### Backend Secrets

The Android application does not contain:

```text
Firebase Admin credentials
JWT signing secret
Spring Boot private key
PKCS#12 server keystore
```

These remain backend-only resources.

---

### Certificate Trust

The local Android application contains:

```text
hibari_local.crt
```

This is a public certificate used for trust configuration.

It is not equivalent to the server keystore and does not contain the server's private key.

---

### Cleartext Traffic

Cleartext HTTP is disabled for the real application backend.

The only intentional cleartext exception is debug-only `localhost` communication used by MockWebServer tests.

---

## 🛠 Technologies Used

### Application

- Java
- Android SDK
- Gradle
- Kotlin DSL
- OkHttp
- SharedPreferences
- MPAndroidChart

### Security

- HTTPS / TLS
- JWT Bearer authentication
- Android Network Security Configuration
- Local development certificate trust
- Cleartext traffic restrictions

### Testing

- JUnit 4
- Robolectric
- Mockito
- OkHttp MockWebServer
- AndroidX Test

---

## ▶️ Running the Application

### Requirements

- Android Studio
- Java Development Kit
- Android SDK
- Internet connection
- Running Hi-Bari Spring Boot server
- Local development certificate configured in the Android project
- Spring Boot server running with HTTPS enabled

---

### Steps

1. Open the following directory in Android Studio:

```text
Hai-Bari android application
```

2. Allow Gradle to synchronize and download the required dependencies.

3. Make sure the Spring Boot server is running.

4. Verify that the local backend is available at:

```text
https://localhost:8443/myapp/api/users
```

5. Verify that `RestClient` uses the Android Emulator URL:

```text
https://10.0.2.2:8443/myapp/api/users
```

6. Make sure the public development certificate exists at:

```text
app/src/main/res/raw/hibari_local.crt
```

7. Make sure the Android Network Security Configuration is enabled in `AndroidManifest.xml`.

8. Select an Android emulator.

9. Run the application.

The project includes the Gradle Wrapper, so the required Gradle version can be used automatically.

---

## 🌐 Backend Dependency

The Android application requires the Spring Boot backend to perform server-side operations.

The backend project is located in:

```text
../Spring Server/
```

The main repository documentation is located in:

```text
../README.md
```

For the Android Emulator, the backend is accessed through:

```text
https://10.0.2.2:8443/myapp/api/users
```

---

## 🔐 Security Notes

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

The Android application does not contain Firebase Admin SDK credentials.

The Android application does not contain the JWT signing secret.

The Android application does not contain the Spring Boot server private key.

The public development certificate:

```text
app/src/main/res/raw/hibari_local.crt
```

may be included because it contains public certificate information only.

All privileged Firebase operations are handled by the Spring Boot backend.

---

## 🔒 Security Layers

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
→ Protects stored passwords

JWT
→ Authenticates and authorizes API requests

Android Network Security Configuration
→ Controls trusted certificates and cleartext traffic
```

---

## 🚀 Future Improvements

- Improved UI and UX
- More detailed health statistics
- Smart hydration suggestions
- Better notification scheduling
- Move JWT storage to a stronger encrypted storage mechanism
- Add refresh-token support when supported by the backend
- Add explicit logout and token invalidation support
- Remove sensitive request-body and token logging from debug networking
- Replace the local self-signed certificate with normal trusted CA validation for public deployment
- Additional charts
- Offline data support
- Improved error messages
- Expanded automated test coverage

---

## 👨‍💻 Author

Sharbel Zarzour
