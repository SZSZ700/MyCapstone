package org.example.CapstoneProject.service;

import org.example.CapstoneProject.model.User;
import org.example.CapstoneProject.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

// -------------------------------------------------------------------------
// Contains authentication-related business logic.
//
// This service handles operations such as signup and login.
//
// It depends only on the UserRepository interface and does not depend
// on a specific database or repository implementation.
// -------------------------------------------------------------------------
@Service
public class AuthenticationService {

    // Repository used to access user data.
    private final UserRepository userRepository;

    // Password encoder used to hash and verify passwords securely.
    private final PasswordEncoder passwordEncoder;

    // ---------------------------------------------------------------------
    // Builds the service using constructor injection.
    // ---------------------------------------------------------------------
    public AuthenticationService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        // Store the injected repository.
        this.userRepository = userRepository;
        // Store the injected password encoder.
        this.passwordEncoder = passwordEncoder;
    }

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
    // Returns:
    // - "User created successfully" when the user was created.
    // - "Username already exists" when the username is already in use.
    // - "Error: invalid username" for an invalid username.
    // ---------------------------------------------------------------------
    public CompletableFuture<String> signup(User user) {
        // Extract the username safely.
        var username = user != null ? user.getUserName() : null;

        // Reject invalid usernames.
        if (username == null || username.isBlank()) {
            return CompletableFuture.completedFuture("Error: invalid username");
        }

        // Hash the raw password using BCrypt before sending
        // the user to the repository.
        user.setPassword(passwordEncoder.encode(user.getPassword()));

        // Ask the repository to create the user.
        return userRepository.create(user).thenApply(created -> {
            // The username already exists.
            if (!created) {
                return "Username already exists";
            }

            return "User created successfully";
        });
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
    public CompletableFuture<User> login(String username, String password) {
        // Get all users that match the provided username.
        //
        // With the UNIQUE username index, this normally contains
        // either zero users or one user.
        return userRepository.findAllByUsername(username).thenApply(users -> {

            // Loop over all matching users.
            for (User existingUser : users) {

                // Skip invalid user records.
                if (existingUser == null || existingUser.getPassword() == null) { continue; }

                // Compare the raw password received from the client
                // with the stored BCrypt password hash.
                if (passwordEncoder.matches(password, existingUser.getPassword())) {

                    // Return the authenticated user when the password matches.
                    return existingUser;
                }
            }

            // Return null when no matching authenticated user was found.
            return null;
        });
    }
}