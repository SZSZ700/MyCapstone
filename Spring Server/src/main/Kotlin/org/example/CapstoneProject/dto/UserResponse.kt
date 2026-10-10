@file:Suppress("PackageName")
package org.example.CapstoneProject.dto

// -------------------------------------------------------------------------
// Represents the public user information returned by the REST API.
// This DTO exposes only the fields that may be returned to the client
// and intentionally does not include the user's password.
// -------------------------------------------------------------------------
@Suppress("unused")
data class UserResponse(
    // Username returned to the client.
    var userName: String = "",
    // User age returned to the client.
    var age: Int = 0,
    // User full name returned to the client.
    var fullName: String = "",
    // User BMI returned to the client.
    var bmi: Double = 0.0
)