# 🌐 Hi-Bari Spring Boot Server

## 📌 Overview

This directory contains the backend server for the Hi-Bari health and water tracking system.

The server exposes a REST API used by the Android application, handles authentication, authorization, business logic, validation, HTTPS/TLS communication, and communicates with MongoDB through the official MongoDB Java Driver.

The backend follows a layered architecture based on controllers, services, repository interfaces, MongoDB repository implementations, security components, DTOs, configuration, and centralized exception handling.

The local development server uses HTTPS on port `8443`.

The local MongoDB environment uses a single-node replica set named:

```text
rs0
```

This enables MongoDB multi-document transactions during local development.

---

## 🧠 Server Responsibilities

The Spring Boot server is responsible for:

- User registration and login
- BCrypt password hashing and verification
- JWT generation and validation
- User-specific authorization
- HTTPS/TLS communication
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
- MongoDB communication
- MongoDB upsert operations
- Transaction-safe database updates
- Concurrency-safe multi-document operations
- Asynchronous repository execution through `CompletableFuture`
- Centralized validation error handling
- Loading local security and environment configuration

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
    │   │       │   │
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

> `.env`, `application.properties`, and `keystore.p12` contain local configuration and must be handled appropriately.

> `keystore.p12` contains the local HTTPS server private key and must never be committed publicly.

> Generated directories such as `target/` and IDE metadata are intentionally omitted from the structure above.

---

## 🧩 Main Components

### `Application.java`

The main Spring Boot application class used to start the server.

Spring Boot starts an embedded server using the HTTPS configuration defined in the local application configuration.

---

### `UsersController.java`

Defines the REST API endpoints used by the Android application.

The controller is responsible for:

- Receiving REST requests
- Reading path variables and query parameters
- Receiving and validating request DTOs
- Calling the appropriate service
- Converting service results into HTTP responses
- Returning response DTOs or dynamic map-based responses when appropriate

The controller does not communicate with MongoDB directly.

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
- Mapping duplicate username creation attempts into application-level signup results

---

### `JwtService.java`

Handles JWT creation and validation.

Responsibilities include:

- Generating signed JWT access tokens
- Adding the authenticated username as the token subject
- Adding issue and expiration timestamps
- Validating token signatures
- Validating token expiration
- Extracting the username from the token subject

JWT access tokens are signed using the configured secret.

The JWT secret is loaded from local environment configuration and is not stored directly in source code.

The JWT secret must contain at least 32 characters.

---

### `UserService.java`

Handles general user operations.

Responsibilities include:

- Retrieving users
- Creating users
- Updating users
- Patching users
- Deleting users
- Checking whether a user exists
- Encoding passwords before persistence
- Validating dynamic PATCH value types before they reach the repository

PATCH values are checked before persistence.

Expected field types include:

```text
password -> String
fullName -> String
age      -> Number
bmi      -> Number
```

For example:

```json
{
  "password": 123
}
```

is rejected instead of allowing an invalid BSON type to reach MongoDB.

---

### `WaterService.java`

Handles water-related application operations.

Responsibilities include:

- Adding water intake
- Retrieving today's and yesterday's water
- Retrieving water history
- Retrieving weekly averages
- Managing daily water goals

Database transactions and MongoDB-specific concurrency behavior remain inside the repository implementation.

---

### `UserHealthService.java`

Handles user health-related data.

Responsibilities include:

- Updating BMI
- Retrieving calories
- Updating calories

---

### `StatisticsService.java`

Handles global statistical operations.

Responsibilities include:

- Retrieving BMI distribution statistics

Current BMI categories are:

```text
Underweight: BMI < 18.5
Normal:      18.5 <= BMI < 25
Overweight:  25 <= BMI < 30
Obese:       BMI >= 30
```

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

Public endpoints include:

```text
GET  /api/users/health
POST /api/users/signup
POST /api/users/login
GET  /api/users/stats/bmiDistribution
```

The JWT access token itself is stateless and is not stored in MongoDB.

---

## 🔑 Password Security

### `PasswordConfiguration.java`

Provides the shared BCrypt password encoder as a Spring bean.

Password creation works as follows:

```text
Signup
    ↓
Raw password received through HTTPS
    ↓
BCrypt encoding
    ↓
BCrypt hash sent to repository
    ↓
passwordHash stored in MongoDB
```

Login works as follows:

```text
Raw login password received through HTTPS
    ↓
PasswordEncoder.matches(...)
    ↓
Stored BCrypt hash
    ↓
Authentication succeeds or fails
```

The raw password is never intentionally stored directly in MongoDB.

Password updates through PUT or PATCH are also encoded before persistence.

BCrypt protects passwords at rest, while HTTPS protects passwords while they travel between the Android client and backend.

---

## 🔒 HTTPS / TLS

The Spring Boot backend uses HTTPS for communication with the Android application.

The local development server listens on:

```text
8443
```

The local server base URL is:

```text
https://localhost:8443/myapp/api/users
```

The Android Emulator accesses the same host machine through:

```text
https://10.0.2.2:8443/myapp/api/users
```

### Local Development Certificate

The local HTTPS setup uses a self-signed development certificate.

The certificate includes Subject Alternative Names for:

```text
localhost
127.0.0.1
10.0.2.2
```

These entries allow TLS hostname verification for the addresses used during local development.

---

### PKCS#12 Keystore

Spring Boot loads its HTTPS identity from:

```text
src/main/resources/keystore.p12
```

The PKCS#12 keystore contains:

```text
Server certificate
Public key
Private key
```

The private key must remain private.

It must never be:

- Uploaded publicly
- Included in the Android application
- Shared publicly
- Included in documentation
- Logged
- Sent to clients

The keystore should be excluded from version control.

---

### Public Certificate

The public certificate is exported separately from the server keystore.

The Android application uses:

```text
hibari_local.crt
```

This certificate contains public certificate information only.

It does not contain the server private key.

The Android application uses this certificate as a local trust anchor so that it can verify the self-signed development server.

Conceptually:

```text
Spring Boot Server

keystore.p12
    │
    ├── Certificate
    ├── Public Key
    └── Private Key 🔐
          │
          │ Public certificate exported
          ▼
hibari_local.crt
          │
          ▼
Android Application
```

The server keeps the private key.

The Android application receives only the public certificate.

---

### TLS Responsibilities

HTTPS/TLS provides:

- Encryption of network traffic
- Server identity verification
- Protection against passive network inspection
- Protection of login passwords while in transit
- Protection of JWT Bearer tokens while in transit
- Integrity protection for API traffic

HTTPS complements the other security mechanisms:

```text
BCrypt
    ↓
Protects stored passwords

JWT
    ↓
Authenticates and authorizes API requests

HTTPS / TLS
    ↓
Protects communication in transit
```

---

## 🗄 Repository Layer

The repository layer separates persistence operations from the service layer.

### Repository Interfaces

```text
UserRepository
WaterRepository
```

These interfaces define the persistence operations required by the application without exposing MongoDB-specific implementation details.

---

### MongoDB Repository Implementations

```text
MongoUserRepository
MongoWaterRepository
```

These classes implement the repository interfaces using the synchronous MongoDB Java Driver.

Responsibilities include:

- Reading data from MongoDB
- Creating users
- Updating users
- Deleting users
- Querying users by username
- Updating BMI
- Reading and updating calories
- Reading and inserting water records
- Managing daily water goals
- Calculating water-related results
- Performing MongoDB transactions
- Performing MongoDB upserts
- Protecting multi-document operations from concurrency races
- Converting MongoDB `Document` objects into application models
- Wrapping synchronous database work with `CompletableFuture.supplyAsync(...)`

The service layer depends on repository interfaces instead of depending directly on MongoDB-specific classes.

---

## ⚙️ Configuration

### `MongoConfiguration.java`

Creates and exposes the MongoDB dependencies used by the repositories.

Responsibilities include:

- Creating the shared `MongoClient`
- Creating the shared `MongoDatabase`
- Reading MongoDB configuration through `EnvConfig`
- Allowing repositories to receive MongoDB dependencies through constructor injection

Conceptually:

```text
.env
    ↓
EnvConfig
    ↓
MongoConfiguration
    ↓
MongoClient
MongoDatabase
    ↓
MongoDB Repositories
```

---

### `PasswordConfiguration.java`

Provides the BCrypt `PasswordEncoder` bean used by authentication and user-update operations.

---

### `EnvConfig.java`

Loads environment-specific configuration such as:

```text
JWT_SECRET
mongodb.uri
mongodb.database
```

Example:

```env
JWT_SECRET=<LOCAL_SECRET>
mongodb.uri=mongodb://localhost:27017/?replicaSet=rs0
mongodb.database=hibari_db
```

The JWT secret and MongoDB connection settings are loaded from environment configuration rather than hardcoded in source code.

---

### `application.properties`

Contains local Spring Boot configuration including:

- Application context path
- Server port
- HTTPS enablement
- PKCS#12 keystore location
- Keystore type
- Keystore alias
- Local keystore credentials
- Logging configuration

The local development HTTPS configuration uses:

```properties
server.servlet.context-path=/myapp

server.port=8443
server.ssl.enabled=true
server.ssl.key-store=classpath:keystore.p12
server.ssl.key-store-type=PKCS12
server.ssl.key-store-password=<LOCAL_KEYSTORE_PASSWORD>
server.ssl.key-alias=hibari-local
```

Real secrets should not be published in documentation or committed publicly.

For production deployment, HTTPS credentials should be supplied through an appropriate secure configuration mechanism.

---

## 🍃 MongoDB Database

The backend uses MongoDB as its persistence database.

The local database is:

```text
hibari_db
```

The project uses the official synchronous MongoDB Java Driver.

The current database contains four main collections:

```text
users
water_records
calories
goals
```

MongoDB does not provide SQL-style foreign keys.

Relationships are represented using MongoDB `ObjectId` values.

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

Fields:

```text
_id
username
passwordHash
fullName
age
bmi
transactionVersion
```

`username` identifies the user at the application level.

`passwordHash` stores the BCrypt password hash.

`transactionVersion` is used as a shared write point for transaction-based concurrency protection.

---

### `water_records`

Each water drink becomes a separate MongoDB document.

Example:

```json
{
  "_id": "ObjectId(...)",
  "userId": "ObjectId(...)",
  "amountMl": 500,
  "recordedAt": "BSON Date"
}
```

This structure supports:

- Individual drink history
- Daily water totals
- Historical water retrieval
- Weekly average calculations

There is no fixed number of drink records per day.

---

### `calories`

Daily calories are stored per user and date.

Example:

```json
{
  "_id": "ObjectId(...)",
  "userId": "ObjectId(...)",
  "calories": 2400,
  "recordDate": "2026-09-13"
}
```

Only one calorie document is allowed per user and date.

Today's value is written using MongoDB upsert.

---

### `goals`

Daily water goals are stored per user and date.

Example:

```json
{
  "_id": "ObjectId(...)",
  "userId": "ObjectId(...)",
  "goalMl": 3000,
  "recordDate": "2026-09-13"
}
```

Older goal records remain stored as history.

Only one goal document is allowed per user and date.

Today's goal is written using MongoDB upsert.

---

## 🔍 MongoDB Indexes

The database uses indexes for both performance and data integrity.

### Unique Username Index

```javascript
db.users.createIndex(
    { username: 1 },
    { unique: true }
)
```

This guarantees that duplicate usernames cannot be stored.

The index also protects against concurrent signup requests.

---

### Water History Index

```javascript
db.water_records.createIndex(
    { userId: 1, recordedAt: 1 }
)
```

This supports queries that retrieve water records for a user across a time range.

---

### Calories Unique Index

```javascript
db.calories.createIndex(
    { userId: 1, recordDate: 1 },
    { unique: true }
)
```

This guarantees at most one calorie document per user and date.

---

### Goals Unique Index

```javascript
db.goals.createIndex(
    { userId: 1, recordDate: 1 },
    { unique: true }
)
```

This guarantees at most one goal document per user and date.

---

## 🔄 MongoDB Transactions

Several operations require multiple MongoDB commands to behave as one logical unit.

Transaction-based operations include:

```text
deleteByUsername()
updateWater()
updateCalories()
updateGoalMl()
```

A MongoDB client session is created using:

```java
mongoClient.startSession()
```

The repositories use MongoDB's convenient transaction API:

```java
session.withTransaction(() -> {
    // MongoDB operations
    return true;
});
```

Every operation belonging to the transaction must receive the same `ClientSession`.

For example:

```java
waterRecords.insertOne(
        session,
        document
);
```

---

## 🔒 Transaction Concurrency Protection

Transactions alone do not act like application-level SQL row locks.

Hi-Bari uses a shared write to the user's document to coordinate concurrent operations affecting the same user.

The `users` document contains:

```text
transactionVersion
```

Transaction-based operations resolve the user through a helper conceptually equivalent to:

```javascript
db.users.findOneAndUpdate(
    {
        username: username
    },
    {
        $inc: {
            transactionVersion: 1
        }
    }
)
```

This makes transaction-based operations affecting the same user compete on the same MongoDB document.

Example race:

```text
Transaction A
Delete user

Transaction B
Insert water for the same user
```

Both operations write to the same user's `transactionVersion`.

MongoDB therefore detects the conflicting concurrent write instead of silently allowing inconsistent related data to be created.

---

## 🔁 `withTransaction(...)` Retry Behavior

The repository uses:

```java
session.withTransaction(...)
```

instead of manually managing only:

```java
session.startTransaction();
session.commitTransaction();
session.abortTransaction();
```

The convenient transaction API manages:

- Transaction start
- Commit
- Abort when appropriate
- Retry behavior for eligible transient transaction failures
- Retry handling around uncertain commit results

This is useful when concurrent operations temporarily conflict.

---

## 🔄 Delete User Transaction

Deleting a user affects several collections.

Conceptually:

```text
Start transaction
        ↓
Find and lock user
        ↓
Delete calories
        ↓
Delete goals
        ↓
Delete water records
        ↓
Delete user document
        ↓
Commit
```

MongoDB operations are conceptually equivalent to:

```javascript
db.calories.deleteMany({
    userId: userId
})

db.goals.deleteMany({
    userId: userId
})

db.water_records.deleteMany({
    userId: userId
})

db.users.deleteOne({
    _id: userId
})
```

The transaction prevents partial deletion.

The intended result is either:

```text
User + all related data deleted
```

or:

```text
Transaction does not commit successfully
```

This prevents orphaned related data caused by partially completed delete workflows.

---

## 🔄 Water Update Flow

When the Android application sends a request to add water:

1. Android creates an HTTPS request.
2. The JWT is included in the `Authorization` header.
3. TLS encrypts the request while it travels to the backend.
4. `JwtAuthenticationFilter` validates the token.
5. The filter verifies that the token subject matches the username in the URL.
6. The request reaches `UsersController`.
7. The controller reads the username and water amount.
8. The controller calls `WaterService`.
9. `WaterService` delegates the persistence operation to `WaterRepository`.
10. `MongoWaterRepository` opens a MongoDB client session.
11. `withTransaction(...)` starts the transactional workflow.
12. The user's `transactionVersion` is incremented.
13. A new `water_records` document is inserted.
14. The transaction is committed.
15. The result returns through the repository and service layers.
16. The controller returns the result through HTTPS.

```text
HTTPS PATCH Request
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
MongoWaterRepository
      ↓
MongoDB Transaction
      ↓
users.transactionVersion increment
      ↓
water_records.insertOne(...)
      ↓
Commit
      ↓
HTTPS Response
```

Each drink remains a separate MongoDB document.

---

## 🔄 MongoDB Upsert

Calories and water goals use MongoDB `upsert`.

An upsert behaves as:

```text
Matching document exists
        ↓
Update it

Matching document does not exist
        ↓
Insert it
```

---

### Calories Upsert

Conceptually:

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

This replaces the previous multi-step pattern:

```text
find
 ↓
document exists?
 ↓
insert or update
```

with one atomic MongoDB command.

---

### Goal Upsert

Conceptually:

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

Older goal documents remain stored because each date has its own document.

---

### Why Water Does Not Use Upsert

Water intake is intentionally stored as individual events.

For example:

```text
08:00 → 250 ml
10:30 → 500 ml
13:00 → 300 ml
```

These must remain three separate documents.

Therefore water uses:

```javascript
db.water_records.insertOne(...)
```

rather than upsert.

---

## 🧩 MongoDB Replica Set

MongoDB multi-document transactions require a replica set.

For local development, the project uses a single-node replica set.

Replica set name:

```text
rs0
```

MongoDB configuration:

```yaml
replication:
  replSetName: rs0
```

The replica set was initialized through MongoDB Shell using:

```javascript
rs.initiate()
```

A healthy local configuration reports:

```text
stateStr: PRIMARY
```

The Spring Boot MongoDB connection URI includes:

```text
?replicaSet=rs0
```

Full local URI:

```text
mongodb://localhost:27017/?replicaSet=rs0
```

The MongoDB Java Driver then detects:

```text
REPLICA_SET_PRIMARY
```

instead of:

```text
STANDALONE
```

---

## ⚡ Asynchronous Repository Operations

The MongoDB Java Driver used by the project is synchronous.

Repository methods preserve the application's asynchronous contracts by wrapping blocking database work with:

```java
CompletableFuture.supplyAsync(...)
```

This allows service and controller methods to continue using asynchronous chains such as:

```java
thenApply(...)
thenCompose(...)
```

Controller methods can therefore return:

```java
CompletableFuture<ResponseEntity<...>>
```

while MongoDB-specific blocking operations remain inside the repository layer.

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

Using DTOs prevents the REST layer from unnecessarily exposing the internal persistence model.

It also allows persistence details to change while keeping the Android API contract stable.

---

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

Validation is used for typed request DTOs such as:

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

### Dynamic PATCH Validation

PATCH requests use dynamic values and therefore require explicit type checking.

Expected types include:

```text
password -> String
fullName -> String
age      -> Number
bmi      -> Number
```

Example invalid request:

```json
{
  "age": "twenty"
}
```

The service rejects the invalid type before it reaches the repository.

This prevents errors such as:

```java
((Number) updates.get("age")).intValue()
```

being executed against an invalid String value.

It also prevents MongoDB from storing fields with incorrect BSON types.

---

## ⚠️ Global Exception Handling

### `GlobalExceptionHandler.java`

Exceptions are handled centrally using:

```java
@RestControllerAdvice
```

The exception handler processes validation failures such as:

```text
MethodArgumentNotValidException
```

and manually detected invalid request values such as:

```text
IllegalArgumentException
```

Typical result:

```text
Invalid client input
→ HTTP 400 Bad Request
```

This prevents individual controller methods from implementing duplicated error-handling code.

---

## 🧠 Architecture

The backend request flow for protected endpoints is:

```text
Android Application
        ↓
HTTPS / TLS
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
MongoDB Repository Implementation
        ↓
MongoDB
        ↓
HTTPS Response
        ↓
Android Application
```

Example authentication flow:

```text
Android Application
        ↓
HTTPS POST /api/users/login
        ↓
UsersController
        ↓
AuthenticationService
        ↓
UserRepository
        ↓
MongoUserRepository
        ↓
MongoDB users collection
        ↓
BCrypt password verification
        ↓
JwtService
        ↓
JWT generated
        ↓
HTTPS response
        ↓
Android Application
```

Example user request flow:

```text
Android
    ↓
HTTPS
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
MongoUserRepository
    ↓
MongoDB
```

Example water flow:

```text
Android
    ↓
HTTPS
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
MongoWaterRepository
    ↓
MongoDB Transaction
```

This architecture separates:

```text
TLS transport security
Authentication and authorization
REST handling
Business/application logic
Persistence contracts
MongoDB-specific implementation
Database configuration
REST request/response models
```

---

## 🌐 REST API

The controller base path is:

```text
/api/users
```

The application context path is:

```text
/myapp
```

The local HTTPS server base URL is:

```text
https://localhost:8443/myapp/api/users
```

For the Android Emulator:

```text
https://10.0.2.2:8443/myapp/api/users
```

`10.0.2.2` is a special Android Emulator address that maps to the host development machine.

---

### Public Endpoints

These endpoints do not require a JWT:

```text
GET     /health
POST    /signup
POST    /login
GET     /stats/bmiDistribution
```

Example health endpoint:

```text
https://localhost:8443/myapp/api/users/health
```

---

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

The server includes automated tests for the service/repository architecture, REST controller layer, MongoDB transactions, concurrency behavior, BCrypt behavior, and JWT functionality.

Testing technologies include:

- JUnit 5
- JUnit Jupiter
- Spring Boot Test
- TestRestTemplate
- MongoDB integration testing
- Transaction testing
- Concurrency testing
- Asynchronous operation testing
- JWT unit testing

The backend contains three main test classes:

```text
CapstoneServicesIntegrationTest
UsersControllerIntegrationTest
JwtServiceTest
```

---

### `CapstoneServicesIntegrationTest`

Tests the backend service/repository flow against the real MongoDB-backed repository implementation.

The test class works directly with services such as:

```text
UserService
AuthenticationService
WaterService
UserHealthService
StatisticsService
```

Because these tests call services directly rather than going through the protected REST API, JWT authentication is not required for those service calls.

Tests cover operations such as:

- Creating users
- Retrieving users
- Duplicate username handling
- Concurrent signup behavior
- BCrypt password storage
- Authentication behavior
- Correct and incorrect login credentials
- Updating user information
- Password update hashing
- PATCH operations
- Updating BMI
- Updating calories
- Updating water consumption
- Retrieving water history
- Retrieving weekly averages
- Managing daily goals
- BMI distribution
- MongoDB transactions
- MongoDB upsert behavior
- `transactionVersion` changes
- Cascade deletion
- Delete-vs-write concurrency
- Orphan-data prevention
- Asynchronous repository operations

---

### `UsersControllerIntegrationTest`

Loads the Spring Boot application and sends REST requests using `TestRestTemplate`.

The test server uses a random embedded port.

The Spring Boot server does not need to be started manually before running these integration tests.

Protected requests use real JWTs generated by `JwtService`.

Tests cover:

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

Tests JWT behavior independently from the controller layer.

Current JWT tests verify areas such as:

```text
Token generation
Valid token acceptance
Username extraction
Invalid-token rejection
Expired-token rejection
Tampered-token rejection
```

JWT tests do not require MongoDB.

---

## 🔐 Security

The backend includes multiple security mechanisms.

### Password Security

- Passwords are hashed with BCrypt
- Raw passwords are not intentionally stored directly in MongoDB
- Login uses `PasswordEncoder.matches(...)`
- Updated passwords are re-hashed before storage
- Passwords are not returned in public user response DTOs
- Passwords are not included in `User.toString()`

---

### JWT Authentication

- Successful login generates a signed JWT
- JWTs contain the username as the token subject
- JWTs contain issue and expiration timestamps
- Protected requests require a Bearer token
- Invalid and expired tokens are rejected
- Tokens are validated before protected controller endpoints run
- JWTs are not stored in MongoDB

---

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

---

### HTTPS / TLS Security

- Android-to-backend communication uses HTTPS
- Login passwords travel through encrypted TLS connections
- JWT Bearer tokens travel through encrypted TLS connections
- The server uses a PKCS#12 keystore
- The server private key remains on the backend
- The Android application receives only the public development certificate
- The local development certificate includes SAN entries for development addresses
- Cleartext communication is not used for the real Android backend connection

The current certificate is intended only for local development.

A production deployment should use a certificate issued by a trusted Certificate Authority for the deployed domain.

---

### MongoDB Security and Integrity

The current local development MongoDB environment provides data-integrity mechanisms including:

- Unique username index
- Unique daily calorie index
- Unique daily goal index
- MongoDB transactions
- Shared user-document concurrency writes
- Transaction retries through `withTransaction(...)`
- Cascade deletion
- Application-side input validation

The local MongoDB installation currently runs as a development environment.

A production MongoDB deployment should additionally use:

- Database authentication
- Appropriate user roles
- Restricted network access
- Secure credentials
- TLS where appropriate
- Production replica-set deployment
- External secret management

---

### Sensitive Configuration

Sensitive files and values should be excluded from public repositories.

Examples:

```text
.env
application.properties
local.properties
*.jks
*.keystore
*.p12
*.pfx
```

Environment configuration includes:

```text
JWT_SECRET
mongodb.uri
mongodb.database
```

The JWT secret must not be hardcoded in source code or exposed to the Android client.

The server's PKCS#12 keystore contains a private key and must never be committed publicly.

The exported public certificate does not contain the server private key and may be distributed to the Android development client when required for local trust configuration.

---

## ⚙️ Local Configuration

Before running the server, create the required local configuration.

### `.env`

Example:

```env
JWT_SECRET=<SECRET_WITH_AT_LEAST_32_CHARACTERS>
mongodb.uri=mongodb://localhost:27017/?replicaSet=rs0
mongodb.database=hibari_db
```

Do not upload the real JWT secret.

---

### MongoDB Configuration

The local MongoDB configuration must enable the development replica set:

```yaml
replication:
  replSetName: rs0
```

After MongoDB starts with replication enabled, initialize the replica set once through `mongosh`:

```javascript
rs.initiate()
```

Verify it using:

```javascript
rs.status()
```

The local member should become:

```text
PRIMARY
```

---

### Spring Boot HTTPS Configuration

Example local configuration:

```properties
server.servlet.context-path=/myapp

server.port=8443
server.ssl.enabled=true
server.ssl.key-store=classpath:keystore.p12
server.ssl.key-store-type=PKCS12
server.ssl.key-store-password=<LOCAL_PASSWORD>
server.ssl.key-alias=hibari-local
```

Do not place production credentials directly inside source code.

Do not upload:

```text
Passwords
JWT secrets
Private keys
PKCS#12 keystores
Production MongoDB credentials
```

to a public repository.

---

## ▶️ Running the Server

### Requirements

- Java 23
- Maven
- MongoDB 8
- MongoDB Shell (`mongosh`) for local administration
- MongoDB local replica set `rs0`
- JWT secret configured locally
- Local PKCS#12 HTTPS keystore
- Local Spring Boot HTTPS configuration

---

### Start MongoDB

Make sure the MongoDB Windows service is running.

The MongoDB configuration should contain:

```yaml
replication:
  replSetName: rs0
```

Verify the local replica set:

```bash
mongosh
```

Then:

```javascript
rs.status()
```

Expected state:

```text
PRIMARY
```

---

### Start from IntelliJ IDEA

1. Open the `Spring Server` directory.
2. Allow Maven to download dependencies.
3. Ensure `.env` contains MongoDB and JWT configuration.
4. Ensure MongoDB is running as replica set `rs0`.
5. Ensure `keystore.p12` exists under:

```text
src/main/resources/
```

6. Run:

```text
src/main/java/org/example/CapstoneProject/Application.java
```

The server starts locally on:

```text
https://localhost:8443
```

with application context:

```text
/myapp
```

---

### Start from the Terminal

From the `Spring Server` directory:

```bash
mvn spring-boot:run
```

Example health endpoint:

```text
https://localhost:8443/myapp/api/users/health
```

Because the local development certificate is self-signed, a local `curl` test may require:

```bash
curl -k https://localhost:8443/myapp/api/users/health
```

Expected response:

```text
OK
```

The `-k` option disables certificate trust verification for that specific `curl` command.

It is intended only for local command-line testing with the self-signed development certificate.

The Android application does not use an insecure trust-all TLS configuration.

Instead, Android explicitly trusts the configured local development certificate.

---

## 🧪 Running the Tests

Run:

```bash
mvn test
```

The Spring integration tests automatically start the required application context.

`UsersControllerIntegrationTest` starts an embedded server on a random port, so a manually running Spring Boot server is not required.

MongoDB must still be running because integration tests use the real MongoDB repository implementations.

The local MongoDB replica set must be available for transaction-related tests.

`CapstoneServicesIntegrationTest` communicates with MongoDB through the service and repository layers.

`JwtServiceTest` tests JWT functionality independently from MongoDB and the controller layer.

---

## 🛠 Technologies

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
- MongoDB 8
- MongoDB Java Driver
- BSON
- MongoDB transactions
- MongoDB upsert
- MongoDB replica set
- Nimbus JOSE + JWT
- CompletableFuture
- JUnit 5
- JUnit Jupiter
- Spring Boot Test
- TestRestTemplate
- Java Keytool
- MongoDB Shell (`mongosh`)

---

## 🏗 Key Backend Design Features

- Layered backend architecture
- Controller / Service / Repository separation
- Repository interfaces
- Dedicated MongoDB repository implementations
- Constructor dependency injection
- Centralized MongoDB configuration
- Shared `MongoClient`
- Shared `MongoDatabase`
- BCrypt password configuration
- Request DTOs
- Response DTOs
- Login-specific response DTO containing JWT
- Jakarta Bean Validation
- Manual PATCH type validation
- Global exception handling
- JWT authentication
- User-specific JWT authorization
- Stateless access-token validation
- HTTPS/TLS communication
- PKCS#12 server keystore
- Self-signed development certificate
- Private server key excluded from version control
- Android-compatible local certificate trust
- Synchronous MongoDB Java Driver
- `CompletableFuture` repository execution
- MongoDB multi-document transactions
- `withTransaction(...)` retry behavior
- Shared `transactionVersion` concurrency mechanism
- MongoDB upserts
- Compound indexes
- Unique indexes
- Transaction-safe water updates
- Transaction-safe calorie updates
- Transaction-safe goal updates
- Transaction-safe user deletion
- Cascade deletion
- Individual water-record document model
- Daily calorie history
- Daily goal history
- Single-node local MongoDB replica set
- Concurrent signup protection
- Concurrency integration testing
- Orphan-data prevention testing
- Android API compatibility preserved during database migration
- Automated service integration testing
- Automated controller integration testing
- Dedicated JWT unit testing

---

## 🔐 Security Layers Summary

The project uses different security mechanisms for different purposes:

```text
Raw Password
      ↓
HTTPS / TLS
Protects password during network transport
      ↓
Spring Boot
      ↓
BCrypt
Protects stored password
      ↓
MongoDB
```

After authentication:

```text
Successful Login
      ↓
JWT Generated
      ↓
HTTPS / TLS
      ↓
JWT returned to Android
      ↓
Android stores JWT
      ↓
Authorization: Bearer <JWT>
      ↓
HTTPS / TLS
      ↓
JwtAuthenticationFilter
      ↓
Authorized REST Request
```

Database integrity:

```text
Repository Operation
      ↓
MongoDB Transaction
      ↓
transactionVersion shared write
      ↓
Related MongoDB operation
      ↓
Commit
```

Each layer solves a different problem:

```text
BCrypt
→ Password storage security

JWT
→ Authentication and authorization

HTTPS / TLS
→ Network transport security

Repository abstraction
→ Persistence-layer separation

MongoDB indexes
→ Query performance and uniqueness

MongoDB transactions
→ Multi-document consistency

transactionVersion
→ Shared concurrency point

withTransaction(...)
→ Retry handling for eligible transient transaction failures
```

---

## 🔑 Local Certificate Design Summary

The local development certificate setup consists of two related components.

### Server Side

```text
keystore.p12
```

Contains:

```text
Certificate
Public key
Private key
```

Used by:

```text
Spring Boot
```

Must remain private.

---

### Android Development Side

```text
hibari_local.crt
```

Contains the public certificate.

Used by Android to trust the self-signed development server.

Does not contain the server private key.

---

### Trust Relationship

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
    │ trusted by Android
    ▼
Verified HTTPS connection
```

The private key never leaves the Spring Boot server environment.

---

## 🚀 Future Improvements

- Add refresh-token support
- Add refresh-token rotation
- Add server-side token revocation
- Add explicit logout / token invalidation support
- Move production HTTPS keystore credentials to dedicated secure environment configuration
- Use a trusted CA-issued certificate for public deployment
- Move production private-key management outside the application package
- Add production MongoDB authentication and authorization
- Deploy MongoDB as a production replica set rather than the local single-node development setup
- Store production MongoDB credentials through secure external secret management
- Add additional dedicated `JwtAuthenticationFilter` integration tests
- More detailed validation
- Additional centralized exception handling
- More detailed health statistics
- Cloud deployment
- Additional automated tests
- Additional repository abstractions if the application grows
- Dedicated production database migration and backup procedures

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
