@file:Suppress("PackageName")
package org.example.CapstoneProject.dto
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

// -------------------------------------------------------------------------
// Represents the data required for a signup request.
// This DTO contains the fields accepted from the client during
// user registration and keeps the controller separated from the
// persistence model.
// -------------------------------------------------------------------------
@Suppress("unused")
data class SignupRequest(
    // Username provided by the client.
    @field:NotBlank(message = "Username is required")
    @field:Size(min = 3, max = 30, message = "Username must be between 3 and 30 characters")
    var userName: String = "",

    // Password provided by the client.
    @field:NotBlank(message = "Password is required")
    @field:Size(min = 4, max = 100, message = "Password must be between 4 and 100 characters")
    var password: String = "",

    // Full name provided by the client.
    @field:NotBlank(message = "Full name is required")
    var fullName: String = "",

    // Age provided by the client.
    @field:Min(value = 1, message = "Age must be greater than 0")
    var age: Int = 0
)