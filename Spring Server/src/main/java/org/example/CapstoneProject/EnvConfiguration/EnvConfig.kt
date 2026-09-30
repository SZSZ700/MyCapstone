@file:Suppress("PackageName")
package org.example.CapstoneProject.EnvConfiguration
import io.github.cdimascio.dotenv.Dotenv

// -------------------------------------------------------------------------
// Provides access to environment-specific values stored in the .env file.
// -------------------------------------------------------------------------
object EnvConfig {

    // Loads the .env file from the project root directory.
    private val dotenv: Dotenv = Dotenv.load()

    // ---------------------------------------------------------------------
    // Returns the JWT secret used to sign and verify JWT tokens.
    // ---------------------------------------------------------------------
    @JvmStatic
    fun getJwtSecret(): String? {
        return dotenv["JWT_SECRET"]
    }

    // ---------------------------------------------------------------------
    // Returns the MongoDB connection URI.
    // ---------------------------------------------------------------------
    @JvmStatic
    fun getMongoUri(): String? {
        return dotenv["mongodb.uri"]
    }

    // ---------------------------------------------------------------------
    // Returns the MongoDB database name.
    // ---------------------------------------------------------------------
    @JvmStatic
    fun getMongoDatabase(): String? {
        return dotenv["mongodb.database"]
    }
}