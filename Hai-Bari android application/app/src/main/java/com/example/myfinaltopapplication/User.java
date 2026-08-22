package com.example.myfinaltopapplication;

// This is the client-side User model
// It is used ONLY on Android side to send/receive data via REST
@SuppressWarnings("unused")
public class User {
    // Username field
    private String userName;
    // Password field
    private String password;
    // Age field
    private int age;
    // Full name field
    private String fullName;

    // Constructor with all fields
    public User(String userName, String password, int age, String fullName) {
        this.userName = userName;
        this.password = password;
        this.age = age;
        this.fullName = fullName;
    }

    // Constructor used when receiving user data from the server
    // without receiving the password field.
    public User(String userName, int age, String fullName) {
        this.userName = userName;
        this.age = age;
        this.fullName = fullName;
    }

    // Constructor with username + password only
    @SuppressWarnings("unused")
    public User(String userName, String password) {
        this.userName = userName;
        this.password = password;
    }

    // Getters and setters
    public String getUserName() { return userName; }
    @SuppressWarnings("unused")
    public void setUserName(String userName) { this.userName = userName; }

    public String getPassword() { return password; }
    @SuppressWarnings("unused")
    public void setPassword(String password) { this.password = password; }

    public int getAge() { return age; }
    @SuppressWarnings("unused")
    public void setAge(int age) { this.age = age; }

    public String getFullName() { return fullName; }
    @SuppressWarnings("unused")
    public void setFullName(String fullName) { this.fullName = fullName; }

    // For debugging / logging
    @SuppressWarnings("NullableProblems")
    @Override
    public String toString() {
        return "User{" +
                "userName='" + userName + '\'' +
                ", password='" + password + '\'' +
                ", age=" + age +
                ", fullName='" + fullName + '\'' +
                '}';
    }
}
