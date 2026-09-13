package org.example.CapstoneProject.config;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import org.example.CapstoneProject.EnvConfiguration.EnvConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// -------------------------------------------------------------------------
// Configures the MongoDB Java Driver.
//
// MongoDB connection values are loaded from the .env file
// through EnvConfig.
// -------------------------------------------------------------------------
@Configuration
public class MongoConfiguration {

    // ---------------------------------------------------------------------
    // Creates one shared MongoClient for the whole application.
    // ---------------------------------------------------------------------
    @Bean
    public MongoClient mongoClient() {
        return MongoClients.create(EnvConfig.getMongoUri());
    }

    // ---------------------------------------------------------------------
    // Creates the MongoDatabase instance used by the repositories.
    // ---------------------------------------------------------------------
    @Bean
    public MongoDatabase mongoDatabase(MongoClient mongoClient) {
        return mongoClient.getDatabase(EnvConfig.getMongoDatabase());
    }
}