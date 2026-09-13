package org.example.CapstoneProject.EnvConfiguration;
import io.github.cdimascio.dotenv.Dotenv;

// -------------------------------------------------------------------------
// Provides access to environment-specific values stored in the .env file.
// -------------------------------------------------------------------------
public class EnvConfig {
    // Loads the .env file from the project root directory.
    private static final Dotenv dotenv = Dotenv.load();

    // ---------------------------------------------------------------------
    // Returns the JWT secret used to sign and verify JWT tokens.
    // ---------------------------------------------------------------------
    public static String getJwtSecret() {
        return dotenv.get("JWT_SECRET");
    }

    // ---------------------------------------------------------------------
    // Returns the MongoDB connection URI.
    // ---------------------------------------------------------------------
    public static String getMongoUri() {
        return dotenv.get("mongodb.uri");
    }

    // ---------------------------------------------------------------------
    // Returns the MongoDB database name.
    // ---------------------------------------------------------------------
    public static String getMongoDatabase() {
        return dotenv.get("mongodb.database");
    }
}