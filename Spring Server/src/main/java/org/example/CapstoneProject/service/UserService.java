package org.example.CapstoneProject.service;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.example.CapstoneProject.model.User;
import org.example.CapstoneProject.repository.UserRepository;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

// -------------------------------------------------------------------------
// Contains business logic related to users.
//
// This service depends on the UserRepository interface and does not know
// that Firebase is used as the database.
// -------------------------------------------------------------------------
@SuppressWarnings("unused")
@Service
public class UserService {

    // Repository used to access user data.
    private final UserRepository userRepository;

    private final PasswordEncoder passwordEncoder;

    // ---------------------------------------------------------------------
    // Builds the service using constructor injection.
    // ---------------------------------------------------------------------
    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        // Store the injected repository.
        this.userRepository = userRepository;
        // Store the password encoder dependency.
        this.passwordEncoder = passwordEncoder;
    }

    // ---------------------------------------------------------------------
    // Finds a user by username.
    //
    // Returns the user when found.
    // Returns null when no matching user exists.
    // ---------------------------------------------------------------------
    public CompletableFuture<User> getUser(String username) {
        // Delegate the database operation to the repository.
        return userRepository.findByUsername(username);
    }

    // ---------------------------------------------------------------------
    // Returns all users stored in the database.
    // ---------------------------------------------------------------------
    public CompletableFuture<List<User>> getAllUsers() {
        // Delegate the database operation to the repository.
        return userRepository.findAll();
    }

    // ---------------------------------------------------------------------
    // Checks whether a user exists by username.
    // ---------------------------------------------------------------------
    public CompletableFuture<Boolean> exists(String username) {
        // Delegate the database operation to the repository.
        return userRepository.existsByUsername(username);
    }

    // ---------------------------------------------------------------------
    // Deletes a user by username.
    //
    // Returns true when the user was found and deleted.
    // Returns false when no matching user exists.
    // ---------------------------------------------------------------------
    public CompletableFuture<Boolean> deleteUser(String username) {
        // Delegate the database operation to the repository.
        return userRepository.deleteByUsername(username);
    }

    // ---------------------------------------------------------------------
    // Updates the editable fields of an existing user.
    //
    // Before updating the user, the raw password received from the client
    // is encoded with BCrypt so a plaintext password is never stored
    // in Firebase.
    // ---------------------------------------------------------------------
    public CompletableFuture<User> updateUser(
            String username,
            User updatedUser) {

        // Encode the raw password before sending the user
        // to the repository layer.
        updatedUser.setPassword(
                passwordEncoder.encode(
                        updatedUser.getPassword()
                )
        );

        // Update the user in the repository and return
        // the complete updated user.
        return userRepository.updateByUsername(
                username,
                updatedUser
        );
    }


    // ---------------------------------------------------------------------
    // Partially updates an existing user.
    //
    // If the update contains a password, the raw password is encoded
    // with BCrypt before the data is sent to the repository.
    //
    // Other fields are passed to the repository without modification.
    // ---------------------------------------------------------------------
    public CompletableFuture<User> patchUser(String username,
            Map<String, Object> updates) {

        // Check whether the PATCH request contains a password.
        if (updates.containsKey("password")) {

            // Read the password value from the updates map.
            var passwordValue = updates.get("password");

            // Encode the password only when it is a valid String.
            if (passwordValue instanceof String password) {

                // Replace the raw password with its BCrypt hash.
                updates.put(
                        "password",
                        passwordEncoder.encode(password)
                );
            }
        }

        // Send the updated fields to the repository.
        return userRepository.patchByUsername(
                username,
                updates
        );
    }

    // ---------------------------------------------------------------------
    // Creates a new user.
    //
    // Returns true when the user was created successfully.
    // Returns false when the username is invalid or already exists.
    // ---------------------------------------------------------------------
    public CompletableFuture<Boolean> createUser(User user) {
        // Delegate the database operation to the repository.
        return userRepository.create(user);
    }
}