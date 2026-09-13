# 💧 Hi-Bari – Health & Water Tracking System

## 📌 Overview

Hi-Bari is a full-stack health tracking application designed to monitor daily water intake, calculate BMI, manage daily water goals, track calories, and store user health data.

The system combines an Android mobile client, a Spring Boot REST API, and MongoDB.

The backend has been refactored into separated controller, service, repository, configuration, DTO, security, and exception-handling layers so that HTTP handling, business logic, authentication, and MongoDB access are not mixed together.

Communication between the Android application and the Spring Boot backend uses HTTPS/TLS.

---

## 🎥 Application Demo

Click the demo image in the repository to watch a short demonstration of the Hi-Bari Android application.

---

## 🗂 Repository Structure

The repository contains two main projects: the Android client and the Spring Boot backend.

The tree below focuses on the source files and configuration that are relevant to the application. Generated build output, IDE metadata, and Gradle/Maven caches are intentionally omitted.

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
        │   │       │
        │   │       ├── configuration/
        │   │       │   ├── MongoConfiguration.java
        │   │       │   └── PasswordConfiguration.java
        │   │       │
        │   │       ├── EnvConfiguration/
        │   │       │   └── EnvConfig.java
        │   │       │
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
        │   │       │
        │   │       ├── exception/
        │   │       │   └── GlobalExceptionHandler.java
        │   │       │
        │   │       ├── model/
        │   │       │   └── User.java
        │   │       │
        │   │       ├── repository/
        │   │       │   ├── UserRepository.java
        │   │       │   ├── WaterRepository.java
        │   │       │   └── mongo/
        │   │       │       ├── MongoUserRepository.java
        │   │       │       └── MongoWaterRepository.java
        │   │       │
        │   │       ├── security/
        │   │       │   └── JwtAuthenticationFilter.java
        │   │       │
        │   │       ├── service/
        │   │       │   ├── AuthenticationService.java
        │   │       │   ├── JwtService.java
        │   │       │   ├── UserService.java
        │   │       │   ├── WaterService.java
        │   │       │   ├── UserHealthService.java
        │   │       │   └── StatisticsService.java
        │   │       │
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

> `application.properties`, `.env`, and `keystore.p12` contain local backend configuration and must be handled as local/sensitive configuration where appropriate.

> Generated directories such as `.gradle/`, `.idea/`, `build/`, `target/`, and other IDE/build artifacts are intentionally not shown in this tree.

---

## 📱 Hai-Bari Android Application

Contains the Android client, user interface, session handling, water tracking, BMI calculation, charts, daily water goal management, calorie-related interaction, and HTTPS communication with the backend through OkHttp.

---

## 🌐 Spring Server

Contains the Spring Boot REST API, service layer, repository abstraction, MongoDB repository implementations, request/response DTOs, authentication, authorization, validation, centralized exception handling, HTTPS configuration, MongoDB transactions, upsert operations, and concurrency-safe database operations.

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
MongoDB Repository Implementations
        ↓
MongoDB
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
MongoDB Repository Implementation
        ↓
MongoDB
```

This separation keeps MongoDB-specific code out of the controller and service layers and keeps authentication logic outside the controller methods.

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
- Does not contain MongoDB credentials
- Does not contain the Spring Boot server private key

Main application areas include:

- Login
- Signup
- Home page
- Water tracking
- BMI tracking
- Water history and charts
- Daily water goal management
- Calories

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
- Does not access MongoDB directly

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
- Hashes passwords before persistence
- Performs PATCH type validation where required
- Delegates persistence operations to repository interfaces
- Does not contain MongoDB-specific database commands

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
└── mongo/
    ├── MongoUserRepository.java
    └── MongoWaterRepository.java
```

Responsibilities:

- Defines persistence contracts through interfaces
- Contains MongoDB-specific operations only in MongoDB repository implementations
- Uses the synchronous MongoDB Java Driver
- Wraps blocking database work with `CompletableFuture.supplyAsync(...)`
- Handles MongoDB reads, writes, filters, updates, deletes, upserts, and transactions
- Handles transaction-based concurrency protection
- Converts MongoDB `Document` objects into application models

---

### Configuration Layer

```text
configuration/
├── MongoConfiguration.java
└── PasswordConfiguration.java

EnvConfiguration/
└── EnvConfig.java
```

Responsibilities:

- Creates the shared `MongoClient` Spring bean
- Creates the shared `MongoDatabase` Spring bean
- Provides the BCrypt `PasswordEncoder` bean
- Loads MongoDB and JWT-related environment configuration
- Uses constructor-based dependency injection throughout the application
- Loads the local HTTPS keystore through Spring Boot configuration

MongoDB configuration is loaded from `.env`:

```env
mongodb.uri=mongodb://localhost:27017/?replicaSet=rs0
mongodb.database=hibari_db
```

JWT configuration is also loaded from `.env`:

```env
JWT_SECRET=<local-secret>
```

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

## ✅ Validation and Exception Handling

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

Dynamic PATCH requests are additionally validated before values reach MongoDB.

Supported PATCH field types include:

```text
password -> String
fullName -> String
age      -> Number
bmi      -> Number
```

For example, a request such as:

```json
{
  "password": 123
}
```

is rejected instead of allowing MongoDB to store an invalid BSON type in `passwordHash`.

Manual validation errors are returned as HTTP `400 Bad Request`.

---

## 🍃 MongoDB Database

Hi-Bari uses MongoDB as its persistence database.

The local development database is:

```text
hibari_db
```

The backend uses the official synchronous MongoDB Java Driver.

The application currently uses four main collections:

```text
users
water_records
calories
goals
```

---

### `users`

Example document:

```json
{
  "_id": "ObjectId(...)",
  "username": "sharbel",
  "passwordHash": "$2a$...",
  "fullName": "Sharbel Zarzour",
  "age": 25,
  "bmi": 22.4,
  "transactionVersion": 0
}
```

The `username` is protected by a unique MongoDB index.

The stored password value is a BCrypt hash rather than the user's raw plaintext password.

`transactionVersion` is incremented by transaction-based operations that need to coordinate concurrent writes affecting the same user.

---

### `water_records`

Each drink is stored as a separate document:

```json
{
  "_id": "ObjectId(...)",
  "userId": "ObjectId(...)",
  "amountMl": 500,
  "recordedAt": "BSON Date"
}
```

Each drink remains independent instead of being appended to a fixed-size array.

This allows the backend to calculate:

- daily totals,
- daily history,
- individual drink entries,
- weekly averages.

---

### `calories`

Daily calories are stored as:

```json
{
  "_id": "ObjectId(...)",
  "userId": "ObjectId(...)",
  "calories": 2400,
  "recordDate": "2026-09-13"
}
```

One calories document is allowed per user per date.

Today's calories are stored using MongoDB `upsert`.

---

### `goals`

Daily water goals are stored as:

```json
{
  "_id": "ObjectId(...)",
  "userId": "ObjectId(...)",
  "goalMl": 3000,
  "recordDate": "2026-09-13"
}
```

Older goal documents remain stored as history.

Today's goal is stored using MongoDB `upsert`.

---

## 🔍 MongoDB Indexes

The database uses indexes for uniqueness and query performance.

```javascript
db.users.createIndex(
    { username: 1 },
    { unique: true }
)
```

```javascript
db.water_records.createIndex(
    { userId: 1, recordedAt: 1 }
)
```

```javascript
db.calories.createIndex(
    { userId: 1, recordDate: 1 },
    { unique: true }
)
```

```javascript
db.goals.createIndex(
    { userId: 1, recordDate: 1 },
    { unique: true }
)
```

The unique username index prevents duplicate users even when concurrent signup requests occur.

The unique calorie and goal indexes guarantee at most one document per user and date.

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
9. `MongoWaterRepository` starts a MongoDB transaction.
10. The user's `transactionVersion` is incremented inside the transaction.
11. A new document is inserted into `water_records`.
12. The transaction is committed.
13. The result travels back through the repository, service, and controller.
14. The response returns to the Android application through HTTPS.

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
MongoWaterRepository
    ↓
MongoDB Transaction
    ↓
users.transactionVersion increment
    ↓
water_records.insertOne(...)
    ↓
Transaction Commit
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

The current persistence implementations are MongoDB-based:

```text
MongoUserRepository
MongoWaterRepository
```

This keeps higher layers independent from MongoDB-specific APIs.

---

### 🔹 Constructor Dependency Injection

Services and repositories are injected using constructors.

This makes dependencies explicit and avoids direct object creation inside controllers.

---

### 🔹 Asynchronous Backend Operations

The project uses the synchronous MongoDB Java Driver.

Blocking repository work is wrapped with:

```java
CompletableFuture.supplyAsync(...)
```

This preserves the application's existing asynchronous service and controller contracts.

---

### 🔹 MongoDB Transactions

Operations that coordinate data across multiple documents or collections use MongoDB transactions.

Transaction-based operations include:

```text
deleteByUsername()
updateWater()
updateCalories()
updateGoalMl()
```

The repositories use the user document as a shared concurrency point through:

```text
transactionVersion
```

Conceptually:

```text
Transaction starts
        ↓
Find user
        ↓
Increment transactionVersion
        ↓
Perform related database operation
        ↓
Commit
```

This helps prevent races such as:

```text
Delete user
        ↕
Insert related water/calorie/goal data
```

---

### 🔹 `withTransaction(...)`

MongoDB's convenient transaction API can be used:

```java
session.withTransaction(() -> {
    // transactional MongoDB operations
    return true;
});
```

`withTransaction(...)` manages transaction start, commit, abort behavior, and retry handling for eligible transient transaction errors.

Every MongoDB command that belongs to the transaction receives the same `ClientSession`.

Example:

```java
waterRecords.insertOne(
        session,
        document
);
```

---

### 🔹 MongoDB Upsert

Calories and daily goals use MongoDB `upsert`.

For calories:

```javascript
db.calories.updateOne(
    {
        userId: userId,
        recordDate: today
    },
    {
        $set: {
            calories: caloriesValue
        }
    },
    {
        upsert: true
    }
)
```

Behavior:

```text
Today's document exists
        ↓
Update it

Today's document does not exist
        ↓
Insert it
```

For goals:

```javascript
db.goals.updateOne(
    {
        userId: userId,
        recordDate: today
    },
    {
        $set: {
            goalMl: goalMl
        }
    },
    {
        upsert: true
    }
)
```

This removes the need for a separate:

```text
find
 ↓
insert or update
```

sequence.

Water does not use upsert because every drink should remain a separate document.

---

### 🔹 MongoDB Replica Set

MongoDB transactions require a replica set.

The local development environment uses a single-node replica set:

```text
rs0
```

MongoDB configuration:

```yaml
replication:
  replSetName: rs0
```

Connection URI:

```text
mongodb://localhost:27017/?replicaSet=rs0
```

The local node operates as:

```text
PRIMARY
```

This enables MongoDB transaction support while using only one local MongoDB server.

---

### 🔹 BCrypt Password Hashing

User passwords are never stored as plaintext.

Signup and password-update flows hash raw passwords with BCrypt before persistence.

Login verifies credentials using:

```java
PasswordEncoder.matches(...)
```

MongoDB stores the hash as:

```text
passwordHash
```

BCrypt protects stored passwords even if database contents are exposed.

---

### 🔹 JWT Authentication and Authorization

Successful login returns a signed JWT access token.

The Android client stores the token locally and sends it in the `Authorization` header for protected requests:

```http
Authorization: Bearer <JWT>
```

The backend validates the token before protected controller endpoints execute and checks that the token subject matches the username in the requested URL.

The access token is stateless and is not stored in MongoDB.

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

A separate debug network configuration permits HTTP only for local MockWebServer instrumented tests.

This keeps the real application connection encrypted while still allowing isolated local HTTP mocks during automated testing.

---

### 🔹 Request and Response DTOs

The backend does not need to expose the internal `User` model directly through user-related REST responses.

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

Manual PATCH validation errors are also converted into HTTP `400 Bad Request`.

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
- MongoDB integration testing
- Transaction testing
- Concurrency testing
- Asynchronous operation testing
- JWT unit testing

The backend includes three main test groups:

```text
CapstoneServicesIntegrationTest
UsersControllerIntegrationTest
JwtServiceTest
```

`CapstoneServicesIntegrationTest` verifies the service and repository flow against MongoDB.

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
- Concurrent signup protection
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
- MongoDB transactions
- MongoDB upserts
- Transaction-version changes
- Cascade deletion
- Concurrency invariants
- Asynchronous operations
- Error handling

---

## 🔐 Security

Current security-related design:

- User passwords are hashed with BCrypt before they are stored in MongoDB
- Login verifies the raw password against the stored BCrypt hash
- Successful login returns a signed JWT access token
- JWTs contain the authenticated username as the token subject and have a limited lifetime
- Protected Android requests send the token through `Authorization: Bearer <token>`
- `JwtAuthenticationFilter` validates protected requests before they reach `UsersController`
- Missing, malformed, invalid, or expired tokens are rejected with `401 Unauthorized`
- A valid token used against another user's protected URL is rejected with `403 Forbidden`
- The JWT access token is not stored in MongoDB
- Android-to-backend communication uses HTTPS/TLS
- Cleartext traffic is disabled for the real backend connection
- The local Android HTTPS connection trusts the configured development certificate
- The Spring Boot private key remains inside the local PKCS#12 keystore
- The server keystore is excluded from version control
- MongoDB connection values are loaded from `.env`
- JWT secret values are loaded from `.env`
- Sensitive local configuration files are excluded from version control
- MongoDB access is centralized in backend repository implementations
- Request validation rejects invalid input before persistence
- PATCH values are type-checked before reaching MongoDB
- User response DTOs do not expose passwords
- User passwords are not included in the `User.toString()` output

Public endpoints include:

```text
GET  /api/users/health
POST /api/users/signup
POST /api/users/login
GET  /api/users/stats/bmiDistribution
```

User-specific endpoints require a valid JWT.

Sensitive files excluded from the repository should include:

```text
.env
application.properties
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

- Java 23
- Spring Boot 3.5
- Spring Web
- Spring Security Crypto
- BCrypt
- Jakarta Bean Validation
- Maven
- REST API
- HTTPS / TLS
- PKCS#12
- Nimbus JOSE + JWT
- CompletableFuture
- MongoDB Java Driver

### Database

- MongoDB 8
- BSON
- MongoDB Java Driver
- Compound indexes
- Unique indexes
- Transactions
- Upserts
- Single-node replica set for local development

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
- MongoDB integration testing
- MongoDB transaction testing
- Concurrency testing
- JWT unit testing

### Development Tools

- Android Studio
- IntelliJ IDEA
- Git
- GitHub
- Java Keytool
- Maven
- MongoDB Shell (`mongosh`)

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

BMI categories:

```text
Underweight: BMI < 18.5
Normal:      18.5 <= BMI < 25
Overweight:  25 <= BMI < 30
Obese:       BMI >= 30
```

### 🔥 Calories

- Store daily calories
- Retrieve calories
- Validate allowed calorie updates
- Update or create today's calorie document using MongoDB upsert

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

Allow Gradle to synchronize, make sure MongoDB and the Spring Boot server are running, select an Android emulator, and run the application.

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

### 🍃 MongoDB

MongoDB must be running before starting the backend.

The local database is:

```text
hibari_db
```

The local server uses a single-node replica set:

```text
rs0
```

MongoDB configuration:

```yaml
replication:
  replSetName: rs0
```

The replica set should report:

```text
PRIMARY
```

The backend uses:

```env
mongodb.uri=mongodb://localhost:27017/?replicaSet=rs0
mongodb.database=hibari_db
```

The replica set is required because the backend uses MongoDB transactions.

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

Sensitive MongoDB, JWT, and HTTPS configuration values are required locally but are not intended to be committed to the repository.

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

The controller integration tests use an embedded web server with a random port, so the normal server does not need to be started manually before running them.

MongoDB must be available because the integration tests use the real MongoDB-backed repositories.

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

MongoDB Transactions
    ↓
Protect multi-document consistency
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
- Add additional dedicated `JwtAuthenticationFilter` security tests
- Replace the local self-signed development certificate with a trusted CA-issued certificate when deploying the backend publicly
- Move production HTTPS certificate/private-key management outside the application package
- Use environment-based HTTPS keystore credentials for deployment
- Add production MongoDB authentication and authorization
- Move production MongoDB deployment away from the local single-node development replica set
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
- MongoDB database
- Layered backend architecture
- Controller / Service / Repository separation
- Repository abstraction
- Dedicated MongoDB repository implementations
- Official MongoDB Java Driver
- Request and response DTOs
- Jakarta Bean Validation
- Manual PATCH type validation
- Centralized exception handling
- Constructor dependency injection
- Automated Android and backend testing
- Robolectric-based JVM testing
- Mocked HTTP testing with MockWebServer
- Spring integration testing with TestRestTemplate
- MongoDB integration testing
- Asynchronous repository contracts with CompletableFuture
- MongoDB multi-document transactions
- `withTransaction(...)` retry support for eligible transient failures
- Transaction-based concurrency protection
- Shared `transactionVersion` concurrency mechanism
- MongoDB upsert operations
- Unique and compound MongoDB indexes
- Cascade deletion of user-related MongoDB data
- Concurrency integrity tests
- Individual water-record document storage
- Historical calorie and goal storage
- Single-node MongoDB replica set for local development
- Separation between client, server, and database
- Organized multi-project repository
- API compatibility preserved during backend database migration
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
