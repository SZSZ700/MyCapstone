@file:Suppress("PackageName", "FoldInitializerAndIfToElvis")
package org.example.CapstoneProject.service
import org.example.CapstoneProject.model.User
import org.example.CapstoneProject.repository.UserRepository
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import java.util.concurrent.CompletableFuture

// -------------------------------------------------------------------------
// Contains business logic related to users.
//
// This service depends only on the UserRepository interface and does not
// depend on a specific database or repository implementation.
//
// Database-specific behavior such as transactions, indexes and document
// operations remains inside the repository implementation.
// -------------------------------------------------------------------------
@Suppress("unused")
@Service
class UserService(
    // Repository used to access user data.
    private val userRepository: UserRepository,
    // Password encoder used to prevent plaintext passwords from
    // being sent to the repository layer.
    private val passwordEncoder: PasswordEncoder
) {

    // ---------------------------------------------------------------------
    // Finds a user by username.
    //
    // Returns the user when found.
    // Returns null when no matching user exists.
    // ---------------------------------------------------------------------
    fun getUser(username: String): CompletableFuture<User?> {
        // Delegate the data operation to the repository layer.
        return userRepository.findByUsername(username)
    }

    // ---------------------------------------------------------------------
    // Returns all users stored in the database.
    // ---------------------------------------------------------------------
    fun getAllUsers(): CompletableFuture<List<User>> {
        // Delegate the data operation to the repository layer.
        return userRepository.findAll()
    }

    // ---------------------------------------------------------------------
    // Checks whether a user exists by username.
    // ---------------------------------------------------------------------
    fun exists(username: String): CompletableFuture<Boolean> {
        // Delegate the data operation to the repository layer.
        return userRepository.existsByUsername(username)
    }

    // ---------------------------------------------------------------------
    // Deletes a user by username.
    //
    // The repository implementation is responsible for deleting
    // the user and all related data safely.
    //
    // Transaction and concurrency handling remain inside the repository.
    //
    // Returns true when the user was found and deleted.
    // Returns false when no matching user exists.
    // ---------------------------------------------------------------------
    fun deleteUser(username: String): CompletableFuture<Boolean> {
        // Delegate the data operation to the repository layer.
        return userRepository.deleteByUsername(username)
    }

    // ---------------------------------------------------------------------
    // Partially updates an existing user.
    // The service validates and prepares all supported fields before
    // sending them to the repository.
    //
    // Supported fields:
    // - password
    // - fullName
    // - age
    // - bmi
    //
    // Unsupported fields are ignored.
    //
    // If the update contains a password, the raw password is encoded
    // with BCrypt before persistence.
    // ---------------------------------------------------------------------
    fun patchUser(username: String, updates: MutableMap<String, Any>): CompletableFuture<User?> {
        // Create a clean map containing only supported and validated fields.
        val repositoryUpdates = mutableMapOf<String, Any>()

        // Validate and encode the password when supplied.
        if (updates.containsKey("password")) {
            val password = updates["password"]

            if (password !is String) {
                throw IllegalArgumentException("Password must be a string")
            }

            repositoryUpdates["password"] = passwordEncoder.encode(password)
        }

        // Validate the full name when supplied.
        if (updates.containsKey("fullName")) {
            val fullName = updates["fullName"]

            if (fullName !is String) {
                throw IllegalArgumentException("Full name must be a string")
            }

            repositoryUpdates["fullName"] = fullName
        }

        // Validate and normalize the age when supplied.
        if (updates.containsKey("age")) {
            val age = updates["age"]

            if (age !is Number) {
                throw IllegalArgumentException("Age must be a number")
            }

            repositoryUpdates["age"] = age.toInt()
        }

        // Validate and normalize the BMI when supplied.
        if (updates.containsKey("bmi")) {
            val bmi = updates["bmi"]

            if (bmi !is Number) {
                throw IllegalArgumentException("BMI must be a number")
            }

            repositoryUpdates["bmi"] = bmi.toDouble()
        }

        // Delegate the prepared update to the repository.
        return userRepository.patchByUsername(username, repositoryUpdates)
    }

    // ---------------------------------------------------------------------
    // Creates a new user.
    // Validates the user data before persistence.
    // The raw password is encoded with BCrypt before the user is sent
    // to the repository.
    // This guarantees that this creation path does not store
    // plaintext passwords.
    // Returns true when the user was created successfully.
    // Returns false when the user data is invalid or the username already exists.
    // ---------------------------------------------------------------------
    fun createUser(user: User?): CompletableFuture<Boolean> {
        // Reject a missing user.
        if (user == null) {
            return CompletableFuture.completedFuture(false)
        }

        // Validate the username before calling the repository.
        val username = user.userName
        if (username.isNullOrBlank()) {
            return CompletableFuture.completedFuture(false)
        }

        // Read the raw password.
        val password = user.password

        // Reject a missing password.
        if (password == null) {
            return CompletableFuture.completedFuture(false)
        }

        // Encode the raw password before sending the user
        // to the repository layer.
        user.password = passwordEncoder.encode(password)

        // Delegate persistence to the repository.
        return userRepository.create(user)
    }
}