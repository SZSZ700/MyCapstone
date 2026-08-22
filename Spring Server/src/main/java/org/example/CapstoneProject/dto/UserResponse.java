package org.example.CapstoneProject.dto;

@SuppressWarnings("unused")
public class UserResponse {

    // Username returned to the client.
    private String userName;

    // User age returned to the client.
    private int age;

    // User full name returned to the client.
    private String fullName;

    // User BMI returned to the client.
    private double bmi;


    // Default constructor required for JSON serialization/deserialization.
    public UserResponse() {}


    // Constructor used to create a user response without exposing
    // the user's password.
    public UserResponse(String userName, int age, String fullName, double bmi) {
        // Store the username.
        this.userName = userName;
        // Store the age.
        this.age = age;
        // Store the full name.
        this.fullName = fullName;
        // Store the BMI value.
        this.bmi = bmi;
    }


    // Return the username.
    public String getUserName() { return userName; }

    // Update the username.
    public void setUserName(String userName) { this.userName = userName; }

    // Return the user's age.
    public int getAge() { return age; }

    // Update the user's age.
    public void setAge(int age) { this.age = age; }

    // Return the user's full name.
    public String getFullName() { return fullName; }

    // Update the user's full name.
    public void setFullName(String fullName) { this.fullName = fullName; }

    // Return the user's BMI.
    public double getBmi() { return bmi; }

    // Update the user's BMI.
    public void setBmi(double bmi) { this.bmi = bmi; }
}