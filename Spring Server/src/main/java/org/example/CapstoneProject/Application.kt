@file:Suppress("PackageName")

// Define the package of the Spring Boot application.
package org.example.CapstoneProject

import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication

// -------------------------------------------------------------------------
// Marks this class as a Spring Boot application and enables
// Spring Boot auto-configuration.
//
// This annotation is equivalent to using:
// @SpringBootConfiguration
// @EnableAutoConfiguration
// @ComponentScan
// -------------------------------------------------------------------------
@SpringBootApplication
class Application

// -------------------------------------------------------------------------
// Main function - the starting point of the Spring Boot application.
// -------------------------------------------------------------------------
fun main(args: Array<String>) {
    // Start the Spring Boot application and load the application context.
    SpringApplication.run(Application::class.java, *args)
}