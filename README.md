# 💧 Hi-Bari – Health & Water Tracking System

## 📌 Overview

Hi-Bari is a full-stack health tracking application designed to monitor daily water intake, calculate BMI, manage daily water goals, track calories, and store user health data.

The system combines:

- A Kotlin Android mobile application
- A Kotlin Spring Boot REST API
- MongoDB

The backend follows a layered architecture with separated controller, service, repository, configuration, DTO, security, and exception-handling components.

Android communicates only with the Spring Boot REST API.

The Android application does not access MongoDB directly.

Communication between the Android application and the Spring Boot backend uses HTTPS/TLS.

---

## 🎥 Application Demo

Click the demo image in the Android project README to watch a short demonstration of the Hi-Bari application.

---

## 🗂 Repository Structure

The repository contains two main projects:

- Android client
- Spring Boot backend

The project root is:

```text
MyCapstone/
```

The tree below focuses on application source files, tests, and relevant configuration.

Generated output such as `.gradle/`, `.idea/`, `build/`, and `target/` is intentionally omitted.

```text
MyCapstone/
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
│           │   │       ├── BMIActivity.kt
│           │   │       ├── DailyWaterGoal.kt
│           │   │       ├── HomePage.kt
│           │   │       ├── LoginActivity.kt
│           │   │       ├── MainActivity.kt
│           │   │       ├── RestClient.kt
│           │   │       ├── signup.kt
│           │   │       ├── User.kt
│           │   │       ├── WaterActivity.kt
│           │   │       ├── WaterChartActivity.kt
│           │   │       └── WaterReminderReceiver.kt
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
│           │   └── java/
│           │       └── com/example/myfinaltopapplication/
│           │           ├── BMIActivityTest.kt
│           │           ├── DailyWaterGoalActivityTest.kt
│           │           ├── HomePageTest.kt
│           │           ├── LoginActivityTest.kt
│           │           ├── RestClientTest.kt
│           │           ├── SignupActivityTest.kt
│           │           ├── WaterActivityTest.kt
│           │           └── WaterChartActivityTest.kt
│           │
│           └── androidTest/
│               └── java/
│                   └── (currently empty)
│
└── Spring Server/
    ├── README.md
    ├── .gitignore
    ├── pom.xml
    │
    └── src/
        ├── main/
        │   ├── java/
        │   │   └── org/example/CapstoneProject/
        │   │       ├── Application.kt
        │   │       │
        │   │       ├── config/
        │   │       │   ├── MongoConfiguration.kt
        │   │       │   └── PasswordConfiguration.kt
        │   │       │
        │   │       ├── EnvConfiguration/
        │   │       │   └── EnvConfig.kt
        │   │       │
        │   │       ├── dto/
        │   │       │   ├── CaloriesResponse.kt
        │   │       │   ├── GoalResponse.kt
        │   │       │   ├── GoalUpdateResponse.kt
        │   │       │   ├── LoginRequest.kt
        │   │       │   ├── LoginResponse.kt
        │   │       │   ├── SignupRequest.kt
        │   │       │   ├── UserResponse.kt
        │   │       │   └── WaterResponse.kt
        │   │       │
        │   │       ├── exception/
        │   │       │   └── GlobalExceptionHandler.kt
        │   │       │
        │   │       ├── model/
        │   │       │   └── User.kt
        │   │       │
        │   │       ├── repository/
        │   │       │   ├── CaloriesRepository.kt
        │   │       │   ├── UserRepository.kt
        │   │       │   ├── WaterRepository.kt
        │   │       │   │
        │   │       │   └── mongo/
        │   │       │       ├── MongoCaloriesRepository.kt
        │   │       │       ├── MongoUserRepository.kt
        │   │       │       └── MongoWaterRepository.kt
        │   │       │
        │   │       ├── security/
        │   │       │   └── JwtAuthenticationFilter.kt
        │   │       │
        │   │       ├── service/
        │   │       │   ├── AuthenticationService.kt
        │   │       │   ├── JwtService.kt
        │   │       │   ├── StatisticsService.kt
        │   │       │   ├── UserHealthService.kt
        │   │       │   ├── UserService.kt
        │   │       │   └── WaterService.kt
        │   │       │
        │   │       └── web/
        │   │           └── UsersController.kt
        │   │
        │   └── resources/
        │       ├── application.properties
        │       └── keystore.p12
        │
        └── test/
            └── java/
                └── CapstoneTests/
                    ├── CapstoneServicesIntegrationTest.kt
                    ├── JwtServiceTest.kt
                    └── UsersControllerIntegrationTest.kt
```

> Both projects are written in Kotlin. The directory names `src/main/java` and `src/test/java` are source-set directory names only. There are no `.java` source files in either project.

> Local or generated directories such as `.gradle/`, `.idea/`, `build/`, and `target/` are intentionally omitted from the structure above.

---

## 📱 Hai-Bari Android Application

The Android project contains the mobile client.

It is responsible for:

- User interface
- Signup
- Login
- Local session handling
- JWT persistence
- BMI calculation and tracking
- Calories tracking
- Daily water tracking
- Daily water goals
- Water history
- Weekly water statistics
- Charts
- Water reminder notifications
- HTTPS REST communication

The Android project is implemented entirely in Kotlin.

---

## 🌐 Spring Server

The Spring Server is a Kotlin Spring Boot backend.

It is responsible for:

- REST API endpoints
- Authentication
- Authorization
- JWT generation and validation
- BCrypt password hashing
- Validation
- Business logic
- MongoDB persistence
- Transactions
- Repository abstraction
- HTTPS/TLS
- Centralized exception handling

The server is also implemented entirely in Kotlin.

---

## 🧠 System Architecture

The system follows a layered client-server architecture:

```text
Android Application
        ↓
     HTTPS/TLS
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
MongoDB Repository Implementations
        ↓
MongoDB
```

Backend request flow:

```text
HTTP Request
        ↓
TLS
        ↓
JwtAuthenticationFilter
        ↓
UsersController
        ↓
Service Layer
        ↓
Repository Interface
        ↓
MongoDB Repository
        ↓
MongoDB
```

The Android application never communicates directly with MongoDB.

---

# 📱 Android Client

## Technology

The Android application uses:

- Kotlin
- Android SDK
- Android Gradle Plugin 9.4.1
- Gradle 9.6.1
- Gradle Kotlin DSL
- AndroidX
- OkHttp
- SharedPreferences
- MPAndroidChart

Android configuration:

```text
compileSdk = 37
targetSdk  = 37
minSdk     = 24
```

The project is configured for JVM 17 compatibility.

Kotlin support is provided through the Android Gradle Plugin's built-in Kotlin support.

---

## Android Application Components

Main Kotlin classes:

```text
BMIActivity.kt
DailyWaterGoal.kt
HomePage.kt
LoginActivity.kt
MainActivity.kt
RestClient.kt
signup.kt
User.kt
WaterActivity.kt
WaterChartActivity.kt
WaterReminderReceiver.kt
```

---

## REST Communication

`RestClient.kt` is responsible for communication with the backend.

The Android emulator backend URL is:

```text
https://10.0.2.2:8443/myapp/api/users
```

The Android application uses OkHttp.

Protected requests send:

```http
Authorization: Bearer <JWT>
```

The JWT returned by a successful login is stored by the application and reused for protected requests.

---

## HTTPS and Certificate Trust

The Spring Boot backend uses HTTPS locally.

Android trusts the local development certificate:

```text
app/src/main/res/raw/hibari_local.crt
```

Network Security Configuration:

```text
app/src/main/res/xml/network_security_config.xml
```

The configuration keeps the real emulator backend connection on HTTPS:

```text
10.0.2.2
```

The locally trusted certificate is used for this endpoint.

Cleartext traffic is disabled by default.

The current network configuration permits HTTP for:

```text
localhost
```

This allows local MockWebServer communication during JVM REST tests.

---

# 🌐 Backend – Spring Boot

The backend is implemented in Kotlin.

## Controller Layer

```text
web/
└── UsersController.kt
```

Responsibilities:

- Defines REST endpoints
- Reads path variables
- Reads request parameters
- Reads request bodies
- Calls service-layer operations
- Builds HTTP responses
- Uses DTOs
- Does not access MongoDB directly

---

## Service Layer

```text
service/
├── AuthenticationService.kt
├── JwtService.kt
├── StatisticsService.kt
├── UserHealthService.kt
├── UserService.kt
└── WaterService.kt
```

Responsibilities include:

- Authentication flow
- User operations
- User input validation and password preparation
- Water amount validation
- Application date selection for daily water, goal, and calorie operations
- Daily water totals and yesterday totals
- Water history completion with zero values for missing days
- Weekly water bucket and average calculations
- Daily water-goal defaults and validation
- Calories validation and application-level fallback behavior
- BMI operations
- BMI distribution classification
- Statistics
- JWT operations

Business rules and application-level decisions are kept in the service layer.

`WaterService` calculates water totals, history results, weekly averages, default goal behavior, and goal validation from persistence data returned by `WaterRepository`.

`UserHealthService` uses `UserRepository` for BMI persistence and `CaloriesRepository` for daily calorie persistence.

`StatisticsService` retrieves stored BMI values from `UserRepository` and applies the BMI category classification in the service layer.

The service layer depends on repository interfaces rather than MongoDB-specific repository classes.

---

## Security Layer

```text
security/
└── JwtAuthenticationFilter.kt

service/
└── JwtService.kt
```

The security layer handles:

- JWT generation
- JWT signature validation
- JWT expiration validation
- Username extraction from the JWT subject
- Bearer authentication
- User-specific authorization

Protected requests use:

```http
Authorization: Bearer <JWT>
```

Missing or invalid authentication is rejected.

A valid token cannot be used to access another user's protected URL.

---

## Repository Layer

Repository interfaces:

```text
repository/
├── CaloriesRepository.kt
├── UserRepository.kt
└── WaterRepository.kt
```

MongoDB implementations:

```text
repository/mongo/
├── MongoCaloriesRepository.kt
├── MongoUserRepository.kt
└── MongoWaterRepository.kt
```

Repository responsibilities are separated by persistence concern:

- `UserRepository` handles user persistence, user BMI updates, and retrieval of stored BMI values.
- `CaloriesRepository` handles daily calorie persistence by user and date.
- `WaterRepository` handles stored water records and daily water-goal persistence.

The MongoDB implementations contain database-specific behavior such as queries, `ObjectId` resolution, upserts, sessions, transactions, and document mapping.

Business rules remain in the service layer. For example, `WaterService` calculates daily totals and weekly averages, `UserHealthService` validates calorie values and selects the application date, and `StatisticsService` classifies BMI values.

`MongoUserRepository` also keeps the cross-collection cleanup required for atomic user deletion because that operation is implemented as a MongoDB transaction.

This separates application business logic from database-specific implementation details.

---

## Configuration Layer

```text
config/
├── MongoConfiguration.kt
└── PasswordConfiguration.kt

EnvConfiguration/
└── EnvConfig.kt
```

`MongoConfiguration` creates shared MongoDB dependencies:

```text
MongoClient
MongoDatabase
```

MongoDB configuration is loaded through `EnvConfig`.

Environment keys include:

```text
mongodb.uri
mongodb.database
JWT_SECRET
```

`PasswordConfiguration` provides BCrypt password encoding.

---

## DTO Layer

```text
dto/
├── CaloriesResponse.kt
├── GoalResponse.kt
├── GoalUpdateResponse.kt
├── LoginRequest.kt
├── LoginResponse.kt
├── SignupRequest.kt
├── UserResponse.kt
└── WaterResponse.kt
```

DTOs separate the REST API contract from internal persistence models.

Passwords are not exposed in user response DTOs.

---

## Validation and Exception Handling

The backend uses Jakarta Bean Validation.

Examples include:

```text
@Valid
@NotBlank
@Min
```

Validation errors are handled centrally through:

```text
exception/
└── GlobalExceptionHandler.kt
```

This avoids repeating validation-response logic inside controller methods.

---

# 🍃 MongoDB Database

MongoDB is the persistence database used by the backend.

The local development database is:

```text
hibari_db
```

The database contains four primary collections:

```text
users
water_records
calories
goals
```

Relationships between collections use MongoDB `ObjectId` values.

---

## `users`

Example structure:

```json
{
  "_id": "ObjectId(...)",
  "username": "exampleUser",
  "passwordHash": "$2a$...",
  "fullName": "Example User",
  "age": 25,
  "bmi": 22.4,
  "transactionVersion": 0
}
```

Important fields:

```text
_id
username
passwordHash
fullName
age
bmi
transactionVersion
```

`passwordHash` contains the BCrypt password hash.

Raw passwords are not intentionally stored in MongoDB.

`transactionVersion` is used as a common write point during transactional operations.

---

## `water_records`

Each water intake entry is stored as an individual document.

Example:

```json
{
  "_id": "ObjectId(...)",
  "userId": "ObjectId(...)",
  "amountMl": 500,
  "recordedAt": "BSON Date"
}
```

This allows the server to calculate:

- Daily water totals
- Yesterday's total
- Historical water data
- Weekly averages
- Individual drink history

There is no fixed number of water records per day.

---

## `calories`

Calories are stored by user and date.

Example:

```json
{
  "_id": "ObjectId(...)",
  "userId": "ObjectId(...)",
  "calories": 2400,
  "recordDate": "yyyy-MM-dd"
}
```

Only one calorie document is allowed per user and date.

Today's calories are stored using MongoDB upsert behavior.

---

## `goals`

Daily water goals are stored by user and date.

Example:

```json
{
  "_id": "ObjectId(...)",
  "userId": "ObjectId(...)",
  "goalMl": 3000,
  "recordDate": "yyyy-MM-dd"
}
```

Only one goal document is allowed per user and date.

Historical goal documents remain stored.

Today's goal is stored using MongoDB upsert behavior.

---

## MongoDB Indexes

The database design uses the following indexes.

### Unique username

```javascript
db.users.createIndex(
    { username: 1 },
    { unique: true }
)
```

This prevents duplicate usernames, including concurrent signup attempts.

### Water history

```javascript
db.water_records.createIndex(
    { userId: 1, recordedAt: 1 }
)
```

This supports time-range queries for water history.

### Calories

```javascript
db.calories.createIndex(
    { userId: 1, recordDate: 1 },
    { unique: true }
)
```

This guarantees one calorie document per user per day.

### Goals

```javascript
db.goals.createIndex(
    { userId: 1, recordDate: 1 },
    { unique: true }
)
```

This guarantees one goal document per user per day.

---

## MongoDB Transactions

The backend uses MongoDB transactions for operations that modify multiple related documents.

Examples include:

- User-related writes
- Water updates
- Calories updates
- Goal updates
- User deletion with related records

Repositories create a MongoDB client session and execute related operations inside a transaction.

This helps prevent inconsistent data when concurrent requests occur.

---

## MongoDB Replica Set

MongoDB multi-document transactions require a replica set.

Local development uses a single-node replica set:

```text
rs0
```

Example local MongoDB URI:

```text
mongodb://localhost:27017/?replicaSet=rs0
```

Typical initialization:

```javascript
rs.initiate()
```

A healthy local node should become:

```text
PRIMARY
```

---

## Asynchronous Repository Contracts

The MongoDB driver used by the project performs synchronous database operations.

Repository methods preserve asynchronous application contracts by wrapping work in:

```kotlin
CompletableFuture.supplyAsync {
    // MongoDB operation
}
```

This allows the service and controller layers to continue using `CompletableFuture`-based APIs.

---

# 🔄 Data Flow – Water Update

Example water update flow:

1. User presses an Add Water button.
2. Android creates an HTTPS PATCH request.
3. Android sends the JWT as a Bearer token.
4. TLS protects the request.
5. `JwtAuthenticationFilter` validates the JWT.
6. `UsersController` receives the authorized request.
7. `WaterService` validates the amount and selects the application timestamp.
8. `WaterRepository` defines the persistence operation.
9. `MongoWaterRepository` resolves the user and performs the MongoDB work.
10. A MongoDB transaction protects the user lock and water-record insert.
11. MongoDB stores the water record.
12. The result propagates back through the repository, service, and controller.
13. Android receives the HTTPS response.

```text
User
 ↓
Android
 ↓
HTTPS + Bearer JWT
 ↓
JwtAuthenticationFilter
 ↓
UsersController
 ↓
WaterService
 ↓
WaterRepository
 ↓
MongoWaterRepository
 ↓
MongoDB Transaction
 ↓
MongoDB
```

---

# 🔐 Security

The project uses multiple security layers.

## BCrypt

Passwords are hashed using BCrypt before persistence.

Login verifies the supplied password against the stored BCrypt hash.

```text
Raw Password
      ↓
BCrypt
      ↓
passwordHash
      ↓
MongoDB
```

---

## JWT

Successful login returns a signed JWT access token.

The JWT contains the authenticated username as its subject.

Android sends the token on protected requests:

```http
Authorization: Bearer <JWT>
```

The access token is stateless.

It is not stored in MongoDB as a server-side session.

---

## HTTPS / TLS

Communication between Android and Spring Boot uses HTTPS.

The local server uses:

```text
src/main/resources/keystore.p12
```

The PKCS#12 keystore contains:

```text
Server certificate
Public key
Private key
```

The private key remains on the backend.

Android receives only the public development certificate:

```text
hibari_local.crt
```

---

## Security Layers

```text
BCrypt
   ↓
Protects passwords at rest

JWT
   ↓
Authenticates and authorizes API requests

HTTPS / TLS
   ↓
Protects traffic between Android and Spring Boot
```

Example login flow:

```text
Raw password entered in Android
        ↓
HTTPS
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
        ↓
Protected requests use Bearer JWT
```

---

# 🧪 Software Testing

Both projects include automated tests.

---

## 📱 Android Testing

All current Android test classes are local JVM tests under:

```text
app/src/test/java/com/example/myfinaltopapplication/
```

Current test classes:

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

The current `androidTest` source set contains no test source files.

The main Android test suite therefore does not require an Android emulator or physical Android device.

---

### Android Test Technologies

- JUnit 4.13.2
- Robolectric 4.17
- Mockito 5.24.0
- Mockito Kotlin 6.4.0
- OkHttp MockWebServer 5.5.0
- AndroidX Test Core

---

### Robolectric

Robolectric is used to execute Android framework behavior directly on the JVM.

Tests cover:

- Activity creation
- Activity lifecycle
- UI elements
- Navigation
- SharedPreferences
- Toast messages
- Notifications
- AlarmManager behavior
- Water reminders
- Charts

Activity tests keep their `ActivityController` references and explicitly close them after each test.

This ensures that Robolectric Activity resources are released after test execution.

---

### Mockito

Mockito is used to isolate dependencies.

The Android tests use Kotlin-aware Mockito matchers where appropriate.

Examples include:

```kotlin
any<String>()
any<Int>()
any<Double>()
eq("john")
eq(200)
```

---

### MockWebServer

`RestClientTest.kt` uses OkHttp MockWebServer.

It runs as a local JVM test with Robolectric.

The real `RestClient` uses:

```text
https://10.0.2.2:8443/myapp/api/users
```

During `RestClientTest`, a test OkHttp client rewrites the destination to:

```text
http://localhost:<random-port>
```

The test keeps the original:

- HTTP method
- Request path
- Query parameters
- Headers
- Request body

This allows the real REST client behavior to be tested without starting the Spring Boot server.

---

### Running Android Tests

Windows:

```powershell
gradlew.bat :app:testDebugUnitTest --tests "com.example.myfinaltopapplication.*"
```

Unix-like systems:

```bash
./gradlew :app:testDebugUnitTest --tests "com.example.myfinaltopapplication.*"
```

No emulator is required for these local JVM tests.

---

## 🌐 Backend Testing

Backend tests are written in Kotlin.

Current test classes:

```text
CapstoneServicesIntegrationTest.kt
UsersControllerIntegrationTest.kt
JwtServiceTest.kt
```

Testing technologies include:

- JUnit 5
- JUnit Jupiter
- Spring Boot Test
- TestRestTemplate
- Mockito
- MongoDB integration testing

---

### `CapstoneServicesIntegrationTest`

Tests the real service/repository architecture against MongoDB.

Coverage includes:

- Signup
- Duplicate signup
- Concurrent duplicate signup
- User creation
- User existence
- User deletion
- Login
- Password validation
- Water updates
- Water totals
- Water history
- Weekly averages
- Goals
- Calories
- BMI statistics
- MongoDB transactions
- Concurrent operations
- Removal of related MongoDB documents

MongoDB must be available for these integration tests.

---

### `UsersControllerIntegrationTest`

Runs the Spring Boot application with an embedded web server.

It performs real HTTP requests through `TestRestTemplate`.

Coverage includes:

- Health endpoint
- Signup
- Login
- GET user
- GET all users
- PATCH user
- DELETE user
- HEAD user
- BMI
- Water
- Water history
- Weekly averages
- Goals
- Calories
- HTTP status codes
- Bearer authentication

---

### `JwtServiceTest`

Tests JWT functionality independently.

Coverage includes:

- Token generation
- Valid-token validation
- Username extraction
- Invalid token
- Tampered token
- Different signing algorithm

MongoDB is not required for the JWT-only tests.

---

# 🌐 REST API

Base path:

```text
/api/users
```

---

## Public Endpoints

```text
GET     /health
POST    /signup
POST    /login
GET     /stats/bmiDistribution
```

These endpoints do not require a JWT.

---

## Protected Endpoints

Protected endpoints require:

```http
Authorization: Bearer <JWT>
```

Available user-related routes include:

```text
GET     /
GET     /{username}
HEAD    /{username}
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

# 📊 Features

## 👤 User System

- Signup
- Login
- User retrieval
- User listing
- Partial user update
- User deletion
- User existence checks
- BCrypt password hashing
- JWT authentication
- User-specific authorization
- Local Android session persistence

---

## 💧 Water Tracking

- Add water intake
- Store individual drink records
- Calculate today's total
- Calculate yesterday's total
- Retrieve water history
- Calculate weekly averages
- Transaction-safe updates

---

## ⚖️ BMI Tracking

- Calculate BMI
- Store BMI
- Update BMI
- Retrieve BMI-related data
- Global BMI distribution statistics

---

## 🔥 Calories

- Store daily calories
- Retrieve daily calories
- Validate calorie values in `UserHealthService`
- Select the application date in the service layer
- Use the dedicated `CaloriesRepository` persistence abstraction
- MongoDB upsert
- Transaction-protected updates

---

## 🎯 Daily Water Goals

- Retrieve the most recently stored goal
- Apply the default goal in `WaterService` when no stored value exists
- Update today's goal
- Keep historical goal records
- Validate allowed goal values in `WaterService`
- MongoDB upsert

---

## 📈 Visualization

- Water history chart
- Weekly water chart
- Daily water tracking
- Daily goal progress
- BMI distribution chart

---

## 🔔 Notifications

- Water reminder switch
- AlarmManager scheduling
- Water reminder receiver
- Android notifications

---

# 🛠 Technologies Used

## Android

- Kotlin
- Android SDK
- Android Gradle Plugin 9.4.1
- Gradle 9.6.1
- Gradle Kotlin DSL
- AndroidX
- OkHttp 5.5.0
- SharedPreferences
- MPAndroidChart

---

## Backend

- Kotlin 1.9.25
- Spring Boot 3.5.16
- Spring Web
- Spring Security Crypto
- BCrypt
- Jakarta Bean Validation
- Maven
- REST API
- HTTPS / TLS
- PKCS#12
- MongoDB
- MongoDB synchronous driver
- BSON
- MongoDB transactions
- MongoDB upsert
- MongoDB replica set
- Nimbus JOSE + JWT
- CompletableFuture

The backend build is configured to use JDK 23.

---

## Database

- MongoDB
- Four primary collections
- ObjectId relationships
- Unique indexes
- Compound indexes
- Transactions
- Upserts
- Single-node development replica set

---

## Android Testing

- JUnit 4.13.2
- Robolectric 4.17
- Mockito 5.24.0
- Mockito Kotlin 6.4.0
- MockWebServer 5.5.0
- AndroidX Test Core

---

## Backend Testing

- JUnit 5
- JUnit Jupiter
- Spring Boot Test
- TestRestTemplate
- Mockito
- MongoDB integration testing

---

## Development Tools

- Android Studio
- IntelliJ IDEA
- Git
- GitHub
- Maven
- Gradle
- MongoDB Shell (`mongosh`)
- Java Keytool

---

# ▶️ Running the Project

## MongoDB

MongoDB must be running before starting the backend.

Local development uses replica set:

```text
rs0
```

Example URI:

```text
mongodb://localhost:27017/?replicaSet=rs0
```

The local database is:

```text
hibari_db
```

MongoDB configuration is loaded from `.env`.

Required configuration includes values for:

```text
mongodb.uri
mongodb.database
JWT_SECRET
```

---

## Spring Boot Server

Open:

```text
Spring Server
```

The application entry point is:

```text
src/main/java/org/example/CapstoneProject/Application.kt
```

Run from IntelliJ IDEA or Maven:

```bash
mvn spring-boot:run
```

The context path is:

```text
/myapp
```

HTTPS port:

```text
8443
```

Health endpoint:

```text
https://localhost:8443/myapp/api/users/health
```

The local Spring Boot server uses:

```text
src/main/resources/keystore.p12
```

---

## Android Application

Open:

```text
Hai-Bari android application
```

in Android Studio.

Allow Gradle synchronization to complete.

Start:

1. MongoDB
2. Spring Boot server
3. Android application

The Android emulator accesses the host backend through:

```text
https://10.0.2.2:8443/myapp/api/users
```

---

# 🔑 Local HTTPS Certificate Design

The backend uses:

```text
keystore.p12
```

The keystore contains:

```text
Certificate
Public key
Private key
```

The private key remains on the backend.

Android uses:

```text
hibari_local.crt
```

This file contains the public certificate and does not contain the backend private key.

```text
Spring Boot
keystore.p12
    │
    ├── Certificate
    ├── Public Key
    └── Private Key
          │
          │ public certificate export
          ▼
Android
hibari_local.crt
    │
    ├── Certificate
    └── Public Key
```

---

# 🔒 Sensitive Files

Sensitive or machine-specific files should not be committed.

Backend examples:

```text
.env
src/main/resources/application.properties
src/main/resources/keystore.p12
*.p12
*.pfx
```

Android examples:

```text
local.properties
*.jks
*.keystore
```

Generated directories should also remain outside version control:

```text
.gradle/
.idea/
build/
target/
```

---

# 🚀 Future Improvements

Possible future improvements include:

- Refresh-token rotation
- Server-side token revocation
- Stronger encrypted Android token storage
- Production MongoDB authentication and authorization
- Production MongoDB replica-set deployment
- External production secret management
- Trusted CA-issued production TLS certificates
- Cloud deployment
- More health statistics
- Smart hydration suggestions
- Improved UI and UX
- Improved notification scheduling
- Additional charts and reports
- Offline support
- Additional automated tests

---

# 👨‍💻 Author

Sharbel Zarzour

---

# 🎓 Academic Context

This project was developed as a final capstone project in Software Engineering studies.

---

# 💡 Key Strengths

- Full-stack architecture
- Kotlin-only application source code
- Kotlin Android client
- Kotlin Spring Boot backend
- Layered backend architecture
- Controller / Service / Repository separation
- Service-owned business rules and validation
- MongoDB repository abstraction
- Dedicated user, water, and calorie repository contracts
- Dedicated MongoDB repository implementations
- Separation of BMI classification from BMI persistence
- Separation of water calculations from water persistence
- Centralized MongoDB configuration
- Four-collection MongoDB data model
- ObjectId-based relationships
- MongoDB indexes
- MongoDB upserts
- MongoDB multi-document transactions
- Replica-set transaction support
- Concurrency-oriented integration testing
- Request and response DTOs
- Jakarta Bean Validation
- Centralized exception handling
- Constructor dependency injection
- BCrypt password hashing
- JWT authentication
- User-specific JWT authorization
- Stateless access tokens
- HTTPS/TLS client-server communication
- Local Android certificate trust
- PKCS#12 backend keystore
- Android Network Security Configuration
- OkHttp REST client
- Local JVM Android testing
- Robolectric Activity testing
- Explicit ActivityController cleanup
- MockWebServer REST testing
- Spring Boot integration testing
- TestRestTemplate controller testing
- MongoDB integration testing
- CompletableFuture-based asynchronous API contracts
- Transaction-safe water updates
- Transaction-safe calories and goal updates
- Separation between Android, backend, and database layers
