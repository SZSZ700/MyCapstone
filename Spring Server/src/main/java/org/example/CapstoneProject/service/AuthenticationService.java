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
// It depends on UserRepository and does not know that Firebase is used
// as the database.
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
    public AuthenticationService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder) {
        // Store the injected repository.
        this.userRepository = userRepository;
        // Store the injected password encoder.
        this.passwordEncoder = passwordEncoder;
    }

    public CompletableFuture<String> signup(User user) {
        // Extract the username safely.
        var username = user != null ? user.getUserName() : null;

        // Reject invalid usernames.
        if (username == null || username.isBlank()) {
            return CompletableFuture.completedFuture("Error: invalid username");
        }

        // Query users by username.
        return userRepository.findByUsername(username).thenCompose(existingUser -> {
                    // Username already exists.
                    if (existingUser != null) {
                        return CompletableFuture.completedFuture(
                                "Username already exists"
                        );
                    }

                    // Hash the raw password using BCrypt before storing it.
                    // The original password is replaced with the generated
                    // hash, so Firebase will never receive the raw password.
                    user.setPassword(passwordEncoder.encode(user.getPassword()));

                    // Store the user with the hashed password.
                    return userRepository.insert(user);
                });
    }

    // ---------------------------------------------------------------------
    // Authenticates a user using the provided username and raw password.
    //
    // The password received from the client is never compared directly
    // with the stored password hash.
    //
    // BCrypt checks whether the raw password matches the encoded password
    // stored in Firebase.
    // ---------------------------------------------------------------------
    public CompletableFuture<User> login(String username, String password) {
        // Get all users that match the provided username.
        return userRepository.findAllByUsername(username)
                .thenApply(users -> {
                    // Loop over all matching users.
                    for (User existingUser : users) {

                        // Skip invalid user records.
                        if (existingUser == null ||
                                existingUser.getPassword() == null) { continue; }

                        // Compare the raw password received from the client
                        // with the BCrypt hash stored in Firebase.
                        if (passwordEncoder.matches(password, existingUser.getPassword())) {
                            // Return the authenticated user when the
                            // password matches.
                            return existingUser;
                        }
                    }

                    // Return null when no matching user was found.
                    return null;
                });
    }


}