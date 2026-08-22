# 🌐 Hi-Bari Spring Boot Server

## 📌 Overview

This directory contains the backend server for the Hi-Bari health and water tracking system.

The server exposes a REST API used by the Android application, handles authentication, authorization, business logic, validation, and communicates with Firebase Realtime Database through the Firebase Admin SDK.

The backend follows a layered architecture based on controllers, services, repository interfaces, Firebase repository implementations, security components, DTOs, configuration, and centralized exception handling.

---

## 🧠 Server Responsibilities

The Spring Boot server is responsible for:

- User registration and login
- BCrypt password hashing and verification
- JWT generation and validation
- User-specific authorization
- User data management
- BMI data updates
- Calories management
- Daily water goal management
- Water intake updates
- Water history retrieval
- Weekly water averages
- BMI statistics
- Request validation
- REST API response handling
- Firebase communication
- Transaction-safe database updates
- Asynchronous Firebase operations
- Centralized validation error handling

---

## 🗂 Project Structure

```text
Spring Server/
├── README.md
├── pom.xml
├── .gitignore
│
└── src/
    ├── main/
    │   ├── java/
    │   │   └── org/example/CapstoneProject/
    │   │       ├── Application.java
    │   │       │
    │   │       ├── config/
    │   │       │   ├── FirebaseConfiguration.java
    │   │       │   └── PasswordConfiguration.java
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
    │   │       ├── EnvConfiguration/
    │   │       │   └── EnvConfig.java
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
    │   │       │   │
    │   │       │   └── firebase/
    │   │       │       ├── FirebaseUserRepository.java
    │   │       │       └── FirebaseWaterRepository.java
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
    │
    └── test/
        └── java/
            └── CapstoneTests/
                ├── CapstoneServicesIntegrationTest.java
                ├── UsersControllerIntegrationTest.java
                └── JwtServiceTest.java
```

---

## 🧩 Main Components

### `Application.java`

The main Spring Boot application class used to start the server.

---

### `UsersController.java`

Defines the REST API endpoints used by the Android application.

The controller is responsible for:

- Receiving HTTP requests
- Reading path variables and query parameters
- Receiving and validating request DTOs
- Calling the appropriate service
- Converting service results into HTTP responses
- Returning response DTOs or dynamic map-based responses when appropriate

The controller does not communicate with Firebase directly.

Authentication and authorization checks for protected endpoints are performed before the request reaches the controller.

---

## 🧠 Service Layer

The service layer is divided by responsibility.

### `AuthenticationService.java`

Handles authentication-related application logic.

Responsibilities include:

- User signup
- Username existence checks during registration
- BCrypt password encoding during signup
- Login credential validation
- Comparing raw login passwords against stored BCrypt hashes

### `JwtService.java`

Handles JWT creation and validation.

Responsibilities include:

- Generating signed JWT access tokens
- Adding the authenticated username as the token subject
- Adding issue and expiration timestamps
- Validating token signatures
- Validating token expiration
- Extracting the username from the token subject

### `UserService.java`

Handles general user operations.

Responsibilities include:

- Retrieving users
- Updating users
- Patching users
- Deleting users
- Checking whether a user exists
- Encoding updated passwords before persistence

### `WaterService.java`

Handles water-related application operations.

Responsibilities include:

- Updating daily water intake
- Retrieving today's and yesterday's water
- Retrieving water history
- Retrieving weekly averages
- Managing daily water goals

### `UserHealthService.java`

Handles user health-related data.

Responsibilities include:

- Updating BMI
- Retrieving calories
- Updating calories

### `StatisticsService.java`

Handles global statistical operations.

Responsibilities include:

- Retrieving BMI distribution statistics

---

## 🔐 Security Layer

### `JwtAuthenticationFilter.java`

Protected REST requests pass through `JwtAuthenticationFilter` before reaching `UsersController`.

The filter:

- Reads the `Authorization` header
- Expects the format:

```http
Authorization: Bearer <JWT>
```

- Rejects missing or malformed Bearer tokens
- Validates the JWT signature
- Rejects expired or invalid tokens
- Extracts the authenticated username from the token subject
- Compares the token username with the `{username}` value in user-specific routes
- Prevents one authenticated user from accessing another user's protected resources

Typical responses:

```text
Missing / invalid / expired token
→ 401 Unauthorized

Valid token for another username
→ 403 Forbidden

Valid matching token
→ Request continues to UsersController
```

The JWT access token itself is stateless and is not stored in Firebase.

---

## 🔑 Password Security

### `PasswordConfiguration.java`

Provides the shared BCrypt password encoder as a Spring bean.

Passwords are handled as follows:

```text
Signup
    ↓
Raw password received
    ↓
BCrypt encoding
    ↓
BCrypt hash stored in Firebase
```

Login works as follows:

```text
Raw login password
    ↓
PasswordEncoder.matches(...)
    ↓
Stored BCrypt hash
    ↓
Authentication succeeds or fails
```

The raw password is never stored directly in Firebase.

Password updates through PUT or PATCH are also encoded before persistence.

---

## 🗄 Repository Layer

The repository layer separates persistence operations from the service layer.

### Repository Interfaces

```text
UserRepository
WaterRepository
```

These interfaces define the persistence operations required by the application without exposing Firebase-specific implementation details.

### Firebase Repository Implementations

```text
FirebaseUserRepository
FirebaseWaterRepository
```

These classes implement the repository interfaces using Firebase Realtime Database.

Responsibilities include:

- Reading data from Firebase
- Creating and updating users
- Deleting users
- Querying users by username
- Updating BMI and calories
- Reading and updating water data
- Managing daily water goals
- Calculating water-related database results
- Performing Firebase transactions
- Wrapping Firebase callbacks with `CompletableFuture`

The service layer depends on repository interfaces instead of depending directly on Firebase-specific classes.

---

## ⚙️ Configuration

### `FirebaseConfiguration.java`

Initializes the Firebase Admin SDK and exposes the shared Firebase database reference as a Spring bean.

Responsibilities include:

- Loading Firebase Admin credentials
- Reading the Firebase database URL
- Initializing Firebase only once
- Creating the shared `Users` database reference
- Providing the database dependency to Firebase repository implementations

### `PasswordConfiguration.java`

Provides the BCrypt `PasswordEncoder` bean used by authentication and user-update operations.

### `EnvConfig.java`

Loads environment-specific configuration such as:

- Firebase database URL
- Firebase credentials configuration
- JWT secret

The JWT secret is loaded from environment configuration rather than being hardcoded in source code.

---

## 📦 DTO Layer

The REST API uses dedicated request and response DTOs.

### Request DTOs

```text
LoginRequest
SignupRequest
UpdateUserRequest
```

These classes represent JSON sent from the Android application to the backend.

### Response DTOs

```text
LoginResponse
UserResponse
WaterResponse
GoalResponse
GoalUpdateResponse
CaloriesResponse
```

These classes represent stable JSON structures returned by the backend.

Using DTOs prevents the REST layer from depending directly on the internal persistence model.

It also allows the backend structure to change while keeping the Android API contract stable.

### Login Response

A successful login returns both the JWT and public user information.

Example:

```json
{
  "token": "<JWT>",
  "userName": "john",
  "age": 25,
  "fullName": "John Doe",
  "bmi": 22.5
}
```

The password is not included in the login response.

---

## ✅ Validation

Request validation uses Jakarta Bean Validation.

Examples include:

```java
@Valid
@NotBlank
@Min
```

Validation is used for request DTOs such as:

```text
LoginRequest
SignupRequest
UpdateUserRequest
```

Example invalid request:

```json
{
  "userName": "",
  "password": ""
}
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

---

## ⚠️ Global Exception Handling

### `GlobalExceptionHandler.java`

Validation exceptions are handled centrally using:

```java
@RestControllerAdvice
```

This prevents every controller endpoint from implementing its own validation error response logic.

The global exception handler converts `MethodArgumentNotValidException` into an HTTP `400 Bad Request` response containing field-specific validation messages.

---

## 🧠 Architecture

The backend request flow for protected endpoints is:

```text
Android Application
        ↓
HTTP REST Request
        ↓
Authorization: Bearer <JWT>
        ↓
JwtAuthenticationFilter
        ↓
UsersController
        ↓
Domain Service
        ↓
Repository Interface
        ↓
Firebase Repository Implementation
        ↓
Firebase Realtime Database
        ↓
HTTP Response
        ↓
Android Application
```

Example authentication flow:

```text
POST /api/users/login
        ↓
UsersController
        ↓
AuthenticationService
        ↓
UserRepository
        ↓
FirebaseUserRepository
        ↓
BCrypt password verification
        ↓
JwtService
        ↓
JWT returned to Android
```

Example user request flow:

```text
Android
    ↓
Bearer JWT
    ↓
JwtAuthenticationFilter
    ↓
UsersController
    ↓
UserService
    ↓
UserRepository
    ↓
FirebaseUserRepository
```

Example water flow:

```text
Android
    ↓
Bearer JWT
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
```

This architecture separates:

```text
Authentication and authorization
HTTP handling
Business/application logic
Persistence contracts
Firebase-specific implementation
Database configuration
REST request/response models
```

---

## 🔄 Water Update Flow

When the Android application sends a request to add water:

1. Android sends the request with a JWT in the `Authorization` header.
2. `JwtAuthenticationFilter` validates the token.
3. The filter verifies that the token subject matches the username in the URL.
4. The request reaches `UsersController`.
5. The controller reads the username and water amount.
6. The controller calls `WaterService`.
7. `WaterService` delegates the persistence operation to `WaterRepository`.
8. `FirebaseWaterRepository` locates the user's daily water log.
9. A Firebase transaction updates the data safely.
10. The new drink amount is added.
11. The total daily amount is updated.
12. Firebase stores the updated list.
13. The result returns through the repository and service layers.
14. The controller returns the HTTP response to the Android application.

```text
PATCH Request
      ↓
Authorization: Bearer <JWT>
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
Firebase Transaction
      ↓
Firebase Realtime Database
      ↓
HTTP Response
```

---

## ⚙️ Transaction-Safe Updates

Water consumption updates use Firebase transaction-based logic to prevent data loss when multiple requests are processed at nearly the same time.

```text
Without transaction:

Request A reads old value
Request B reads old value
One update may overwrite the other ❌
```

```text
With transaction:

Firebase processes the updates safely
Both updates are preserved ✅
```

This helps prevent race conditions and maintains database consistency.

---

## ⚡ Asynchronous Operations

Firebase uses callback-based asynchronous APIs.

The Firebase repository implementations wrap these callbacks with:

```java
CompletableFuture
```

This allows the rest of the application to use asynchronous method chains such as:

```java
thenApply(...)
thenCompose(...)
```

Controller methods can therefore return:

```java
CompletableFuture<ResponseEntity<...>>
```

without manually blocking while waiting for Firebase operations to complete.

---

## ☁️ Firebase Data Structure

Example:

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

The `password` field contains a BCrypt hash rather than the original plaintext password.

JWT access tokens are not stored in Firebase.

Water log format:

```text
Index 0   → Total daily water intake
Index 1-N → Individual drink entries
```

Example:

```text
[1850, 150, 200, 500, 1000]
```

The list is dynamic and can contain multiple drink entries for the same day.

---

## 🌐 REST API

The controller base path is:

```text
/api/users
```

When using the configured application context path, the local server base URL is:

```text
http://localhost:8080/myapp/api/users
```

For the Android emulator:

```text
http://10.0.2.2:8080/myapp/api/users
```

### Public Endpoints

These endpoints do not require a JWT:

```text
GET     /health
POST    /signup
POST    /login
GET     /stats/bmiDistribution
```

### Protected Endpoints

Protected requests require:

```http
Authorization: Bearer <JWT>
```

Endpoints:

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

For user-specific endpoints, the JWT subject must match the `{username}` path value.

---

## 📤 API Response DTO Examples

### Login Response

```json
{
  "token": "<JWT>",
  "userName": "john",
  "age": 25,
  "fullName": "John Doe",
  "bmi": 22.5
}
```

### User Response

```json
{
  "userName": "john",
  "age": 25,
  "fullName": "John Doe",
  "bmi": 22.5
}
```

### Water Response

```json
{
  "todayWater": 1850,
  "yesterdayWater": 1600
}
```

### Daily Goal Response

```json
{
  "goalMl": 3000
}
```

### Goal Update Response

```json
{
  "status": "OK"
}
```

### Calories Response

```json
{
  "calories": 1800
}
```

These DTOs preserve the JSON structure expected by the Android application while avoiding exposure of internal password data.

---

## 🧪 Backend Testing

The server includes integration tests for the service/repository architecture, REST controller layer, and JWT functionality.

Testing technologies include:

- JUnit 5
- JUnit Jupiter
- Spring Boot Test
- TestRestTemplate
- Firebase integration testing
- Asynchronous operation testing

The backend currently contains three main test classes:

```text
CapstoneServicesIntegrationTest
UsersControllerIntegrationTest
JwtServiceTest
```

---

### `CapstoneServicesIntegrationTest`

Tests the backend service flow against Firebase.

The test class works directly with services such as:

```text
UserService
AuthenticationService
WaterService
UserHealthService
StatisticsService
```

Because these tests call services directly rather than using HTTP, JWT authentication is not required for them.

The tests cover operations such as:

- Creating users
- Retrieving users
- BCrypt password storage
- Authentication behavior
- Correct and incorrect login credentials
- Updating user information
- Password update hashing
- Updating BMI
- Updating calories
- Updating water consumption
- Retrieving water history
- Retrieving weekly averages
- Managing daily goals
- BMI distribution
- Asynchronous Firebase operations
- Transaction-safe updates

---

### `UsersControllerIntegrationTest`

Loads the Spring Boot application and sends real HTTP requests using `TestRestTemplate`.

The test server uses a random embedded port.

The Spring Boot server does not need to be started manually before running these integration tests.

Protected requests use real JWTs generated by `JwtService`.

The tests cover:

- REST endpoints
- Signup
- Login
- Login JWT response
- Request DTOs
- Response DTOs
- BCrypt password behavior
- Protected endpoint access
- Bearer-token authenticated requests
- Validation
- Response bodies
- HTTP status codes
- GET requests
- POST requests
- PUT requests
- PATCH requests
- DELETE requests
- HEAD requests
- Water operations
- BMI operations
- Calories operations
- Daily goals
- Error handling

---

### `JwtServiceTest`

Tests JWT behavior independently from the HTTP layer.

Current JWT tests include:

```text
generateToken_returnsToken
validateToken_withValidToken_returnsTrue
extractUsername_withValidToken_returnsUsername
validateToken_withInvalidToken_returnsFalse
validateToken_withTamperedToken_returnsFalse
```

These tests verify:

- Token generation
- Signature validation
- Username extraction
- Rejection of invalid tokens
- Rejection of tampered tokens

---

## 🔐 Security

The backend currently includes several security mechanisms.

### Password Security

- Passwords are hashed with BCrypt
- Raw passwords are never stored directly in Firebase
- Login uses `PasswordEncoder.matches(...)`
- Updated passwords are re-hashed before storage
- Passwords are not returned in public user response DTOs
- Passwords are not included in `User.toString()`

### JWT Authentication

- Successful login generates a signed JWT
- JWTs contain the username as the token subject
- JWTs contain issue and expiration timestamps
- Protected requests require a Bearer token
- Invalid and expired tokens are rejected
- Tokens are validated before protected controller endpoints run

### Authorization

For user-specific endpoints:

```text
JWT subject
    ==
username in request path
```

If they do not match:

```text
403 Forbidden
```

This prevents a user from using their own valid token to access another user's protected data.

### Firebase Security

- Firebase Admin SDK credentials exist only on the backend
- The Android application does not contain Firebase Admin credentials
- Firebase access is performed through backend repository implementations
- The client communicates with Firebase only indirectly through the Spring Boot API

### Sensitive Configuration

Sensitive files are intentionally excluded from the repository.

Examples:

```text
.env
application.properties
Firebase Admin SDK JSON file
```

These files must never be committed to GitHub.

Environment configuration includes the JWT secret.

The JWT secret must not be hardcoded in source code or exposed to the Android client.

---

## ⚙️ Local Configuration

Before running the server, create the required local configuration files.

Configuration may include:

```text
Firebase credentials path
Firebase database URL
JWT secret
Server configuration
```

Example environment variables:

```text
FIREBASE_URL=...
JWT_SECRET=...
```

The JWT secret must contain sufficient entropy and must remain private.

Do not place real credentials directly inside source code.

Do not upload credentials, passwords, JWT secrets, private keys, or Firebase service-account files to GitHub.

---

## ▶️ Running the Server

### Requirements

- Java
- Maven
- Internet access
- Firebase project
- Firebase Admin SDK credentials
- JWT secret configured locally

### Start from IntelliJ IDEA

1. Open the `Spring Server` directory.
2. Allow Maven to download the dependencies.
3. Add the required local Firebase and JWT configuration.
4. Run:

```text
src/main/java/org/example/CapstoneProject/Application.java
```

### Start from the Terminal

From the `Spring Server` directory:

```bash
mvn spring-boot:run
```

---

## 🧪 Running the Tests

Run:

```bash
mvn test
```

The Spring integration tests automatically start the required application context.

`UsersControllerIntegrationTest` starts an embedded server on a random port, so a manually running server is not required.

The service integration tests communicate with Firebase directly through the backend service and repository layers.

---

## 🛠 Technologies

- Java
- Spring Boot 3
- Spring Web
- Spring Security Crypto
- BCrypt
- Jakarta Bean Validation
- Maven
- REST API
- Firebase Admin SDK
- Firebase Realtime Database
- Nimbus JOSE + JWT
- CompletableFuture
- JUnit 5
- JUnit Jupiter
- Spring Boot Test
- TestRestTemplate

---

## 🏗 Key Backend Design Features

- Layered backend architecture
- Controller / Service / Repository separation
- Repository interfaces
- Dedicated Firebase repository implementations
- Constructor dependency injection
- Centralized Firebase configuration
- BCrypt password configuration
- Request DTOs
- Response DTOs
- Login-specific response DTO containing JWT
- Jakarta Bean Validation
- Global exception handling
- JWT authentication
- User-specific JWT authorization
- Stateless access-token validation
- Asynchronous Firebase operations
- Transaction-safe water updates
- Dynamic daily water logs
- Android API compatibility preserved during backend refactoring
- Automated service integration testing
- Automated controller integration testing
- Dedicated JWT unit testing

---

## 🚀 Future Improvements

- Add refresh-token support
- Add refresh-token rotation
- Add server-side token revocation
- Add explicit logout / token invalidation support
- Add dedicated `JwtAuthenticationFilter` integration tests for:
  - Missing token → `401`
  - Invalid token → `401`
  - Expired token → `401`
  - Token belonging to another user → `403`
- More detailed validation
- Additional centralized exception handling
- More detailed health statistics
- Cloud deployment
- Additional automated tests
- Additional repository abstractions if the application grows

---

## 🔗 Related Project

The Android client is located in:

```text
../Hai-Bari android application/
```

The main repository documentation is located in:

```text
../README.md
```

---

## 👨‍💻 Author

Sharbel Zarzour
