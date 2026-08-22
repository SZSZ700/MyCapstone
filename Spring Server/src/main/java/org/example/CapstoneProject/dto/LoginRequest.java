package org.example.CapstoneProject.dto;

import jakarta.validation.constraints.NotBlank;

// -------------------------------------------------------------------------
// Represents the data required for a login request.
//
// This DTO contains only the fields required for authentication
// and prevents the controller from depending on the full User model
// when processing login requests.
// -------------------------------------------------------------------------
@SuppressWarnings("unused")
public class LoginRequest {

    // Username provided by the client.
    @NotBlank(message = "Username is required")
    private String userName;

    // Password provided by the client.
    @NotBlank(message = "Password is required")
    private String password;

    // ---------------------------------------------------------------------
    // Default constructor required for JSON deserialization.
    // ---------------------------------------------------------------------
    public LoginRequest() {
    }

    // ---------------------------------------------------------------------
    // Returns the username.
    // ---------------------------------------------------------------------
    public String getUserName() {
        return userName;
    }

    // ---------------------------------------------------------------------
    // Sets the username.
    // ---------------------------------------------------------------------
    public void setUserName(String userName) {
        this.userName = userName;
    }

    // ---------------------------------------------------------------------
    // Returns the password.
    // ---------------------------------------------------------------------
    public String getPassword() {
        return password;
    }

    // ---------------------------------------------------------------------
    // Sets the password.
    // ---------------------------------------------------------------------
    public void setPassword(String password) {
        this.password = password;
    }
}