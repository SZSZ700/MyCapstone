@file:Suppress("PackageName")
package org.example.CapstoneProject.config
import com.mongodb.client.MongoClient
import com.mongodb.client.MongoClients
import com.mongodb.client.MongoDatabase
import org.example.CapstoneProject.EnvConfiguration.EnvConfig
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

// -------------------------------------------------------------------------
// Configures the MongoDB Java Driver.
// MongoDB connection values are loaded from the .env file
// through EnvConfig.
// -------------------------------------------------------------------------
@Configuration
class MongoConfiguration {
    // ---------------------------------------------------------------------
    // Creates one shared MongoClient for the whole application.
    // ---------------------------------------------------------------------
    @Bean
    fun mongoClient(): MongoClient { return MongoClients.create(EnvConfig.getMongoUri()) }

    // ---------------------------------------------------------------------
    // Creates the MongoDatabase instance used by the repositories.
    // ---------------------------------------------------------------------
    @Bean
    fun mongoDatabase(mongoClient: MongoClient): MongoDatabase {
        return mongoClient.getDatabase(EnvConfig.getMongoDatabase())
    }
}