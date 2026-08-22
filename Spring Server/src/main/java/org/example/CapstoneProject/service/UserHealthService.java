package org.example.CapstoneProject.service;

import org.example.CapstoneProject.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

// -------------------------------------------------------------------------
// Contains business logic related to user health data.
//
// This service handles BMI and calorie-related operations.
// It depends on UserRepository and does not know that Firebase
// is used as the database.
// -------------------------------------------------------------------------
@Service
public class UserHealthService {

    // Repository used to access user data.
    private final UserRepository userRepository;

    // ---------------------------------------------------------------------
    // Builds the service using constructor injection.
    // ---------------------------------------------------------------------
    public UserHealthService(UserRepository userRepository) {
        // Store the injected repository.
        this.userRepository = userRepository;
    }

    // ---------------------------------------------------------------------
    // Updates the BMI value of a user.
    // ---------------------------------------------------------------------
    public CompletableFuture<Boolean> updateBmi(String username, double bmi) {
        // Delegate the database operation to the repository.
        return userRepository.updateBmi(username, bmi);
    }

    // ---------------------------------------------------------------------
    // Returns the calories value of a user.
    // ---------------------------------------------------------------------
    public CompletableFuture<Integer> getCalories(String username) {
        // Delegate the database operation to the repository.
        return userRepository.getCalories(username);
    }

    // ---------------------------------------------------------------------
    // Updates the calories value of a user.
    // ---------------------------------------------------------------------
    public CompletableFuture<Boolean> updateCalories(String username, int calories) {
        // Delegate the database operation to the repository.
        return userRepository.updateCalories(username, calories);
    }
}