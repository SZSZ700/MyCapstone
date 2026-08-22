package org.example.CapstoneProject.dto;

// -------------------------------------------------------------------------
// Response returned after a successful login.
//
// Contains the JWT token together with the public user information.
//
// The user's password is intentionally never included in this response.
// -------------------------------------------------------------------------
@SuppressWarnings("unused")
public class LoginResponse {

    // JWT token used for authenticated requests.
    private String token;

    // Username of the authenticated user.
    private String userName;

    // Age of the authenticated user.
    private int age;

    // Full name of the authenticated user.
    private String fullName;

    // BMI value of the authenticated user.
    private double bmi;

    // ---------------------------------------------------------------------
    // Empty constructor required by serialization frameworks.
    // ---------------------------------------------------------------------
    public LoginResponse() {}

    // ---------------------------------------------------------------------
    // Creates a complete login response.
    // ---------------------------------------------------------------------
    public LoginResponse(String token, String userName, int age, String fullName, double bmi) {
        this.token = token;
        this.userName = userName;
        this.age = age;
        this.fullName = fullName;
        this.bmi = bmi;
    }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }
    public int getAge() { return age; }
    public void setAge(int age) { this.age = age; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public double getBmi() { return bmi; }
    public void setBmi(double bmi) { this.bmi = bmi; }
}