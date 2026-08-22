# 📱 Hi-Bari Android Application

## 📌 Overview

This directory contains the Android client of the Hi-Bari health and water tracking system.

The application allows users to register, log in, calculate BMI, define a daily water goal, track water consumption, and view weekly water statistics.

The Android client communicates with the Spring Boot backend through REST API requests.

## 🎥 Application Demo

[![Watch the Hi-Bari application demo](https://img.youtube.com/vi/3k6u2FfhNGw/hqdefault.jpg)](https://youtube.com/shorts/3k6u2FfhNGw)

Click the image above to watch a short demonstration of the application.

## 🧠 Application Architecture

```text
Android Activities
        ↓
RestClient
        ↓
OkHttp HTTP Requests
        ↓
Spring Boot REST API
        ↓
Firebase Realtime Database
```

The Android application is responsible for:

- Displaying the user interface
- Collecting user input
- Managing local session data
- Sending HTTP requests to the backend
- Processing server responses
- Displaying water tracking and BMI information

## 🗂 Project Structure

```text
Hai-Bari android application/
├── README.md
├── build.gradle.kts
├── settings.gradle.kts
├── gradle.properties
├── gradlew
├── gradlew.bat
├── gradle/
│   ├── libs.versions.toml
│   └── wrapper/
│
└── app/
    ├── build.gradle.kts
    ├── proguard-rules.pro
    │
    └── src/
        ├── main/
        │   ├── AndroidManifest.xml
        │   ├── java/
        │   │   └── com/example/myfinaltopapplication/
        │   └── res/
        │       ├── drawable/
        │       ├── layout/
        │       ├── mipmap-*/
        │       ├── values/
        │       └── xml/
        │
        ├── test/
        │   └── java/
        │
        └── androidTest/
            └── java/
```

## 🧩 Main Components

### `MainActivity`

The main entry point of the Android application.

### `LoginActivity`

Handles user login and communicates with the backend to validate user credentials.

### `signup`

Handles new user registration and sends user information to the Spring Boot server.

### `HomePage`

Displays the main application screen and provides navigation to the application's features.

### `WaterActivity`

Allows users to add water consumption entries and view the current daily total.

Supported drink amounts include:

- 150 ml
- 200 ml
- 1000 ml

### `DailyWaterGoal`

Allows users to define and update their daily water intake goal.

### `BMIActivity`

Calculates BMI using user data and displays the result.

### `WaterChartActivity`

Displays weekly water consumption data using MPAndroidChart.

### `WaterReminderReceiver`

Handles water reminder notifications.

### `RestClient`

Handles communication between the Android application and the Spring Boot backend.

Main responsibilities:

- Sending REST API requests
- Processing HTTP responses
- Managing request bodies
- Handling network errors
- Connecting the Android client to backend endpoints

### `User`

Represents user-related data transferred between the Android application and the backend.

## 🔄 Application Flow

```text
Launch Application
        ↓
Login or Signup
        ↓
Home Page
        ↓
Choose Feature
        ↓
Water Tracking / BMI / Daily Goal / Weekly Chart
        ↓
REST Request to Spring Boot Server
        ↓
Updated Data Returned to Android
```

## 💧 Water Tracking Flow

When the user adds water:

1. The user selects a drink amount.
2. `WaterActivity` sends the update through `RestClient`.
3. `RestClient` sends an HTTP request to the Spring Boot server.
4. The backend updates the user's water log.
5. The server returns the updated water data.
6. The Android interface displays the new daily total.

```text
User presses Add Water
        ↓
WaterActivity
        ↓
RestClient
        ↓
PATCH Request
        ↓
Spring Boot Server
        ↓
Updated Water Data
        ↓
Android UI
```

## 🔐 Local Session Management

The application uses `SharedPreferences` to store local session-related information.

This allows the application to remember user-related data between screens and application sessions.

Sensitive backend credentials are not stored inside the Android application.

## 📊 Data Visualization

The application uses MPAndroidChart to display water consumption information.

Visualization features include:

- Weekly water chart
- Daily water totals
- Historical water consumption
- Progress toward the daily goal

## 🧪 Android Testing

The Android application includes automated tests for activities, UI behavior, business logic, and REST communication.

### Testing Technologies

- JUnit 4
- Robolectric
- Mockito
- OkHttp MockWebServer
- AndroidX Test

### JUnit 4

JUnit 4 is used to define test methods and assertions.

### Robolectric

Robolectric is used to test Android activities and Android framework behavior directly on the JVM without requiring a physical device or emulator.

It is also used to work with Android-specific behavior such as:

- Activity lifecycle
- Application context
- Toast messages
- Android runtime components

### Mockito

Mockito is used to create mock objects and isolate components during unit testing.

### OkHttp MockWebServer

MockWebServer is used to simulate backend HTTP responses and inspect outgoing requests from the Android application.

It allows tests to verify:

- HTTP methods
- Request paths
- Request bodies
- Response handling
- Error handling
- REST communication behavior

### AndroidX Test

AndroidX Test is used for Android instrumentation testing.

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
- HTTP request validation
- HTTP response handling
- Network error handling
- Toast messages
- Android runtime behavior

## 🛠 Technologies Used

- Java
- Android SDK
- Gradle
- Kotlin DSL
- OkHttp
- SharedPreferences
- MPAndroidChart
- JUnit 4
- Robolectric
- Mockito
- OkHttp MockWebServer
- AndroidX Test

## ▶️ Running the Application

### Requirements

- Android Studio
- Java Development Kit
- Android SDK
- Internet connection
- Running Hi-Bari Spring Boot server

### Steps

1. Open the following directory in Android Studio:

```text
Hai-Bari android application
```

2. Allow Gradle to synchronize and download the required dependencies.

3. Make sure the Spring Boot server is running.

4. Make sure the backend URL used by `RestClient` is correct for your environment.

5. Select an Android emulator or a physical Android device.

6. Run the application.

The project includes the Gradle Wrapper, so the required Gradle version can be used automatically.

## 🔗 Backend Dependency

The Android application requires the Spring Boot backend to perform server-side operations.

The backend project is located in:

```text
../Spring Server/
```

The main repository documentation is located in:

```text
../README.md
```

## 🔐 Security Notes

The following files should not be committed:

```text
local.properties
*.jks
*.keystore
build/
app/build/
.idea/
.gradle/
```

The Android application does not contain Firebase Admin SDK credentials.

All secure Firebase operations are handled by the Spring Boot backend.

## 🚀 Future Improvements

- Improved UI and UX
- More detailed health statistics
- Smart hydration suggestions using AI
- Better notification scheduling
- Token-based authentication
- Additional charts
- Offline data support
- Improved error messages
- Expanded automated test coverage

## 👨‍💻 Author

Sharbel Zarzour
