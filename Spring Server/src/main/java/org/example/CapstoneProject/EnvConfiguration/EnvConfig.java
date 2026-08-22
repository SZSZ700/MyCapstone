package org.example.CapstoneProject.EnvConfiguration;

import io.github.cdimascio.dotenv.Dotenv;

public class EnvConfig {

    // Loads the .env file from the root directory of the project.
    private static final Dotenv dotenv = Dotenv.load();

    // Returns the Firebase URL stored in the .env file.
    public static String getFirebaseUrl() {
        return dotenv.get("FIREBASE_URL");
    }

    // Returns the JWT secret stored in the .env file.
    public static String getJwtSecret() {
        return dotenv.get("JWT_SECRET");
    }
}