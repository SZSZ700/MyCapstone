@file:Suppress("PackageName")
package org.example.CapstoneProject.dto
import jakarta.validation.constraints.NotBlank

// -------------------------------------------------------------------------
// Represents the data required for a login request.
// This DTO contains only the fields required for authentication
// and prevents the controller from depending on the full User model
// when processing login requests.
// -------------------------------------------------------------------------
@Suppress("unused")
data class LoginRequest(
    // Username provided by the client.
    @field:NotBlank(message = "Username is required")
    var userName: String = "",
    // Password provided by the client.
    @field:NotBlank(message = "Password is required")
    var password: String = ""
)