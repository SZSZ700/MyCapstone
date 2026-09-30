@file:Suppress("PackageName")

package org.example.CapstoneProject.config
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder

// -------------------------------------------------------------------------
// Configures password hashing for the application.
//
// BCrypt is used to securely hash user passwords before they are stored.
// -------------------------------------------------------------------------
@Configuration
class PasswordConfiguration {

    // ---------------------------------------------------------------------
    // Creates the shared PasswordEncoder used by the application.
    // ---------------------------------------------------------------------
    @Bean
    fun passwordEncoder(): PasswordEncoder {
        return BCryptPasswordEncoder()
    }
}