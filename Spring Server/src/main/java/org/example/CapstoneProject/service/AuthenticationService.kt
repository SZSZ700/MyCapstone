@file:Suppress("PackageName")
package org.example.CapstoneProject.service
import org.example.CapstoneProject.model.User
import org.example.CapstoneProject.repository.UserRepository
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import java.util.concurrent.CompletableFuture

// -------------------------------------------------------------------------
// Contains authentication-related business logic.
//
// This service handles operations such as signup and login.
//
// It depends only on the UserRepository interface and does not depend
// on a specific database or repository implementation.
// -------------------------------------------------------------------------
@Service
class AuthenticationService(
    // Repository used to access user data.
    private val userRepository: UserRepository,

    // Password encoder used to hash and verify passwords securely.
    private val passwordEncoder: PasswordEncoder
) {

    // ---------------------------------------------------------------------
    // Creates a new user account.
    //
    // The method:
    // 1. Validates the username.
    // 2. Encodes the raw password using BCrypt.
    // 3. Sends the user to the repository for creation.
    //
    // The repository checks whether the username already exists.
    //
    // The database also has a UNIQUE username index.
    // Therefore, even if two signup requests happen at almost the same time,
    // MongoDB still prevents duplicate usernames.
    //
    // Returns:
    // - "User created successfully" when the user was created.
    // - "Username already exists" when the username is already in use.
    // - "Error: invalid username" for an invalid username.
    // ---------------------------------------------------------------------
    fun signup(user: User?): CompletableFuture<String> {
        // Reject an invalid user or username.
        if (user == null || user.userName.isNullOrBlank()) {
            return CompletableFuture.completedFuture("Error: invalid username")
        }

        // Hash the raw password using BCrypt before sending
        // the user to the repository.
        user.password = passwordEncoder.encode(user.password)

        // Ask the repository to create the user.
        return userRepository.create(user).thenApply { created ->
            if (!created) {
                "Username already exists"
            } else {
                "User created successfully"
            }
        }
    }

    // ---------------------------------------------------------------------
    // Authenticates a user using the provided username and raw password.
    //
    // The password received from the client is never compared directly
    // with the stored password hash.
    //
    // BCrypt verifies whether the supplied raw password matches
    // the encoded password stored in the database.
    //
    // Returns the authenticated user when the credentials are valid.
    // Returns null when authentication fails.
    // ---------------------------------------------------------------------
    fun login(username: String, password: String): CompletableFuture<User?> {
        // Find the single user matching the supplied username.
        return userRepository.findByUsername(username).thenApply { existingUser ->
            if (existingUser == null) { null }
            else {
                val storedPassword = existingUser.password

                // Verify that a stored password exists and matches
                // the raw password received from the client.
                if (storedPassword == null ||
                    !passwordEncoder.matches(
                        password, storedPassword)) {
                    null
                }
                // Credentials are valid.
                else { existingUser }
            }
        }
    }
}