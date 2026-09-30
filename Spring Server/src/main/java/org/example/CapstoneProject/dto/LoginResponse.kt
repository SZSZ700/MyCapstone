@file:Suppress("PackageName")
package org.example.CapstoneProject.dto

// -------------------------------------------------------------------------
// Response returned after a successful login.
//
// Contains the JWT token together with the public user information.
//
// The user's password is intentionally never included in this response.
// -------------------------------------------------------------------------
@Suppress("unused")
data class LoginResponse(
    // JWT token used for authenticated requests.
    var token: String = "",

    // Username of the authenticated user.
    var userName: String = "",

    // Age of the authenticated user.
    var age: Int = 0,

    // Full name of the authenticated user.
    var fullName: String = "",

    // BMI value of the authenticated user.
    var bmi: Double = 0.0
)