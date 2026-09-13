package org.example.CapstoneProject.service;

import org.example.CapstoneProject.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

// -------------------------------------------------------------------------
// Contains business logic related to global application statistics.
//
// This service depends only on the UserRepository interface and does not
// depend on a specific database or repository implementation.
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
        // Delegate the data operation to the repository layer.
        return userRepository.getBmiDistribution();
    }
}