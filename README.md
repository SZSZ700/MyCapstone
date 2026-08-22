# 💧 Hi-Bari – Health & Water Tracking System

## 📌 Overview

Hi-Bari is a full-stack health tracking application designed to monitor daily water intake, calculate BMI, manage daily water goals, and store user health data.

The system combines an Android mobile client, a Spring Boot REST API, and Firebase Realtime Database.

The backend has been refactored into separated controller, service, repository, configuration, DTO, security, and exception-handling layers so that HTTP handling, business logic, authentication, and Firebase access are no longer mixed together.

Communication between the Android application and the Spring Boot backend uses HTTPS/TLS.

---

## 🎥 Application Demo

Click the demo image in the repository to watch a short demonstration of the Hi-Bari Android application.

---

## 🗂 Repository Structure

The repository contains two main projects: the Android client and the Spring Boot backend.

The tree below focuses on the source files and configuration that are relevant to the application. Generated build output, IDE metadata, and Gradle caches are intentionally omitted.

```text
MyPrivatePractice/
├── README.md
│
├── Hai-Bari android application/
│   ├── README.md
│   ├── .gitignore
│   ├── build.gradle.kts
│   ├── settings.gradle.kts
│   ├── gradle.properties
│   ├── gradlew
│   ├── gradlew.bat
│   │
│   ├── gradle/
│   │   ├── libs.versions.toml
│   │   ├── gradle-daemon-jvm.properties
│   │   └── wrapper/
│   │       ├── gradle-wrapper.jar
│   │       └── gradle-wrapper.properties
│   │
│   └── app/
│       ├── .gitignore
│       ├── build.gradle.kts
│       ├── proguard-rules.pro
│       │
│       └── src/
│           ├── main/
│           │   ├── AndroidManifest.xml
│           │   │
│           │   ├── java/
│           │   │   └── com/example/myfinaltopapplication/
│           │   │       ├── BMIActivity.java
│           │   │       ├── DailyWaterGoal.java
│           │   │       ├── HomePage.java
│           │   │       ├── LoginActivity.java
│           │   │       ├── MainActivity.java
│           │   │       ├── RestClient.java
│           │   │       ├── signup.java
│           │   │       ├── User.java
│           │   │       ├── WaterActivity.java
│           │   │       ├── WaterChartActivity.java
│           │   │       └── WaterReminderReceiver.java
│           │   │
│           │   └── res/
│           │       ├── drawable/
│           │       │   ├── bottlemini.png
│           │       │   ├── cartonmini.png
│           │       │   ├── dropy.png
│           │       │   ├── ic_launcher_background.xml
│           │       │   ├── ic_launcher_foreground.xml
│           │       │   ├── plasticmini2.png
│           │       │   └── waterdropmini.png
│           │       │
│           │       ├── layout/
│           │       │   ├── activity_bmiactivity.xml
│           │       │   ├── activity_daily_water_goal.xml
│           │       │   ├── activity_home_page.xml
│           │       │   ├── activity_login.xml
│           │       │   ├── activity_main.xml
│           │       │   ├── activity_signup.xml
│           │       │   ├── activity_water.xml
│           │       │   └── activity_water_chart.xml
│           │       │
│           │       ├── mipmap-anydpi-v26/
│           │       │   ├── ic_launcher.xml
│           │       │   └── ic_launcher_round.xml
│           │       ├── mipmap-hdpi/
│           │       ├── mipmap-mdpi/
│           │       ├── mipmap-xhdpi/
│           │       ├── mipmap-xxhdpi/
│           │       ├── mipmap-xxxhdpi/
│           │       │
│           │       ├── raw/
│           │       │   └── hibari_local.crt
│           │       │
│           │       ├── values/
│           │       │   ├── arrays.xml
│           │       │   ├── colors.xml
│           │       │   ├── strings.xml
│           │       │   └── themes.xml
│           │       │
│           │       ├── values-night/
│           │       │   └── themes.xml
│           │       │
│           │       └── xml/
│           │           ├── backup_rules.xml
│           │           ├── data_extraction_rules.xml
│           │           └── network_security_config.xml
│           │
│           ├── test/
│           │   └── java/com/example/myfinaltopapplication/
│           │       ├── BMIActivityTest.java
│           │       ├── DailyWaterGoalActivityTest.java
│           │       ├── ExampleUnitTest.java
│           │       ├── HomePageTest.java
│           │       ├── LoginActivityTest.java
│           │       ├── SignupActivityTest.java
│           │       ├── WaterActivityTest.java
│           │       └── WaterChartActivityTest.java
│           │
│           └── androidTest/
│               └── java/com/example/myfinaltopapplication/
│                   ├── ExampleInstrumentedTest.java
│                   └── RestClientTest.java
│
└── Spring Server/
    ├── README.md
    ├── pom.xml
    └── src/
        ├── main/
        │   ├── java/
        │   │   └── org/example/CapstoneProject/
        │   │       ├── Application.java
        │   │       ├── config/
        │   │       │   ├── FirebaseConfiguration.java
        │   │       │   └── PasswordConfiguration.java
        │   │       ├── EnvConfiguration/
        │   │       │   └── EnvConfig.java
        │   │       ├── dto/
        │   │       │   ├── LoginRequest.java
        │   │       │   ├── LoginResponse.java
        │   │       │   ├── SignupRequest.java
        │   │       │   ├── UpdateUserRequest.java
        │   │       │   ├── UserResponse.java
        │   │       │   ├── WaterResponse.java
        │   │       │   ├── GoalResponse.java
        │   │       │   ├── GoalUpdateResponse.java
        │   │       │   └── CaloriesResponse.java
        │   │       ├── exception/
        │   │       │   └── GlobalExceptionHandler.java
        │   │       ├── model/
        │   │       │   └── User.java
        │   │       ├── repository/
        │   │       │   ├── UserRepository.java
        │   │       │   ├── WaterRepository.java
        │   │       │   └── firebase/
        │   │       │       ├── FirebaseUserRepository.java
        │   │       │       └── FirebaseWaterRepository.java
        │   │       ├── security/
        │   │       │   └── JwtAuthenticationFilter.java
        │   │       ├── service/
        │   │       │   ├── AuthenticationService.java
        │   │       │   ├── JwtService.java
        │   │       │   ├── UserService.java
        │   │       │   ├── WaterService.java
        │   │       │   ├── UserHealthService.java
        │   │       │   └── StatisticsService.java
        │   │       └── web/
        │   │           └── UsersController.java
        │   │
        │   └── resources/
        │       ├── application.properties
        │       └── keystore.p12
        │
        └── test/
            └── java/
                └── CapstoneTests/
                    ├── CapstoneServicesIntegrationTest.java
                    ├── UsersControllerIntegrationTest.java
                    └── JwtServiceTest.java
```

> `application.properties` and `keystore.p12` are local backend configuration files and are excluded from version control. The keystore contains the server private key.

> Generated directories such as `.gradle/`, `.idea/`, `build/`, and other IDE/build artifacts are intentionally not shown in this tree.

---

## 📱 Hai-Bari Android Application

Contains the Android client, user interface, session handling, water tracking, BMI calculation, charts, daily water goal management, and HTTPS communication with the backend through OkHttp.

---

## 🌐 Spring Server

Contains the Spring Boot REST API, service layer, repository abstraction, Firebase repository implementations, request/response DTOs, authentication, authorization, validation, centralized exception handling, HTTPS configuration, and transaction-safe database operations.

---

## 🧠 System Architecture

The system follows a layered client-server architecture:

```text
Android Application
        ↓
      HTTPS
        ↓
Spring Boot REST API
        ↓
JwtAuthenticationFilter
        ↓
UsersController
        ↓
Domain Services
        ↓
Repository Interfaces
        ↓
Firebase Repository Implementations
        ↓
Firebase Realtime Database
```

The backend flow is:

```text
HTTP Request over TLS
        ↓
Security Filter
        ↓
Controller
        ↓
Service
        ↓
Repository Interface
        ↓
Firebase Repository Implementation
        ↓
Firebase
```

This separation keeps Firebase-specific code out of the controller and service layers and keeps authentication logic outside the controller methods.

---

## 📱 Android Client

- Java-based Android application
- Uses OkHttp for REST communication
- Communicates with the Spring Boot backend over HTTPS
- Stores local session data and the JWT in SharedPreferences
- Restores the saved JWT when the application starts
- Sends `Authorization: Bearer <token>` on protected API requests
- Uses a local development certificate for the emulator HTTPS connection
- Uses Android Network Security Configuration to control certificate trust and cleartext traffic
- Uses MPAndroidChart for data visualization
- Sends and receives JSON through the Spring Boot REST API
- Does not contain Firebase Admin credentials
- Does not contain the Spring Boot server private key

Main application areas include:

- Login
- Signup
- Home page
- Water tracking
- BMI tracking
- Water history and charts
- Daily water goal management

---

## 🌐 Backend – Spring Boot

The backend is divided into dedicated layers.

### Controller Layer

```text
web/
└── UsersController.java
```

Responsibilities:

- Defines REST endpoints
- Reads path variables, query parameters, and request bodies
- Converts service results into HTTP responses
- Uses request and response DTOs
- Does not access Firebase directly

---

### Service Layer

```text
service/
├── AuthenticationService.java
├── JwtService.java
├── UserService.java
├── WaterService.java
├── UserHealthService.java
└── StatisticsService.java
```

Responsibilities:

- Coordinates application operations
- Contains domain-oriented service logic
- Handles authentication-related application flow
- Delegates persistence operations to repository interfaces
- Does not contain Firebase initialization code

---

### Security Layer

```text
security/
└── JwtAuthenticationFilter.java

service/
└── JwtService.java
```

Responsibilities:

- Generates signed JWT access tokens after successful login
- Validates JWT signatures and expiration
- Extracts the authenticated username from the token subject
- Protects user-specific endpoints before requests reach the controller
- Returns `401 Unauthorized` for missing or invalid tokens
- Returns `403 Forbidden` when a valid token is used for a different username
- Keeps signup, login, health, and BMI distribution endpoints public

---

### Repository Layer

```text
repository/
├── UserRepository.java
├── WaterRepository.java
└── firebase/
    ├── FirebaseUserRepository.java
    └── FirebaseWaterRepository.java
```

Responsibilities:

- Defines persistence contracts through interfaces
- Contains Firebase-specific database operations only in Firebase repository implementations
- Uses asynchronous Firebase callbacks and `CompletableFuture`
- Handles Firebase reads, writes, queries, updates, and transactions

---

### Configuration Layer

```text
config/
├── FirebaseConfiguration.java
└── PasswordConfiguration.java

EnvConfiguration/
└── EnvConfig.java
```

Responsibilities:

- Initializes Firebase Admin SDK
- Loads Firebase and JWT-related environment configuration
- Provides the shared `DatabaseReference` Spring bean
- Provides the BCrypt `PasswordEncoder` bean
- Uses constructor-based dependency injection throughout the application
- Loads the local HTTPS keystore through Spring Boot configuration

---

### DTO Layer

```text
dto/
├── LoginRequest.java
├── LoginResponse.java
├── SignupRequest.java
├── UpdateUserRequest.java
├── UserResponse.java
├── WaterResponse.java
├── GoalResponse.java
├── GoalUpdateResponse.java
└── CaloriesResponse.java
```

DTOs separate the REST API contract from the internal `User` persistence model.

Request DTOs are used for incoming JSON, while response DTOs define stable JSON structures returned to the Android client.

`LoginResponse` returns the generated JWT together with public user data.

Passwords are not exposed in user-related response DTOs.

---

## Validation and Exception Handling

Request validation uses Jakarta Bean Validation:

```text
@Valid
@NotBlank
@Min
```

Validation failures are handled centrally by:

```text
exception/
└── GlobalExceptionHandler.java
```

Example validation response:

```json
{
  "errors": {
    "userName": "Username is required",
    "password": "Password is required"
  }
}
```

This keeps validation error handling out of individual controller methods.

---

## ☁️ Firebase Realtime Database

Main user data is stored under:

```text
Users/
  userId/
    userName
    password
    fullName
    age
    bmi
    calories
    goalMl
    waterLog/
      yyyy-MM-dd/
        [total, drink1, drink2, ...]
```

For each daily water list:

- Index `0` → total water consumed that day
- Index `1..N` → individual drink entries

The value stored under `password` is a BCrypt hash rather than the user's raw plaintext password.

Firebase access is performed by the repository layer instead of directly by controllers or services.

JWT access tokens are not stored in Firebase.

---

## 🔄 Data Flow – Water Update

1. The user presses an Add Water button.
2. The Android application creates an HTTPS PATCH request.
3. The saved JWT is added to the `Authorization` header.
4. TLS protects the request while it travels between Android and Spring Boot.
5. `JwtAuthenticationFilter` validates the token and verifies that the token subject matches the username in the URL.
6. `UsersController` receives the authorized request.
7. `WaterService` handles the water-related application flow.
8. `WaterRepository` defines the required persistence operation.
9. `FirebaseWaterRepository` performs the Firebase transaction.
10. Firebase Realtime Database stores the updated water data.
11. The result travels back through the repository, service, and controller.
12. The response returns to the Android application through HTTPS.

```text
User Action
    ↓
Android Application
    ↓
HTTPS / TLS
    ↓
Authorization: Bearer <JWT>
    ↓
PATCH /api/users/{username}/water
    ↓
JwtAuthenticationFilter
    ↓
UsersController
    ↓
WaterService
    ↓
WaterRepository
    ↓
FirebaseWaterRepository
    ↓
Firebase Realtime Database
    ↓
HTTPS Response to Android
```

---

## ⚙️ Advanced Implementation

### 🔹 Repository Abstraction

Database access is defined through repository interfaces:

```text
UserRepository
WaterRepository
```

The current persistence implementation is Firebase:

```text
FirebaseUserRepository
FirebaseWaterRepository
```

This keeps the higher layers independent from Firebase-specific APIs.

---

### 🔹 Constructor Dependency Injection

Services and repositories are injected using constructors.

This makes dependencies explicit and avoids direct object creation inside controllers.

---

### 🔹 Asynchronous Backend Operations

Firebase callback-based APIs are wrapped with:

```java
CompletableFuture
```

This allows controller methods to return asynchronous results without blocking on Firebase operations.

---

### 🔹 Transaction-Based Water Updates

Water consumption is updated using Firebase transactions to prevent data loss when multiple updates happen close together.

Example:

```text
Two requests arrive at the same time

Without transaction → one update may overwrite the other ❌
With transaction    → both updates are stored safely ✅
```

---

### 🔹 Dynamic Water Data Structure

- Daily water data is stored as a dynamic list
- There is no fixed number of drink entries
- The first value stores the daily total
- Additional values store individual drink amounts

---

### 🔹 BCrypt Password Hashing

User passwords are never stored as plaintext.

Signup and password-update flows hash raw passwords with BCrypt before persistence.

Login verifies credentials using:

```java
PasswordEncoder.matches(...)
```

BCrypt protects stored passwords even if the database contents are exposed.

---

### 🔹 JWT Authentication and Authorization

Successful login returns a signed JWT access token.

The Android client stores the token locally and sends it in the `Authorization` header for protected requests:

```http
Authorization: Bearer <JWT>
```

The backend validates the token before protected controller endpoints execute and checks that the token subject matches the username in the requested URL.

The access token is stateless and is not stored in Firebase.

---

### 🔹 HTTPS / TLS Communication

Communication between the Android application and the Spring Boot backend uses HTTPS.

For local development, Spring Boot uses a PKCS#12 keystore:

```text
keystore.p12
```

The keystore contains:

```text
Server certificate
Public key
Private key
```

The private key remains on the Spring Boot server and must not be committed to version control.

The public certificate is exported separately as:

```text
hibari_local.crt
```

The Android application uses this public certificate to trust the local development server.

The local certificate contains Subject Alternative Names for the addresses used during development, including:

```text
localhost
127.0.0.1
10.0.2.2
```

`10.0.2.2` is the special Android Emulator address used to access the host development machine.

The Android production/main network configuration blocks cleartext HTTP.

A separate debug network configuration permits HTTP only for local `MockWebServer` instrumented tests.

This keeps the real application connection encrypted while still allowing isolated local HTTP mocks during automated testing.

---

### 🔹 Request and Response DTOs

The backend no longer needs to expose the internal `User` model directly through user-related REST responses.

Examples:

```text
LoginRequest
SignupRequest
UpdateUserRequest
UserResponse
WaterResponse
GoalResponse
GoalUpdateResponse
CaloriesResponse
```

This improves separation between persistence data and the public REST contract.

---

### 🔹 Centralized Validation Errors

Invalid request bodies are handled through a global exception handler instead of repeating validation response logic in every endpoint.

---

## 🧪 Software Testing

The project includes automated tests for both the Android application and the Spring Boot backend.

### 📱 Android Testing

Testing technologies include:

- JUnit 4
- Robolectric
- Mockito
- OkHttp MockWebServer
- AndroidX Test

Android tests cover:

- Activity behavior
- User interface logic
- Login and signup flows
- BMI calculations
- Daily water goal management
- Water intake updates
- Weekly chart behavior
- REST API communication
- REST request and response handling
- JWT parsing and storage after login
- Bearer-token headers on protected REST requests
- Network errors
- Toast messages
- Android runtime behavior

Robolectric is used to test Android components directly on the JVM without requiring a physical device or emulator.

MockWebServer is used to simulate backend responses and inspect outgoing HTTP requests, including `Authorization` headers.

The production Android client communicates with the Spring Boot backend over HTTPS.

MockWebServer runs locally over HTTP during instrumented tests. The debug-only Android Network Security Configuration permits cleartext traffic to `localhost` for these tests without enabling cleartext communication for the real application backend.

Mockito is used to create mock objects and isolate dependencies.

---

### 🌐 Spring Boot Testing

Testing technologies include:

- JUnit 5 / JUnit Jupiter
- Spring Boot Test
- TestRestTemplate
- Firebase integration testing
- Asynchronous operation testing

The backend includes three main test groups:

```text
CapstoneServicesIntegrationTest
UsersControllerIntegrationTest
JwtServiceTest
```

`CapstoneServicesIntegrationTest` verifies the service and repository flow against Firebase.

`UsersControllerIntegrationTest` runs Spring Boot with an embedded server on a random port and performs requests through `TestRestTemplate`.

Protected controller requests use real JWTs generated by `JwtService`.

`JwtServiceTest` verifies:

- Token generation
- Token validation
- Username extraction
- Invalid-token rejection
- Expired-token rejection
- Tampered-token rejection

Backend tests cover:

- Spring application context startup
- Service-to-repository integration
- REST controller endpoints
- Signup and login behavior
- BCrypt password storage and matching
- JWT generation and validation
- Protected requests with Bearer authentication
- User creation, retrieval, update, patch, delete, and existence checks
- HTTP GET, POST, PUT, PATCH, DELETE, and HEAD
- HTTP status codes and response bodies
- Validation behavior
- Water intake updates
- Water history
- Weekly averages
- Daily water goals
- BMI updates and distribution
- Calories
- Firebase transactions
- Asynchronous operations
- Error handling

---

## 🔐 Security

Current security-related design:

- User passwords are hashed with BCrypt before they are stored in Firebase
- Login verifies the raw password against the stored BCrypt hash
- Successful login returns a signed JWT access token
- JWTs contain the authenticated username as the token subject and have a limited lifetime
- Protected Android requests send the token through `Authorization: Bearer <token>`
- `JwtAuthenticationFilter` validates protected requests before they reach `UsersController`
- Missing, malformed, invalid, or expired tokens are rejected with `401 Unauthorized`
- A valid token used against another user's protected URL is rejected with `403 Forbidden`
- The JWT access token is not stored in Firebase
- Android-to-backend communication uses HTTPS/TLS
- Cleartext traffic is disabled for the real backend connection
- The local Android HTTPS connection trusts only the configured development certificate in addition to system certificates
- The Spring Boot private key remains inside the local PKCS#12 keystore
- The server keystore is excluded from version control
- Firebase Admin SDK is used only by the Spring Boot backend
- Firebase Admin credentials are not stored in the Android application
- Sensitive local configuration files are excluded from version control
- Firebase access is centralized in backend repository implementations
- Request validation rejects missing or invalid required input before business operations run
- User response DTOs do not expose passwords
- User passwords are not included in the `User.toString()` output, reducing accidental backend logging

Public endpoints include:

```text
GET  /api/users/health
POST /api/users/signup
POST /api/users/login
GET  /api/users/stats/bmiDistribution
```

User-specific endpoints require a valid JWT.

Sensitive files excluded from the repository include:

```text
.env
application.properties
Firebase Admin SDK JSON file
local.properties
*.jks
*.keystore
*.p12
*.pfx
```

The local public development certificate:

```text
hibari_local.crt
```

does not contain the server private key and may be included in the Android project for local development trust configuration.

> **Current limitation:** the current implementation uses a stateless access token without refresh-token rotation or server-side token revocation. Those mechanisms would be appropriate future hardening for a production deployment.

> **Development HTTPS note:** the current local HTTPS setup uses a self-signed development certificate. A production deployment should use a real domain and a certificate issued by a trusted Certificate Authority.

---

## 🛠 Technologies Used

### Android Client

- Java
- Android SDK
- Gradle
- Kotlin DSL
- OkHttp
- SharedPreferences
- Android Network Security Configuration
- MPAndroidChart

### Backend

- Java
- Spring Boot 3
- Spring Web
- Spring Security Crypto
- BCrypt
- Jakarta Bean Validation
- Maven
- REST API
- HTTPS / TLS
- PKCS#12
- Firebase Admin SDK
- Nimbus JOSE + JWT
- CompletableFuture

### Database

- Firebase Realtime Database

### Security

- BCrypt
- JWT / HS256
- HTTPS / TLS
- PKCS#12 server keystore
- Android certificate trust configuration
- Bearer authentication

### Testing

#### Android

- JUnit 4
- Robolectric
- Mockito
- OkHttp MockWebServer
- AndroidX Test

#### Backend

- JUnit 5 / JUnit Jupiter
- Spring Boot Test
- TestRestTemplate
- Firebase integration testing
- JWT unit testing

### Development Tools

- Android Studio
- IntelliJ IDEA
- Git
- GitHub
- Java Keytool

---

## 📊 Features

### 👤 User System

- Signup
- Login
- User retrieval
- Full user update
- Partial user update
- User deletion
- User existence checks
- BCrypt password hashing
- JWT-based authentication and user-specific authorization
- Local Android session management with persisted JWT
- HTTPS-protected client-server communication

### 💧 Water Tracking

- Add 150 ml
- Add 200 ml
- Add 1000 ml
- Track daily totals
- Store individual drink entries
- Store daily history
- Calculate weekly averages
- Configure a daily water goal

### ⚖️ BMI Tracking

- Calculate BMI
- Store BMI data
- Update BMI
- Retrieve BMI-related information
- Calculate global BMI distribution statistics

### 🔥 Calories

- Store calories
- Retrieve calories
- Validate allowed calorie updates

### 📈 Visualization

- Weekly water chart
- Daily water tracking
- Historical water consumption
- Daily goal progress

---

## 🌐 Main REST Endpoints

Base path:

```text
/api/users
```

Public endpoints:

```text
GET     /health
POST    /signup
POST    /login
GET     /stats/bmiDistribution
```

Protected endpoints require:

```http
Authorization: Bearer <JWT>
```

```text
GET     /
GET     /{username}
HEAD    /{username}
PUT     /{username}
PATCH   /{username}
DELETE  /{username}

PATCH   /{username}/bmi

PATCH   /{username}/water
GET     /{username}/water
GET     /{username}/waterHistoryMap
GET     /{username}/weeklyAverages

GET     /{username}/goal
PUT     /{username}/goal

GET     /{username}/calories
PUT     /{username}/calories
```

For user-specific routes, the JWT subject must match the `{username}` path value.

---

## ▶️ Running the Project

### 📱 Android Application

Open this directory in Android Studio:

```text
Hai-Bari android application
```

Allow Gradle to synchronize, make sure the Spring Boot server is running, select an Android emulator, and run the application.

For the Android emulator, the backend is accessed through:

```text
https://10.0.2.2:8443/myapp/api/users
```

`10.0.2.2` is the Android Emulator address that maps to the host computer.

The Android application trusts the local development certificate through:

```text
app/src/main/res/raw/hibari_local.crt
```

and:

```text
app/src/main/res/xml/network_security_config.xml
```

The normal application configuration disables cleartext HTTP traffic.

---

### 🌐 Spring Boot Server

Open this directory in IntelliJ IDEA:

```text
Spring Server
```

Run:

```text
Spring Server/
└── src/main/java/org/example/CapstoneProject/Application.java
```

Or from the terminal:

```bash
mvn spring-boot:run
```

The configured server context path is:

```text
/myapp
```

The local development HTTPS port is:

```text
8443
```

Example health endpoint:

```text
https://localhost:8443/myapp/api/users/health
```

Because the local development certificate is self-signed, command-line testing with `curl` may require:

```bash
curl -k https://localhost:8443/myapp/api/users/health
```

The `-k` option is used only for local command-line testing with the self-signed certificate.

The Android application does not disable certificate verification. Instead, it explicitly trusts the local development certificate through Android Network Security Configuration.

The Spring Boot HTTPS configuration uses a local PKCS#12 keystore:

```text
src/main/resources/keystore.p12
```

The keystore contains the server private key and is excluded from Git.

Sensitive Firebase, JWT, and HTTPS configuration files are required locally but are not included in the repository.

---

### 🧪 Android MockWebServer Tests

Instrumented `RestClientTest` tests use OkHttp `MockWebServer`.

MockWebServer runs locally over HTTP:

```text
http://localhost:<random-port>
```

The test OkHttp interceptor redirects production REST URLs to the local MockWebServer while preserving the original request path, query parameters, method, headers, and body.

A debug-only network security configuration exists under:

```text
app/src/debug/res/xml/network_security_config.xml
```

This configuration allows cleartext HTTP only for `localhost`, which is required by MockWebServer.

The real backend connection remains HTTPS-only.

---

### Running Backend Tests

The Spring Boot integration tests start the required Spring application context automatically.

The controller integration tests use an embedded web server with a random port, so the server does not need to be started manually before running the tests.

Run from IntelliJ IDEA or:

```bash
mvn test
```

---

## 🔑 Local HTTPS Certificate Design

The local development HTTPS setup uses two related files.

### Server Keystore

```text
keystore.p12
```

The PKCS#12 keystore contains:

```text
Certificate
Public key
Private key
```

It is used by Spring Boot to establish HTTPS connections and prove ownership of the server certificate.

The private key is sensitive and must never be distributed with the Android application or committed to Git.

### Android Public Certificate

```text
hibari_local.crt
```

This file contains the public certificate only.

It does not contain the server private key.

Android uses this certificate as a local trust anchor so that the application can establish a verified TLS connection to the self-signed development server.

The relationship is:

```text
keystore.p12
    │
    ├── Certificate
    ├── Public Key
    └── Private Key 🔐
          │
          │ certificate export
          ▼
hibari_local.crt
    │
    ├── Certificate
    └── Public Key
```

The server keeps the private key.

The Android client receives only the public certificate.

---

## 🔒 Security Layers

Different security mechanisms solve different problems in the project:

```text
BCrypt
    ↓
Protects passwords at rest

JWT
    ↓
Authenticates and authorizes API requests

HTTPS / TLS
    ↓
Protects data while it travels across the network
```

Example login flow:

```text
Raw password entered in Android
        ↓
HTTPS encrypted transport
        ↓
Spring Boot receives login request
        ↓
BCrypt verifies password
        ↓
JWT generated
        ↓
JWT returned over HTTPS
        ↓
Android stores JWT
        ↓
Protected requests send Bearer token over HTTPS
```

---

## 🚀 Future Improvements

- Add refresh-token rotation for longer-lived sessions
- Add server-side token revocation / logout support
- Move Android token storage to a stronger encrypted storage mechanism
- Remove sensitive request-body and token logging from Android debug interceptors
- Add dedicated `JwtAuthenticationFilter` tests for missing token, invalid token, and cross-user access
- Replace the local self-signed development certificate with a trusted CA-issued certificate when deploying the backend publicly
- Move production HTTPS certificate/private-key management outside the application package
- Use environment-based HTTPS keystore credentials for deployment
- More detailed health statistics
- Smart hydration suggestions
- Improved UI and UX
- Improved notification scheduling
- Cloud deployment for the Spring Boot server
- Additional charts and reports
- Offline data support
- Additional automated test coverage
- Further separation of calculation logic from persistence logic where appropriate

---

## 👨‍💻 Author

Sharbel Zarzour

---

## 🎓 Academic Context

This project was developed as a final capstone project in Software Engineering studies.

---

## 💡 Key Strengths

- Full-stack architecture
- Android mobile client
- Spring Boot REST API
- Layered backend architecture
- Controller / Service / Repository separation
- Repository abstraction
- Dedicated Firebase repository implementations
- Request and response DTOs
- Jakarta Bean Validation
- Centralized exception handling
- Constructor dependency injection
- Cloud-based real-time database
- Automated Android and backend testing
- Robolectric-based JVM testing
- Mocked HTTP testing with MockWebServer
- Spring integration testing with TestRestTemplate
- Asynchronous Firebase operations with CompletableFuture
- Transaction-safe water updates
- Dynamic water-log data structure
- Separation between client, server, and database
- Organized multi-project repository
- API compatibility preserved during backend refactoring
- BCrypt password hashing
- JWT-based authentication
- User-specific authorization through JWT subject checks
- Stateless access-token validation
- HTTPS/TLS client-server communication
- Local self-signed certificate support for Android development
- PKCS#12 server keystore
- Android Network Security Configuration
- Cleartext traffic disabled for the real backend connection
- Debug-only localhost access for MockWebServer testing
- Private server key excluded from version control
