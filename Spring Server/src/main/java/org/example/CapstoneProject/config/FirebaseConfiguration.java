package org.example.CapstoneProject.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import org.example.CapstoneProject.EnvConfiguration.EnvConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.io.InputStream;

// -------------------------------------------------------------------------
// Marks this class as a Spring configuration class.
// Spring will scan this class and create the objects returned by @Bean.
// -------------------------------------------------------------------------
@Configuration
public class FirebaseConfiguration {

    // ---------------------------------------------------------------------
    // Initializes Firebase and returns a reference to the Users node.
    // ---------------------------------------------------------------------
    @Bean
    public DatabaseReference usersReference() throws IOException {

        // Load the Firebase service account file from resources
        // and close the stream automatically when initialization is finished.
        try (InputStream serviceAccount =
                     getClass().getResourceAsStream(
                             "/myfinaltopap-firebase-adminsdk-fbsvc-765944770e.json"
                     )) {

            // Check that the resource was found.
            if (serviceAccount == null) {
                // Throw an error if the file is missing.
                throw new IllegalStateException(
                        "Service account JSON not found!"
                );
            }

            // Read the Firebase database URL from configuration.
            String firebaseUrl = EnvConfig.getFirebaseUrl();

            // Build the Firebase options object.
            FirebaseOptions options = FirebaseOptions.builder()
                    // Set credentials from the service account stream.
                    .setCredentials(
                            GoogleCredentials.fromStream(serviceAccount)
                    )
                    // Set the realtime database URL.
                    .setDatabaseUrl(firebaseUrl)
                    // Finish building the options.
                    .build();

            // Initialize Firebase only once.
            if (FirebaseApp.getApps().isEmpty()) {
                // Create the Firebase app instance.
                FirebaseApp.initializeApp(options);

                // Print a connection message.
                System.out.println(
                        "Connected to Firebase project: myfinaltopap"
                );
            }

            // Return a reference to the Users collection in Firebase.
            return FirebaseDatabase.getInstance()
                    .getReference("Users");
        }
    }
}