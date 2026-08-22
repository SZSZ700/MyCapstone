package org.example.CapstoneProject.service;

import org.example.CapstoneProject.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

// -------------------------------------------------------------------------
// Contains business logic related to global application statistics.
//
// This service depends on UserRepository and does not know that Firebase
// is used as the database.
// -------------------------------------------------------------------------
@Service
public class StatisticsService {

    // Repository used to access user data for statistical operations.
    private final UserRepository userRepository;

    // ---------------------------------------------------------------------
    // Builds the service using constructor injection.
    // ---------------------------------------------------------------------
    public StatisticsService(UserRepository userRepository) {
        // Store the injected repository.
        this.userRepository = userRepository;
    }

    // ---------------------------------------------------------------------
    // Returns the global BMI distribution for all users.
    //
    // The result contains the number of users in each BMI category.
    // ---------------------------------------------------------------------
    public CompletableFuture<Map<String, Integer>> getBmiDistribution() {
        // Delegate the database operation to the repository.
        return userRepository.getBmiDistribution();
    }
}