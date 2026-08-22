// Define the package of the Spring Boot application
package org.example.CapstoneProject;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

// -------------------------------------------------------------------------
// Marks this class as a Spring Boot application (enables auto-configuration)
// This is a convenience annotation that is equivalent to declaring
// @SpringBootConfiguration, @EnableAutoConfiguration and @ComponentScan.
// -------------------------------------------------------------------------
@SpringBootApplication
public class Application {
    // -------------------------------------------------------------------------
    // Main method - the starting point of the Spring Boot application
    // -------------------------------------------------------------------------
    public static void main(String[] args) {
        // Start the Spring Boot application (loads context, beans, etc.)
        SpringApplication.run(Application.class, args);
    }
}

